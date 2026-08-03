package run.halo.linksubmit.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
