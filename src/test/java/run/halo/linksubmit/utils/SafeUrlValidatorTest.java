package run.halo.linksubmit.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.util.concurrent.ImmediateEventExecutor;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

class SafeUrlValidatorTest {

    @Test
    void acceptsPublicHttpAddress() {
        assertDoesNotThrow(() -> SafeUrlValidator.requirePublicHttpUrl("https://1.1.1.1"));
    }

    @Test
    void rejectsLocalAndPrivateAddresses() {
        assertThrows(IllegalArgumentException.class,
            () -> SafeUrlValidator.requirePublicHttpUrl("http://127.0.0.1/admin"));
        assertThrows(IllegalArgumentException.class,
            () -> SafeUrlValidator.requirePublicHttpUrl("http://10.0.0.1"));
        assertThrows(IllegalArgumentException.class,
            () -> SafeUrlValidator.requirePublicHttpUrl("http://192.0.2.1"));
        assertThrows(IllegalArgumentException.class,
            () -> SafeUrlValidator.requirePublicHttpUrl("http://100.64.0.1"));
        assertThrows(IllegalArgumentException.class,
            () -> SafeUrlValidator.requirePublicHttpUrl("http://169.254.169.254/latest/meta-data"));
        assertThrows(IllegalArgumentException.class,
            () -> SafeUrlValidator.requirePublicHttpUrl("http://[::1]/"));
    }

    @Test
    void rejectsUnsupportedSchemesAndUserInfo() {
        assertThrows(IllegalArgumentException.class,
            () -> SafeUrlValidator.requirePublicHttpUrl("file:///etc/passwd"));
        assertThrows(IllegalArgumentException.class,
            () -> SafeUrlValidator.requirePublicHttpUrl("http://user@example.com"));
    }

    @Test
    void rejectsNonWebPorts() {
        // 不限端口时，匿名接口可用来探测 6379(Redis)、25(SMTP)、9200(ES) 等非 HTTP 服务
        assertThrows(IllegalArgumentException.class,
            () -> SafeUrlValidator.requirePublicHttpUrl("http://1.1.1.1:6379"));
        assertThrows(IllegalArgumentException.class,
            () -> SafeUrlValidator.requirePublicHttpUrl("http://1.1.1.1:25"));
        assertThrows(IllegalArgumentException.class,
            () -> SafeUrlValidator.requirePublicHttpUrl("https://1.1.1.1:9200/"));
        assertDoesNotThrow(() -> SafeUrlValidator.requirePublicHttpUrl("http://1.1.1.1:80"));
        assertDoesNotThrow(() -> SafeUrlValidator.requirePublicHttpUrl("https://1.1.1.1:443"));
    }

    @Test
    void resolverPinsConnectionsToValidatedAddresses() throws Exception {
        var validated = SafeUrlValidator.validatePublicHttpUrl("https://1.1.1.1");
        var resolverGroup = SafeUrlValidator.pinnedResolver(validated);
        try {
            var resolver = resolverGroup.getResolver(ImmediateEventExecutor.INSTANCE);
            var address = resolver.resolve(InetSocketAddress.createUnresolved("1.1.1.1", 443)).get();
            assertEquals("1.1.1.1", address.getAddress().getHostAddress());
            assertThrows(ExecutionException.class,
                () -> resolver.resolve(InetSocketAddress.createUnresolved("127.0.0.1", 443)).get());
        } finally {
            resolverGroup.close();
        }
    }

    @Test
    void rejectsIpv4MappedLocalAddresses() throws Exception {
        byte[] mappedLoopback = new byte[16];
        mappedLoopback[10] = (byte) 0xff;
        mappedLoopback[11] = (byte) 0xff;
        mappedLoopback[12] = 127;
        mappedLoopback[15] = 1;
        var address = Inet6Address.getByAddress(null, mappedLoopback, -1);
        assertTrue(SafeUrlValidator.isPrivateOrLocal(address));
    }

    @Test
    void rejectsIpv4EmbeddedInIpv6Wrappers() throws Exception {
        // ::7f00:1 —— IPv4 兼容地址，等价于 ::127.0.0.1
        assertTrue(SafeUrlValidator.isPrivateOrLocal(ipv6(
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x7f, 0, 0, 1)));
        // 64:ff9b::a00:1 —— NAT64，内层是 10.0.0.1
        assertTrue(SafeUrlValidator.isPrivateOrLocal(ipv6(
            0x00, 0x64, 0xff, 0x9b, 0, 0, 0, 0, 0, 0, 0, 0, 0x0a, 0, 0, 1)));
        // 2002:7f00:1:: —— 6to4，内层是 127.0.0.1
        assertTrue(SafeUrlValidator.isPrivateOrLocal(ipv6(
            0x20, 0x02, 0x7f, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)));
        // 2606:4700::1111 —— 公网地址，不能误杀
        assertFalse(SafeUrlValidator.isPrivateOrLocal(ipv6(
            0x26, 0x06, 0x47, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x11)));
    }

    private static InetAddress ipv6(int... raw) throws Exception {
        byte[] bytes = new byte[16];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) raw[i];
        }
        return Inet6Address.getByAddress(null, bytes, -1);
    }
}
