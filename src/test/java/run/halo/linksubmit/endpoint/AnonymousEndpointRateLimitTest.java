package run.halo.linksubmit.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import java.net.InetSocketAddress;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import run.halo.linksubmit.service.LinkService;
import run.halo.linksubmit.service.LinkSubmitService;
import run.halo.linksubmit.service.SettingConfigLinkSubmit;

class AnonymousEndpointRateLimitTest {

    @Test
    void shouldRateLimitSiteInfoWithoutFetchingRemoteSite() {
        var settings = mock(SettingConfigLinkSubmit.class);
        var basic = new SettingConfigLinkSubmit.BasicConfig();
        basic.setEnableLinkPreview(true);
        when(settings.getBasicConfig()).thenReturn(Mono.just(basic));
        var linkService = mock(LinkService.class);
        var submitService = mock(LinkSubmitService.class);
        var registry = RateLimiterRegistry.ofDefaults();
        var endpoint = new AnonymousEndpoint(settings, linkService, submitService, registry);
        var clientAddress = new InetSocketAddress("198.51.100.10", 54321);
        var limiter = registry.rateLimiter(
            "site-info-198.51.100.10", AnonymousEndpoint.SITE_INFO_RATE_LIMITER_CONFIG);

        for (int i = 0; i < AnonymousEndpoint.SITE_INFO_LIMIT_PER_MINUTE; i++) {
            assertThat(limiter.acquirePermission()).isTrue();
        }

        var request = MockServerRequest.builder()
            .exchange(MockServerWebExchange.from(
                MockServerHttpRequest.get("/site-info")
                    .remoteAddress(clientAddress)
                    .build()))
            .queryParam("url", "https://example.com")
            .build();
        var response = endpoint.fetchSiteInfo(request).block();

        assertThat(response).isNotNull();
        assertThat(response.statusCode().value()).isEqualTo(429);
        assertThat(response.headers().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.headers().getFirst("Retry-After")).isEqualTo("60");
        verifyNoInteractions(linkService, submitService);

        endpoint.cleanup();
        assertThat(registry.getAllRateLimiters()).isEmpty();
    }

    @Test
    void shouldReturn429WhenSubmitLimiterRejectsRequest() {
        var settings = mock(SettingConfigLinkSubmit.class);
        var linkService = mock(LinkService.class);
        var submitService = mock(LinkSubmitService.class);
        var config = RateLimiterConfig.custom()
            .limitForPeriod(1)
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ZERO)
            .build();
        var registry = RateLimiterRegistry.of(config);
        var endpoint = new AnonymousEndpoint(settings, linkService, submitService, registry);
        var clientAddress = new InetSocketAddress("203.0.113.20", 54321);
        assertThat(registry.rateLimiter("submit-link-203.0.113.20").acquirePermission()).isTrue();

        var request = MockServerRequest.builder()
            .exchange(MockServerWebExchange.from(
                MockServerHttpRequest.post("/linksubmits/-/submit")
                    .remoteAddress(clientAddress)
                    .build()))
            .body(Mono.just(new AnonymousEndpoint.CreateLinkSubmitRequest()));
        var response = endpoint.submit(request).block();

        assertThat(response).isNotNull();
        assertThat(response.statusCode().value()).isEqualTo(429);
        assertThat(response.headers().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        verifyNoInteractions(linkService, submitService);
    }
}
