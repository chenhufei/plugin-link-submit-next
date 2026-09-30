package run.halo.linksubmit.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.util.concurrent.ImmediateEventExecutor;
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
}
