package run.halo.linksubmit.utils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import io.netty.resolver.AbstractAddressResolver;
import io.netty.resolver.AddressResolver;
import io.netty.resolver.AddressResolverGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;

/** Validates outbound URLs before any server-side fetch. */
public final class SafeUrlValidator {

    private static final Set<String> BLOCKED_HOST_NAMES = Set.of(
        "localhost", "localhost.localdomain", "ip6-localhost", "ip6-loopback",
        "ip6-allnodes", "ip6-allrouters"
    );

    private SafeUrlValidator() {
    }

    public static URI requirePublicHttpUrl(String value) {
        return validatePublicHttpUrl(value).uri();
    }

    public static ValidatedUrl validatePublicHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("URL 不能为空");
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (host == null || uri.getUserInfo() != null
                || BLOCKED_HOST_NAMES.contains(host.toLowerCase(java.util.Locale.ROOT))
                || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("仅支持公网 HTTP 或 HTTPS URL");
            }
            return new ValidatedUrl(uri, resolvePublicAddresses(uri));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("URL 主机无法解析", e);
        }
    }

    public static AddressResolverGroup<InetSocketAddress> pinnedResolver(ValidatedUrl validatedUrl) {
        return new AddressResolverGroup<>() {
            @Override
            protected AddressResolver<InetSocketAddress> newResolver(EventExecutor executor) {
                return new AbstractAddressResolver<>(executor, InetSocketAddress.class) {
                    @Override
                    protected boolean doIsResolved(InetSocketAddress address) {
                        return !address.isUnresolved();
                    }

                    @Override
                    protected void doResolve(InetSocketAddress address,
                        Promise<InetSocketAddress> promise) {
                        if (!validatedUrl.uri().getHost().equalsIgnoreCase(address.getHostString())) {
                            promise.setFailure(new UnknownHostException("请求主机与已校验 URL 不一致"));
                            return;
                        }
                        promise.setSuccess(new InetSocketAddress(
                            validatedUrl.addresses().get(0), address.getPort()));
                    }

                    @Override
                    protected void doResolveAll(InetSocketAddress address,
                        Promise<List<InetSocketAddress>> promise) {
                        if (!validatedUrl.uri().getHost().equalsIgnoreCase(address.getHostString())) {
                            promise.setFailure(new UnknownHostException("请求主机与已校验 URL 不一致"));
                            return;
                        }
                        promise.setSuccess(validatedUrl.addresses().stream()
                            .map(ip -> new InetSocketAddress(ip, address.getPort()))
                            .toList());
                    }
                };
            }
        };
    }

    private static List<InetAddress> resolvePublicAddresses(URI uri) throws UnknownHostException {
        InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
        if (addresses.length == 0 || Arrays.stream(addresses).anyMatch(SafeUrlValidator::isPrivateOrLocal)) {
            throw new IllegalArgumentException("不允许访问内网或本机地址");
        }
        return List.of(addresses);
    }

    public static boolean isPublicHttpUrl(String value) {
        try {
            requirePublicHttpUrl(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    static boolean isPrivateOrLocal(InetAddress address) {
        byte[] normalized = address.getAddress();
        if (normalized.length == 16 && isIpv4Mapped(normalized)) {
            normalized = Arrays.copyOfRange(normalized, 12, 16);
        }
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
            || address.isLinkLocalAddress() || address.isSiteLocalAddress()
            || address.isMulticastAddress()) {
            return true;
        }
        if (normalized.length == 4) {
            int first = normalized[0] & 0xff;
            int second = normalized[1] & 0xff;
            return first == 0 || first == 10 || first == 127 || first >= 224
                || first == 169 && second == 254
                || first == 172 && second >= 16 && second <= 31
                || first == 192 && second == 168
                || first == 100 && second >= 64 && second <= 127
                || first == 192 && second == 0 && (normalized[2] & 0xff) == 0
                || first == 192 && second == 0 && (normalized[2] & 0xff) == 2
                || first == 192 && second == 88 && (normalized[2] & 0xff) == 99
                || first == 198 && (second == 18 || second == 19)
                || first == 198 && second == 51 && (normalized[2] & 0xff) == 100
                || first == 203 && second == 0 && (normalized[2] & 0xff) == 113;
        }
        return normalized.length == 16 && (
            (normalized[0] & 0xfe) == 0xfc
                || (normalized[0] == (byte) 0xfe && (normalized[1] & 0xc0) == 0x80)
                || (normalized[0] == 0x20 && normalized[1] == 0x01
                    && normalized[2] == 0x0d && normalized[3] == (byte) 0xb8)
        );
    }

    private static boolean isIpv4Mapped(byte[] bytes) {
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff;
    }

    public record ValidatedUrl(URI uri, List<InetAddress> addresses) {
    }
}
