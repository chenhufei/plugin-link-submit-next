package com.kunkunyu.link.submit.endpoint;

import com.kunkunyu.link.submit.service.BadgeService;
import com.kunkunyu.link.submit.service.RankingService;
import com.kunkunyu.link.submit.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublicEndpoint implements CustomEndpoint {

    private final BadgeService badgeService;

    private final RankingService rankingService;

    private final WeeklyReportService weeklyReportService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .GET("badge", this::getBadge, builder -> {
                builder.operationId("getBadge")
                    .description("获取友链认证徽章")
                    .tag("public");
            })
            .GET("card", this::getCard, builder -> {
                builder.operationId("getCard")
                    .description("获取友链卡片 HTML")
                    .tag("public");
            })
            .GET("ranking", this::getRanking, builder -> {
                builder.operationId("getRanking")
                    .description("获取友链排行榜")
                    .tag("public");
            })
            .GET("report", this::getReport, builder -> {
                builder.operationId("getReport")
                    .description("获取友链健康周报")
                    .tag("public");
            })
            .build();
    }

    Mono<ServerResponse> getBadge(ServerRequest request) {
        String siteName = request.queryParam("site").orElse("My Site");
        String logo = request.queryParam("logo").orElse("");
        String badge = badgeService.generateBadge(siteName, logo, "");
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_XML)
            .bodyValue(badge);
    }

    Mono<ServerResponse> getCard(ServerRequest request) {
        String siteName = request.queryParam("site").orElse("");
        String siteUrl = request.queryParam("url").orElse("");
        String logo = request.queryParam("logo").orElse("");
        String desc = request.queryParam("desc").orElse("");
        String card = badgeService.generateCardHtml(siteName, siteUrl, logo, desc);
        return ServerResponse.ok()
            .contentType(MediaType.TEXT_HTML)
            .bodyValue(card);
    }

    Mono<ServerResponse> getRanking(ServerRequest request) {
        int limit = request.queryParam("limit")
            .filter(s -> s.matches("\\d+"))
            .map(Integer::parseInt)
            .filter(l -> l > 0 && l <= 100)
            .orElse(10);
        var ranking = rankingService.getLinkRanking(limit);
        return ServerResponse.ok()
            .bodyValue(ranking);
    }

    Mono<ServerResponse> getReport(ServerRequest request) {
        String report = weeklyReportService.generateWeeklyReport();
        return ServerResponse.ok()
            .contentType(MediaType.TEXT_MARKDOWN)
            .bodyValue(report);
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.link.submit.kunkunyu.com/v1alpha1");
    }
}
