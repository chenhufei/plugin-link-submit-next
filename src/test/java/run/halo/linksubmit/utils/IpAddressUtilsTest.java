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

    @Test
    void usesTheHopAppendedByTheTrustedProxy() {
        // 最左值由客户端自带，可写成任意 IP 绕过限流；最右值才是可信代理看到的对端
        var request = MockServerHttpRequest.get("/")
            .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
            .header("X-Forwarded-For", "6.6.6.6, 203.0.113.8")
            .build();

        assertThat(IpAddressUtils.getClientIp(request)).isEqualTo("203.0.113.8");
    }

    @Test
    void ignoresNonLiteralHeaderValues() {
        // beef 既不是 IP 又形似十六进制：必须直接丢弃，旧实现会拿它做域名解析
        var request = MockServerHttpRequest.get("/")
            .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
            .header("X-Forwarded-For", "beef")
            .build();

        assertThat(IpAddressUtils.getClientIp(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void normalizesIpv4Literal() {
        var request = MockServerHttpRequest.get("/")
            .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
            .header("X-Forwarded-For", "203.000.113.008")
            .build();

        assertThat(IpAddressUtils.getClientIp(request)).isEqualTo("203.0.113.8");
    }

    @Test
    void rejectsOutOfRangeIpv4AndKeepsLookingLeft() {
        var request = MockServerHttpRequest.get("/")
            .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
            .header("X-Forwarded-For", "198.51.100.7, 256.1.1.1")
            .build();

        assertThat(IpAddressUtils.getClientIp(request)).isEqualTo("198.51.100.7");
    }
}
