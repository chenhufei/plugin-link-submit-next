package run.halo.linksubmit.endpoint;

import run.halo.linksubmit.service.SettingConfigLinkSubmit;
import run.halo.linksubmit.utils.IpAddressUtils;
import run.halo.linksubmit.utils.SafeUrlValidator;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.infra.utils.HttpSecurityUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

@Slf4j
@Component
public class AnonymousEndpoint implements CustomEndpoint {

    private static final String TAG = "api.link.submit.halo.run/v1alpha1/LinkSubmit";
    static final int SITE_INFO_LIMIT_PER_MINUTE = 12;
    static final int SITE_INFO_MAX_BODY_BYTES = 1024 * 1024;
    private static final int MAX_TRACKED_LIMITERS = 10_000;
    // 一次性淘汰到半水位，而不是刚好回到上限：否则下一次请求又会触发一轮全表排序
    private static final int LIMITER_PRUNE_TARGET = MAX_TRACKED_LIMITERS / 2;
    private static final int MAX_REDIRECTS = 4;
    static final Duration SITE_INFO_TIMEOUT = Duration.ofSeconds(12);
    static final RateLimiterConfig SITE_INFO_RATE_LIMITER_CONFIG = RateLimiterConfig.custom()
        .limitForPeriod(SITE_INFO_LIMIT_PER_MINUTE)
        .limitRefreshPeriod(Duration.ofMinutes(1))
        .timeoutDuration(Duration.ZERO)
        .build();

    // 请求级配置（connector、resolver）每次 clone 后再设置，公共部分只建一次
    private static final WebClient.Builder WEB_CLIENT_BUILDER = WebClient.builder()
        .filter(HttpSecurityUtils.maxResponseSizeFilter(SITE_INFO_MAX_BODY_BYTES))
        .codecs(configurer -> configurer.defaultCodecs()
            .maxInMemorySize(SITE_INFO_MAX_BODY_BYTES));

    /**
     * 安全 HTTP 客户端模板。
     *
     * <p>{@code HttpSecurityUtils.secureHttpClient()} 每次调用都要重新装配 SSL 上下文、
     * 编解码器与连接参数；它本身是不可变配置对象，可以安全复用。真正按请求变化的只有
     * resolver（把域名钉死到已校验的 IP），所以每次只需 {@code .resolver(...)} 克隆一份。
     * 底层连接池由 Reactor Netty 全局共享，resolver 只影响本次请求的地址解析，
     * 不会因为复用模板而让不同站点共用连接。
     */
    private static final reactor.netty.http.client.HttpClient SECURE_HTTP_CLIENT =
        HttpSecurityUtils.secureHttpClient();

    /**
     * 单跳抓取的上限（含读完 body）。
     *
     * <p>取 10 秒而不是把外层 12 秒平均分给 4 跳：真实网络里首跳慢、后续跳快，平均分配会让
     * 首跳频繁踩线（实测直连 example.com 就要 4 秒）。总预算仍由外层 {@code timeout} 兜住。
     */
    private static final Duration PINNED_RESPONSE_TIMEOUT = Duration.ofSeconds(10);

    private final SettingConfigLinkSubmit settingConfigLinkSubmit;

    private final RateLimiterRegistry rateLimiterRegistry;

    private final Set<String> limiterNames = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, Long> limiterLastUsed = new ConcurrentHashMap<>();

