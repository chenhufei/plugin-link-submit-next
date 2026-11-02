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
            // 只放行 Web 默认端口：不限端口时，一个匿名请求就能探测内网/外网的
            // 6379(Redis)、25(SMTP)、9200(ES) 等非 HTTP 服务，用于确认内网资产存活。
            int port = uri.getPort();
            if (port != -1 && port != 80 && port != 443) {
                throw new IllegalArgumentException("仅支持 80 或 443 端口的 URL");
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
        byte[] raw = address.getAddress();
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
            || address.isLinkLocalAddress() || address.isSiteLocalAddress()
            || address.isMulticastAddress()) {
            return true;
        }
        if (raw.length == 16) {
            // 内嵌 IPv4 的写法必须按内层地址判，只比 IPv6 前缀会漏：
            // ::a.b.c.d（IPv4 兼容）、::ffff:a.b.c.d（IPv4 映射）、
            // 64:ff9b::/96（NAT64）、2002::/16（6to4）都能把 127.0.0.1 或 10.x 塞进来。
            byte[] embedded = embeddedIpv4(raw);
            if (embedded != null) {
                return isPrivateIpv4(embedded);
            }
            return (raw[0] & 0xfe) == 0xfc
                || (raw[0] == (byte) 0xfe && (raw[1] & 0xc0) == 0x80)
                || (raw[0] == 0x20 && raw[1] == 0x01
                    && raw[2] == 0x0d && raw[3] == (byte) 0xb8);
        }
        return isPrivateIpv4(raw);
    }

    private static boolean isPrivateIpv4(byte[] ip) {
        int first = ip[0] & 0xff;
        int second = ip[1] & 0xff;
        return first == 0 || first == 10 || first == 127 || first >= 224
            || first == 169 && second == 254
            || first == 172 && second >= 16 && second <= 31
            || first == 192 && second == 168
            || first == 100 && second >= 64 && second <= 127
            || first == 192 && second == 0 && (ip[2] & 0xff) == 0
            || first == 192 && second == 0 && (ip[2] & 0xff) == 2
            || first == 192 && second == 88 && (ip[2] & 0xff) == 99
            || first == 198 && (second == 18 || second == 19)
            || first == 198 && second == 51 && (ip[2] & 0xff) == 100
            || first == 203 && second == 0 && (ip[2] & 0xff) == 113;
    }

    /**
     * 取出 IPv6 地址里内嵌的 IPv4 四字节，没有则返回 {@code null}。
     */
    private static byte[] embeddedIpv4(byte[] raw) {
        // ::ffff:a.b.c.d
        if (isZero(raw, 0, 10) && raw[10] == (byte) 0xff && raw[11] == (byte) 0xff) {
            return Arrays.copyOfRange(raw, 12, 16);
        }
        // ::a.b.c.d（IPv4 兼容地址，含全零的 ::）
        if (isZero(raw, 0, 12)) {
            return Arrays.copyOfRange(raw, 12, 16);
        }
        // 64:ff9b::a.b.c.d（NAT64）
        if (raw[0] == 0x00 && raw[1] == 0x64 && raw[2] == (byte) 0xff
            && raw[3] == (byte) 0x9b && isZero(raw, 4, 12)) {
            return Arrays.copyOfRange(raw, 12, 16);
        }
        // 2002:a.b.c.d::（6to4）
        if (raw[0] == 0x20 && raw[1] == 0x02) {
            return Arrays.copyOfRange(raw, 2, 6);
        }
        return null;
    }

    private static boolean isZero(byte[] bytes, int from, int to) {
        for (int i = from; i < to; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return true;
    }

    public record ValidatedUrl(URI uri, List<InetAddress> addresses) {
    }
}
