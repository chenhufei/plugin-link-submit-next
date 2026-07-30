package com.kunkunyu.link.submit.service.impl;

import com.kunkunyu.link.submit.extension.Link;
import com.kunkunyu.link.submit.extension.LinkSubmit;
import com.kunkunyu.link.submit.service.HealthCheckService;
import com.kunkunyu.link.submit.utils.LinkUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ReactiveExtensionClient;

import javax.net.ssl.HttpsURLConnection;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class HealthCheckServiceImpl implements HealthCheckService {

    private final ReactiveExtensionClient client;

    @Override
    public Mono<LinkSubmit.HealthStatus> checkLinkHealth(String url) {
        return Mono.fromCallable(() -> {
            LinkSubmit.HealthStatus status = new LinkSubmit.HealthStatus();
            status.setLastCheckedAt(Instant.now());

            try {
                long startTime = System.currentTimeMillis();
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(10000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

                int responseCode = connection.getResponseCode();
                long responseTime = System.currentTimeMillis() - startTime;

                status.setReachable(responseCode >= 200 && responseCode < 400);
                status.setHttpStatusCode(responseCode);
                status.setResponseTimeMs(responseTime);

                if (url.startsWith("https")) {
                    try {
                        HttpsURLConnection httpsConn = (HttpsURLConnection) connection;
                        httpsConn.connect();
                        status.setSslValid(true);
                    } catch (Exception e) {
                        status.setSslValid(false);
                    }
                } else {
                    status.setSslValid(false);
                }

                connection.disconnect();
            } catch (Exception e) {
                status.setReachable(false);
                status.setErrorMessage(e.getMessage());
                log.warn("Health check failed for {}: {}", url, e.getMessage());
            }

            return status;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> detectHiddenLink(String url, String ownDomain) {
        return Mono.fromCallable(() -> {
            try {
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

                String html;
                try (var inputStream = connection.getInputStream()) {
                    html = new String(inputStream.readAllBytes());
                } finally {
                    connection.disconnect();
                }

                if (!html.contains(ownDomain)) {
                    return true;
                }

                String lowerHtml = html.toLowerCase();
                String[] hiddenPatterns = {
                    "display:none", "display: none", "visibility:hidden", "visibility: hidden",
                    "font-size:0", "font-size: 0", "opacity:0", "opacity: 0",
                    "height:0", "height: 0", "width:0", "width: 0",
                    "position:absolute", "position: absolute"
                };

                int linkIndex = lowerHtml.indexOf(ownDomain.toLowerCase());
                if (linkIndex >= 0) {
                    String surrounding = lowerHtml.substring(
                        Math.max(0, linkIndex - 200),
                        Math.min(lowerHtml.length(), linkIndex + ownDomain.length() + 200)
                    );
                    for (String pattern : hiddenPatterns) {
                        if (surrounding.contains(pattern)) {
                            return true;
                        }
                    }
                }

                return false;
            } catch (Exception e) {
                log.warn("Hidden link detection failed for {}: {}", url, e.getMessage());
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> batchHealthCheck() {
        return client.listAll(Link.class, new ListOptions(), Sort.unsorted())
            .flatMap(link -> {
                String url = link.getSpec().getUrl();
                return checkLinkHealth(url)
                    .flatMap(health -> {
                        log.info("Health check for {}: reachable={}, status={}, responseTime={}ms",
                            url, health.getReachable(), health.getHttpStatusCode(), health.getResponseTimeMs());
                        return Mono.empty();
                    });
            })
            .then();
    }
}