    public AnonymousEndpoint(SettingConfigLinkSubmit settingConfigLinkSubmit,
        RateLimiterRegistry rateLimiterRegistry) {
        this.settingConfigLinkSubmit = settingConfigLinkSubmit;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @PreDestroy
    void cleanup() {
        limiterNames.forEach(rateLimiterRegistry::remove);
        limiterNames.clear();
        limiterLastUsed.clear();
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .GET("site-info", this::fetchSiteInfo,
                builder -> builder.operationId("fetchSiteInfo")
                    .description("根据网址获取网站标题、描述、Logo等信息")
                    .tag(TAG)
                    .parameter(parameterBuilder()
                        .name("url")
                        .description("网站地址")
                        .required(true))
                    .response(responseBuilder()
                        .implementation(Map.class))
            ).build();
    }

    /**
     * 服务端代理获取网站信息，避免前端跨域问题和国内网络限制。
     * 使用 jsoup 抓取目标页面 HTML 并解析 og 标签和标准 meta 标签。
     */
    Mono<ServerResponse> fetchSiteInfo(ServerRequest request) {
        String url = request.queryParam("url").orElse("").trim();
        if (url.isEmpty()) {
            return ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyValue(Map.of(
                    "title", "缺少参数",
                    "status", HttpStatus.BAD_REQUEST.value(),
                    "detail", "url 参数不能为空"));
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        final String finalUrl = url;
        // 取 IP、维护限流器都要读写共享集合，放到 boundedElastic 上，避免卡住事件循环
        return resolveRateLimiter(request)
            .flatMap(rateLimiter -> linkPreviewEnabled().flatMap(enabled -> {
                if (!enabled) {
                    return previewDisabled();
                }
                // 整条抓取链已是响应式，不再需要 fromCallable 包住一个阻塞方法：
                // 超时能真正取消订阅并断开连接，也不会长时间占住 boundedElastic 的线程。
                return parseSiteInfo(finalUrl)
                    .timeout(SITE_INFO_TIMEOUT)
                    .transformDeferred(RateLimiterOperator.of(rateLimiter))
                    .flatMap(result -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result))
                    .onErrorResume(RequestNotPermitted.class,
                        e -> tooManyRequests("网站信息获取过于频繁，请稍后再试"))
                    .onErrorResume(TimeoutException.class, e -> siteUnavailable())
                    .onErrorResume(IllegalArgumentException.class, e -> {
                        // 具体原因（内网地址被拒 / 主机无法解析 / 端口不允许）只写日志。
                        // 原样回显等于给出「这个主机是否存在、是否在内网」的探测 oracle。
                        log.warn("Rejected site-info url {}: {}", finalUrl, e.getMessage());
                        return ServerResponse.badRequest()
                            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                            .bodyValue(Map.of(
                                "title", "URL 不可访问",
                                "status", HttpStatus.BAD_REQUEST.value(),
                                "detail", "该地址不受支持或无法访问"));
                    })
                    .onErrorResume(e -> {
                        log.warn("Failed to fetch site info for {}: {}", finalUrl, e.getMessage());
                        return siteUnavailable();
                    });
            }));
    }

    /**
     * 读取链接预览开关。
     *
     * <p>设置组缺失（刚装上插件、从没保存过设置）时，{@code getBasicConfig()} 给的是
     * {@code extensions/settings.yaml} 里声明的默认值——也就是开启，和站长在后台看到的
     * 开关状态一致。这里原先挂着一个 {@code defaultIfEmpty(Boolean.FALSE)}，但上游
     * {@code map} 之后永远不会是空流，那句代码执行不到，注释却写着「缺失即关闭」，
     * 两句话互相矛盾。真读失败（异常）才按关闭处理：这是保护开关，故障时不该消失。
     */
    private Mono<Boolean> linkPreviewEnabled() {
        return settingConfigLinkSubmit.getBasicConfig()
            .map(SettingConfigLinkSubmit.BasicGroupConfig::isEnableLinkPreview)
            .onErrorResume(e -> {
                log.warn("Read link submit config failed, link preview stays disabled", e);
                return Mono.just(Boolean.FALSE);
            });
    }

    private Mono<RateLimiter> resolveRateLimiter(ServerRequest request) {
        return Mono.fromCallable(() -> {
            String limiterName = "site-info-" + IpAddressUtils.getIpAddress(request);
            limiterNames.add(limiterName);
            limiterLastUsed.put(limiterName, System.currentTimeMillis());
            pruneLimitersIfNeeded();
            return rateLimiterRegistry.rateLimiter(limiterName, SITE_INFO_RATE_LIMITER_CONFIG);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private static Mono<ServerResponse> previewDisabled() {
        return ServerResponse.status(HttpStatus.FORBIDDEN)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .bodyValue(Map.of(
                "title", "链接预览未启用",
                "status", HttpStatus.FORBIDDEN.value(),
                "detail", "管理员已关闭链接预览功能"));
    }

    private static Mono<ServerResponse> siteUnavailable() {
        return ServerResponse.status(HttpStatus.BAD_GATEWAY)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .bodyValue(Map.of(
                "title", "网站信息获取失败",
                "status", HttpStatus.BAD_GATEWAY.value(),
                "detail", "目标网站暂时无法访问"));
    }

    /**
     * 抓取并解析目标站点。返回的文本一律按目标站点声明的字符集解码：
     * 先取响应的 Content-Type 字符集，缺失时交给 Jsoup 用页面 meta 自动识别。
     *
     * <p>整条链路是响应式的：DNS 校验与 Jsoup 解析放在 boundedElastic（这两步是真阻塞），
     * HTTP 请求本身在事件循环上异步完成。旧实现在一个 Callable 里 {@code block(10s)}，
     * 外层 {@code timeout(12s)} 只能取消订阅、中断不了线程，超时后那个线程还要陪着跑完
     * 最多 4 跳重定向——并发 10×CPU 个预览请求就能把 boundedElastic 占满，
     * 连带通知、查库都跟着排队。
     */
    private static Mono<Map<String, String>> parseSiteInfo(String url) {
        return Mono.fromCallable(() -> SafeUrlValidator.validatePublicHttpUrl(url))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(validated -> fetchDocument(validated.uri(), MAX_REDIRECTS))
            .map(document -> extractSiteInfo(document, url));
    }

    /**
     * 跟随有限次重定向取回目标 HTML。
     * 每一步都对地址做公网校验（含重定向目标），避免 DNS rebinding 把请求引到内网。
     */
    private static Mono<Document> fetchDocument(java.net.URI initial, int remainingRedirects) {
        if (remainingRedirects <= 0) {
            return Mono.error(new IllegalArgumentException("目标网站重定向次数过多"));
        }
        return Mono.fromCallable(() -> SafeUrlValidator.validatePublicHttpUrl(initial.toString()))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(validated -> fetchPinned(validated)
                .flatMap(response -> {
                    int status = response.status();
                    if (status >= 300 && status < 400) {
                        String location = response.location();
                        if (location == null || location.isBlank()) {
                            return Mono.error(
                                new IllegalArgumentException("目标网站重定向地址为空"));
                        }
                        return fetchDocument(validated.uri().resolve(location),
                            remainingRedirects - 1);
                    }
                    if (status < 200 || status >= 400) {
                        return Mono.error(new java.io.IOException("目标网站返回 HTTP " + status));
                    }
                    String contentType = response.contentType();
                    String normalizedContentType = contentType == null
                        ? "" : contentType.toLowerCase(java.util.Locale.ROOT);
                    if (!normalizedContentType.isBlank()
                        && !normalizedContentType.startsWith("text/html")
                        && !normalizedContentType.startsWith("application/xhtml+xml")) {
                        return Mono.error(new IllegalArgumentException("目标网站不是 HTML 页面"));
                    }
                    String baseUri = validated.uri().toString();
                    return Mono.fromCallable(
                            () -> parseHtml(response.body(), response.charset(), baseUri))
                        .subscribeOn(Schedulers.boundedElastic());
                }));
    }

    /** 解析已抓到的 HTML，提取标题 / 描述 / Logo。纯 CPU，不涉及线程调度。 */
    private static Map<String, String> extractSiteInfo(Document doc, String fallbackUrl) {
        String documentUrl = doc.location();
        if (documentUrl == null || documentUrl.isBlank()) {
            documentUrl = fallbackUrl;
        }

        // 解析顺序与官方友链后台「获取网站信息」一致：<title> / meta description / rel=icon，
        // og 标签只在标准标签缺失时兜底
        String title = doc.title();
        if (title == null || title.isBlank()) {
            Element ogTitle = doc.selectFirst("meta[property=og:title]");
            if (ogTitle != null && !ogTitle.attr("content").isBlank()) {
                title = ogTitle.attr("content").trim();
            }
        }

        String description = null;
        Element metaDesc = doc.selectFirst("meta[name=description]");
        if (metaDesc != null && !metaDesc.attr("content").isBlank()) {
            description = metaDesc.attr("content").trim();
        }
        if (description == null || description.isEmpty()) {
            Element ogDesc = doc.selectFirst("meta[property=og:description]");
            if (ogDesc != null && !ogDesc.attr("content").isBlank()) {
                description = ogDesc.attr("content").trim();
            }
        }

        String logo = null;
        Element iconLink = doc.selectFirst("link[rel~=icon]");
        if (iconLink != null && !iconLink.attr("href").isBlank()) {
            logo = absUrl(iconLink.attr("href").trim(), documentUrl);
        }
        if (logo == null || logo.isEmpty()) {
            Element ogImage = doc.selectFirst("meta[property=og:image]");
            if (ogImage != null && !ogImage.attr("content").isBlank()) {
                logo = absUrl(ogImage.attr("content").trim(), documentUrl);
            }
        }
        // 兜底 favicon
        if (logo == null || logo.isEmpty()) {
            try {
                java.net.URL u = new java.net.URL(documentUrl);
                logo = u.getProtocol() + "://" + u.getHost()
                    + (u.getPort() > 0 ? ":" + u.getPort() : "") + "/favicon.ico";
            } catch (Exception ignored) {
            }
        }

        Map<String, String> result = new HashMap<>();
        result.put("title", title == null ? "" : title);
        result.put("description", description == null ? "" : description);
        result.put("logo", logo == null ? "" : logo);
        return result;
    }


    private void pruneLimitersIfNeeded() {
        if (limiterLastUsed.size() <= MAX_TRACKED_LIMITERS) {
            return;
        }
        limiterLastUsed.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .limit(Math.max(1, limiterLastUsed.size() - LIMITER_PRUNE_TARGET))
            .map(Map.Entry::getKey)
            .toList()
            .forEach(name -> {
                limiterLastUsed.remove(name);
                limiterNames.remove(name);
                rateLimiterRegistry.remove(name);
            });
    }

    private static Mono<ServerResponse> tooManyRequests(String detail) {
        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .header("Retry-After", "60")
            .bodyValue(Map.of(
                "title", "请求过于频繁",
                "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                "detail", detail));
    }

    /**
     * 按站点声明的字符集解析响应字节：
     * 有 Content-Type 字符集时用它，缺失时传 null 让 Jsoup 依据页面 meta 自动识别，
     * 不再固定按 UTF-8 解码导致 GBK 页面出现乱码。
     */
    private static Document parseHtml(byte[] body, String charsetName, String baseUri) {
        try (var input = new java.io.ByteArrayInputStream(body)) {
            return Jsoup.parse(input, charsetName, baseUri);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("读取目标网站响应失败", e);
        }
    }

    /**
     * 单跳抓取：把域名钉死到已校验的 IP，禁跟随重定向（由调用方逐跳校验）。
     *
     * <p>注意这里不 block：返回的 Mono 完成时才算一次抓取结束，超时由 {@code timeout}
     * 取消订阅，Reactor Netty 会随即关掉连接。原来的 {@code block(10s)} 既占线程，
     * 又让外层 timeout 形同虚设——线程不会因为取消订阅而停下。
     */
    private static Mono<PinnedHttpResponse> fetchPinned(
        SafeUrlValidator.ValidatedUrl validatedUrl) {
        var resolver = SafeUrlValidator.pinnedResolver(validatedUrl);
        var httpClient = SECURE_HTTP_CLIENT.resolver(resolver)
            .followRedirect(false)
            .compress(true)
            .responseTimeout(PINNED_RESPONSE_TIMEOUT);
        var webClient = WEB_CLIENT_BUILDER.clone()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
        return webClient.get()
            .uri(validatedUrl.uri())
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .accept(MediaType.TEXT_HTML, MediaType.valueOf("application/xhtml+xml"))
            .exchangeToMono(response -> response.bodyToMono(byte[].class)
                .defaultIfEmpty(new byte[0])
                .map(body -> new PinnedHttpResponse(
                    response.statusCode().value(),
                    response.headers().asHttpHeaders().getFirst("Location"),
                    response.headers().contentType().map(MediaType::toString).orElse(""),
                    response.headers().contentType()
                        .map(mediaType -> mediaType.getCharset() == null
                            ? null : mediaType.getCharset().name())
                        .orElse(null),
                    body)))
            .timeout(PINNED_RESPONSE_TIMEOUT)
            .doFinally(signal -> resolver.close());
    }

    private record PinnedHttpResponse(int status, String location, String contentType,
                                      String charset, byte[] body) {
    }

    /** 将相对 URL 转为绝对 URL */
    static String absUrl(String href, String baseUrl) {
        if (href == null || href.isEmpty()) return null;
        try {
            java.net.URI resolved = java.net.URI.create(baseUrl).resolve(href.trim());
            String scheme = resolved.getScheme();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                return null;
            }
            try {
                return SafeUrlValidator.requirePublicHttpUrl(resolved.toString()).toString();
            } catch (IllegalArgumentException e) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.link.submit.halo.run/v1alpha1");
    }
}
