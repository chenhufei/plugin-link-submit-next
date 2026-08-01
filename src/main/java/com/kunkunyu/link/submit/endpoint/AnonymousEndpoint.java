package com.kunkunyu.link.submit.endpoint;

import com.kunkunyu.link.submit.extension.LinkSubmit;
import com.kunkunyu.link.submit.service.LinkService;
import com.kunkunyu.link.submit.service.LinkSubmitService;
import com.kunkunyu.link.submit.service.SettingConfigLinkSubmit;
import com.kunkunyu.link.submit.utils.IpAddressUtils;
import com.kunkunyu.link.submit.vo.LinkGroupVo;
import com.kunkunyu.link.submit.utils.SafeUrlValidator;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springdoc.core.fn.builders.schema.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

@Slf4j
@Component
public class AnonymousEndpoint implements CustomEndpoint {

    private static final String TAG = "api.link.submit.kunkunyu.com/v1alpha1/LinkSubmit";

    private final SettingConfigLinkSubmit settingConfigLinkSubmit;

    private final LinkService linkService;

    private final LinkSubmitService linkSubmitService;

    private final RateLimiterRegistry rateLimiterRegistry;

    private final Set<String> limiterNames = ConcurrentHashMap.newKeySet();

    public AnonymousEndpoint(SettingConfigLinkSubmit settingConfigLinkSubmit,
        LinkService linkService, LinkSubmitService linkSubmitService,
        RateLimiterRegistry rateLimiterRegistry) {
        this.settingConfigLinkSubmit = settingConfigLinkSubmit;
        this.linkService = linkService;
        this.linkSubmitService = linkSubmitService;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @PreDestroy
    void cleanup() {
        limiterNames.forEach(rateLimiterRegistry::remove);
        limiterNames.clear();
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .GET("linkgroups", this::linkGroups, builder -> {
                builder.operationId("linkGroups")
                    .description("友链分组")
                    .tag(TAG)
                    .response(
                        responseBuilder()
                            .implementationArray(LinkGroupVo.class)
                    );
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
            )
            .POST("linksubmits/-/submit", this::submit,
                builder -> builder.operationId("submit")
                    .description("自助提交友链")
                    .tag(TAG)
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(Builder.schemaBuilder()
                                .implementation(CreateLinkSubmitRequest.class))
                        ))
                    .response(responseBuilder()
                        .implementation(LinkSubmit.class))
            ).build();
    }

    Mono<ServerResponse> linkGroups(ServerRequest request) {
        var basicConfig = settingConfigLinkSubmit.getBasicConfig();

        return basicConfig.flatMap(basic -> {
            List<String> forbidSelectedGroupNames = basic.getForbidSelectedGroupName();

            return linkService.listGroup()
                .filter(linkGroupVo -> {
                    if (forbidSelectedGroupNames != null) {
                       return !forbidSelectedGroupNames.contains(linkGroupVo.getGroupName());
                    }
                    return true;
                }).collectList();
        }).flatMap(linkGroupVoList -> ServerResponse.ok().bodyValue(linkGroupVoList));
    }

    Mono<ServerResponse> submit(ServerRequest request) {
        String clientIp = IpAddressUtils.getIpAddress(request);
        String limiterName = "submit-link-" + clientIp;
        limiterNames.add(limiterName);
        RateLimiter rateLimiter = this.rateLimiterRegistry.rateLimiter(limiterName);
        return request.bodyToMono(CreateLinkSubmitRequest.class)
            .flatMap(req -> linkSubmitService.createLinkSubmit(req, clientIp))
            .transformDeferred(RateLimiterOperator.of(rateLimiter))
            .flatMap(resultsVo -> ServerResponse.ok().bodyValue(resultsVo))
            .onErrorResume(e -> {
                log.error("Link submit failed: {}", e.getMessage(), e);
                org.springframework.web.server.ResponseStatusException ex;
                if (e instanceof org.springframework.web.server.ResponseStatusException) {
                    ex = (org.springframework.web.server.ResponseStatusException) e;
                } else {
                    ex = new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage(), e);
                }
                return Mono.error(ex);
            });
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
        .flatMap(result -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(result))
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
    }

    private static FetchResult fetchDocument(String initialUrl) throws Exception {
        String current = initialUrl;
        for (int redirect = 0; redirect < 4; redirect++) {
            SafeUrlValidator.requirePublicHttpUrl(current);
            var response = Jsoup.connect(current)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .timeout(8000)
                .followRedirects(false)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .execute();
            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                String location = response.header("Location");
                if (location == null || location.isBlank()) {
                    throw new IllegalArgumentException("目标网站重定向地址为空");
                }
                current = SafeUrlValidator.requirePublicHttpUrl(
                    java.net.URI.create(current).resolve(location).toString()).toString();
                continue;
            }
            if (status < 200 || status >= 400) {
                throw new java.io.IOException("目标网站返回 HTTP " + status);
            }
            return new FetchResult(response.parse());
        }
        throw new IllegalArgumentException("目标网站重定向次数过多");
    }

    private record FetchResult(Document doc) {
    }

    /** 将相对 URL 转为绝对 URL */
    private static String absUrl(String href, String baseUrl) {
        if (href == null || href.isEmpty()) return null;
        if (href.startsWith("http://") || href.startsWith("https://")) return href;
        if (href.startsWith("//")) return "https:" + href;
        try {
            return new java.net.URL(new java.net.URL(baseUrl), href).toString();
        } catch (Exception e) {
            return null;
        }
    }

    @Data
    public static class CreateLinkSubmitRequest {

        @NotBlank
        private String url;

        @NotBlank
        private String displayName;

        private String logo;

        private String description;

        private String oldUrl;

        private String email;

        @NotBlank
        private String groupName;

        private String rssUrl;

        private String message;

        @NotBlank
        private LinkSubmit.LinkSubmitType type;

    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.link.submit.kunkunyu.com/v1alpha1");
    }
}
