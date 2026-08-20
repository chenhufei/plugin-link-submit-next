package run.halo.linksubmit.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

class IpAddressUtilsTest {

    @Test
    void ignoresForwardedHeadersFromPublicClients() {
        var request = MockServerHttpRequest.get("/")
            .remoteAddress(new InetSocketAddress("198.51.100.10", 8080))
            .header("X-Forwarded-For", "203.0.113.8")
            .build();

        assertThat(IpAddressUtils.getClientIp(request)).isEqualTo("198.51.100.10");
    }

    @Test
    void acceptsForwardedHeadersFromLocalProxy() {
        var request = MockServerHttpRequest.get("/")
            .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
            .header("X-Forwarded-For", "203.0.113.8")
            .build();

        assertThat(IpAddressUtils.getClientIp(request)).isEqualTo("203.0.113.8");
    }
}
