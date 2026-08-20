package run.halo.linksubmit.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import run.halo.linksubmit.service.SettingConfigLinkSubmit;

class AnonymousEndpointRateLimitTest {

    @Test
    void shouldRateLimitSiteInfoWithoutFetchingRemoteSite() {
        var settings = mock(SettingConfigLinkSubmit.class);
        var basic = new SettingConfigLinkSubmit.BasicConfig();
        basic.setEnableLinkPreview(true);
        when(settings.getBasicConfig()).thenReturn(Mono.just(basic));
        var registry = RateLimiterRegistry.ofDefaults();
        var endpoint = new AnonymousEndpoint(settings, registry);
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
        endpoint.cleanup();
        assertThat(registry.getAllRateLimiters()).isEmpty();
    }

    @Test
    void shouldNotExposeLegacySubmissionRoute() {
        var settings = mock(SettingConfigLinkSubmit.class);
        var endpoint = new AnonymousEndpoint(settings, RateLimiterRegistry.ofDefaults());

        var routeText = endpoint.endpoint().toString();
        assertThat(routeText).doesNotContain("linksubmits/-/submit");
        assertThat(routeText).contains("configuration", "site-info");
    }

    @Test
    void shouldOnlyReturnHttpLogoUrls() {
        assertThat(AnonymousEndpoint.absUrl("/favicon.ico", "https://example.com/page")).isEqualTo(
            "https://example.com/favicon.ico");
        assertThat(AnonymousEndpoint.absUrl("data:image/png;base64,abc",
            "https://example.com/page")).isNull();
        assertThat(AnonymousEndpoint.absUrl("javascript:alert(1)",
            "https://example.com/page")).isNull();
        assertThat(AnonymousEndpoint.absUrl("http://127.0.0.1/logo.png",
            "https://example.com/page")).isNull();
    }
}
