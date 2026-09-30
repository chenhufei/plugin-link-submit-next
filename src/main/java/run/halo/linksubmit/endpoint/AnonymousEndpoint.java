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
import reactor.netty.http.client.HttpClient;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

@Slf4j
@Component
public class AnonymousEndpoint implements CustomEndpoint {

    private static final String TAG = "api.link.submit.halo.run/v1alpha1/LinkSubmit";
    static final int SITE_INFO_LIMIT_PER_MINUTE = 12;
    static final int SITE_INFO_MAX_BODY_BYTES = 1024 * 1024;
    private static final int MAX_TRACKED_LIMITERS = 10_000;
    static final RateLimiterConfig SITE_INFO_RATE_LIMITER_CONFIG = RateLimiterConfig.custom()
        .limitForPeriod(SITE_INFO_LIMIT_PER_MINUTE)
        .limitRefreshPeriod(Duration.ofMinutes(1))
        .timeoutDuration(Duration.ZERO)
        .build();

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
            .GET("configuration", this::configuration, builder -> {
                builder.operationId("linkSubmitConfiguration")
                    .description("友链自助提交公开功能配置")
                    .tag(TAG)
                    .response(responseBuilder().implementation(Map.class));
            })
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

    Mono<ServerResponse> configuration(ServerRequest request) {
        return settingConfigLinkSubmit.getBasicConfig()
            .flatMap(config -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("linkPreviewEnabled", config.isEnableLinkPreview())));
    }

    /**
     * 服务端代理获取网站信息，避免前端跨域问题和国内网络限制。
     * 使用 jsoup 抓取目标页面 HTML 并解析 og 标签和标准 meta 标签。
     */
    Mono<ServerResponse> fetchSiteInfo(ServerRequest request) {
        String clientIp = IpAddressUtils.getIpAddress(request);
        String limiterName = "site-info-" + clientIp;
        limiterNames.add(limiterName);
        limiterLastUsed.put(limiterName, System.currentTimeMillis());
        pruneLimitersIfNeeded();
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(
            limiterName, SITE_INFO_RATE_LIMITER_CONFIG);
        String url = request.queryParam("url").orElse("").trim();
        if (url.isEmpty()) {
            return ServerResponse.badRequest().bodyValue(Map.of("error", "url 参数不能为空"));
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        final String finalUrl = url;
        return settingConfigLinkSubmit.getBasicConfig().flatMap(config -> {
            if (!config.isEnableLinkPreview()) {
                return ServerResponse.status(HttpStatus.FORBIDDEN).bodyValue(Map.of(
                    "title", "链接预览未启用",
                    "status", 403,
                    "detail", "管理员已关闭链接预览功能"));
            }
            return Mono.fromCallable(() -> {
            SafeUrlValidator.requirePublicHttpUrl(finalUrl);
            Document doc = fetchDocument(finalUrl)
                .doc();
            String documentUrl = doc.location();
            if (documentUrl == null || documentUrl.isBlank()) {
                documentUrl = finalUrl;
            }

            String title = null;
            // 优先 og:title
            Element ogTitle = doc.selectFirst("meta[property=og:title]");
            if (ogTitle != null && !ogTitle.attr("content").isBlank()) {
                title = ogTitle.attr("content").trim();
            }
            if (title == null || title.isEmpty()) {
                title = doc.title();
            }

            String description = null;
            Element ogDesc = doc.selectFirst("meta[property=og:description]");
            if (ogDesc != null && !ogDesc.attr("content").isBlank()) {
                description = ogDesc.attr("content").trim();
            }
            if (description == null || description.isEmpty()) {
                Element metaDesc = doc.selectFirst("meta[name=description]");
                if (metaDesc != null && !metaDesc.attr("content").isBlank()) {
                    description = metaDesc.attr("content").trim();
                }
            }

            String logo = null;
            Element ogImage = doc.selectFirst("meta[property=og:image]");
            if (ogImage != null && !ogImage.attr("content").isBlank()) {
                logo = absUrl(ogImage.attr("content").trim(), documentUrl);
            }
            if (logo == null || logo.isEmpty()) {
                Element iconLink = doc.selectFirst("link[rel~=icon]");
                if (iconLink != null && !iconLink.attr("href").isBlank()) {
                    logo = absUrl(iconLink.attr("href").trim(), documentUrl);
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
            })
            .subscribeOn(Schedulers.boundedElastic())
            .transformDeferred(RateLimiterOperator.of(rateLimiter))
            .flatMap(result -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(result))
            .onErrorResume(RequestNotPermitted.class,
                e -> tooManyRequests("网站信息获取过于频繁，请稍后再试"))
            .onErrorResume(IllegalArgumentException.class, e ->
                ServerResponse.badRequest().bodyValue(Map.of(
                    "title", "URL 不可访问",
                    "status", 400,
                    "detail", e.getMessage())))
            .onErrorResume(e -> {
                log.warn("Failed to fetch site info for {}: {}", finalUrl, e.getMessage());
                return ServerResponse.status(502).bodyValue(Map.of(
                    "title", "网站信息获取失败",
                    "status", 502,
                    "detail", "目标网站暂时无法访问"));
            });
        });
    }

    private void pruneLimitersIfNeeded() {
        if (limiterLastUsed.size() <= MAX_TRACKED_LIMITERS) {
            return;
        }
        limiterLastUsed.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .limit(Math.max(1, limiterLastUsed.size() - MAX_TRACKED_LIMITERS))
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

    private static FetchResult fetchDocument(String initialUrl) throws Exception {
        java.net.URI current = java.net.URI.create(initialUrl);
        for (int redirect = 0; redirect < 4; redirect++) {
            var validatedUrl = SafeUrlValidator.validatePublicHttpUrl(current.toString());
            var response = fetchPinned(validatedUrl);
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
            return new FetchResult(Jsoup.parse(response.body(), validatedUrl.uri().toString()));
        }
        throw new IllegalArgumentException("目标网站重定向次数过多");
    }

    private static PinnedHttpResponse fetchPinned(SafeUrlValidator.ValidatedUrl validatedUrl) {
        var resolver = SafeUrlValidator.pinnedResolver(validatedUrl);
        try {
            var httpClient = HttpClient.newConnection()
                .resolver(resolver)
                .followRedirect(false)
                .compress(true)
                .responseTimeout(Duration.ofSeconds(8));
            var webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs()
                    .maxInMemorySize(SITE_INFO_MAX_BODY_BYTES))
                .build();
            return webClient.get()
                .uri(validatedUrl.uri())
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .accept(MediaType.TEXT_HTML, MediaType.valueOf("application/xhtml+xml"))
                .exchangeToMono(response -> response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> new PinnedHttpResponse(
                        response.statusCode().value(),
                        response.headers().asHttpHeaders().getFirst("Location"),
                        response.headers().contentType().map(MediaType::toString).orElse(""),
                        body)))
                .block(Duration.ofSeconds(10));
        } finally {
            resolver.close();
        }
    }

    private record PinnedHttpResponse(int status, String location, String contentType, String body) {
    }

    private record FetchResult(Document doc) {
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
