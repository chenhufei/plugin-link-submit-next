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
            return ServerResponse.badRequest().bodyValue(Map.of("error", "url 参数不能为空"));
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
                return Mono.fromCallable(() -> parseSiteInfo(finalUrl))
                    .subscribeOn(Schedulers.boundedElastic())
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
                        return ServerResponse.badRequest().bodyValue(Map.of(
                            "title", "URL 不可访问",
                            "status", 400,
                            "detail", "该地址不受支持或无法访问"));
                    })
                    .onErrorResume(e -> {
                        log.warn("Failed to fetch site info for {}: {}", finalUrl, e.getMessage());
                        return siteUnavailable();
                    });
            }));
    }

    /**
     * 读取链接预览开关。配置缺失或读取失败时按关闭处理：
     * 这个开关本身就是保护，"读不到就当开启"会让保护在故障时消失。
     */
    private Mono<Boolean> linkPreviewEnabled() {
        return settingConfigLinkSubmit.getBasicConfig()
            .map(SettingConfigLinkSubmit.BasicConfig::isEnableLinkPreview)
            .defaultIfEmpty(Boolean.FALSE)
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
        return ServerResponse.status(HttpStatus.FORBIDDEN).bodyValue(Map.of(
            "title", "链接预览未启用",
            "status", 403,
            "detail", "管理员已关闭链接预览功能"));
    }

    private static Mono<ServerResponse> siteUnavailable() {
        return ServerResponse.status(502).bodyValue(Map.of(
            "title", "网站信息获取失败",
            "status", 502,
            "detail", "目标网站暂时无法访问"));
    }

    /**
     * 抓取并解析目标站点。返回的文本一律按目标站点声明的字符集解码：
     * 先取响应的 Content-Type 字符集，缺失时交给 Jsoup 用页面 meta 自动识别。
     */
    private static Map<String, String> parseSiteInfo(String url) throws Exception {
        SafeUrlValidator.requirePublicHttpUrl(url);
        Document doc = fetchDocument(url);
        String documentUrl = doc.location();
        if (documentUrl == null || documentUrl.isBlank()) {
            documentUrl = url;
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
     * 跟随有限次重定向取回目标 HTML。
     * 每一步都对地址做公网校验（含重定向目标），避免 DNS rebinding 把请求引到内网。
     */
    private static Document fetchDocument(String initialUrl) throws Exception {
        java.net.URI current = java.net.URI.create(initialUrl);
        for (int redirect = 0; redirect < MAX_REDIRECTS; redirect++) {
            var validatedUrl = SafeUrlValidator.validatePublicHttpUrl(current.toString());
            var response = fetchPinned(validatedUrl);
            if (response == null) {
                throw new java.io.IOException("目标网站未在 " + SITE_INFO_TIMEOUT.getSeconds()
                    + " 秒内响应");
            }
            int status = response.status();
            if (status >= 300 && status < 400) {
                String location = response.location();
                if (location == null || location.isBlank()) {
                    throw new IllegalArgumentException("目标网站重定向地址为空");
                }
                current = validatedUrl.uri().resolve(location);
                continue;
            }
            if (status < 200 || status >= 400) {
                throw new java.io.IOException("目标网站返回 HTTP " + status);
            }
            String contentType = response.contentType();
            String normalizedContentType = contentType == null
                ? "" : contentType.toLowerCase(java.util.Locale.ROOT);
            if (!normalizedContentType.isBlank()
                && !normalizedContentType.startsWith("text/html")
                && !normalizedContentType.startsWith("application/xhtml+xml")) {
                throw new IllegalArgumentException("目标网站不是 HTML 页面");
            }
            return parseHtml(response.body(), response.charset(), validatedUrl.uri().toString());
        }
        throw new IllegalArgumentException("目标网站重定向次数过多");
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

    private static PinnedHttpResponse fetchPinned(SafeUrlValidator.ValidatedUrl validatedUrl) {
        var resolver = SafeUrlValidator.pinnedResolver(validatedUrl);
        try {
            var httpClient = HttpSecurityUtils.secureHttpClient()
                .resolver(resolver)
                .followRedirect(false)
                .compress(true)
                .responseTimeout(Duration.ofSeconds(8));
            // 每次请求都新建 WebClient 会连带新建连接池与编解码器，这里复用公共配置
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
                .block(Duration.ofSeconds(10));
        } finally {
            resolver.close();
        }
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
