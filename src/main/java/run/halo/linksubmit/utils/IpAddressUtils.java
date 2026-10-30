package run.halo.linksubmit.utils;

import lombok.extern.slf4j.Slf4j;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Locale;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.server.ServerRequest;

/**
 * Ip address utils.
 * Code from internet.
 */
@Slf4j
public class IpAddressUtils {
    public static final String UNKNOWN = "unknown";

    /** IPv6 文本形式最长 45 字符（含 IPv4-mapped 写法） */
    private static final int IPv6_MAX_LENGTH = 45;

    private static final String[] IP_HEADER_NAMES = {
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "CF-Connecting-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR",
    };

    /**
     * Gets the IP address from request.
     *
     * @param request is server http request
     * @return IP address if found, otherwise {@link #UNKNOWN}.
     */
    public static String getClientIp(ServerHttpRequest request) {
        var remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && !remoteAddress.isUnresolved()
            && remoteAddress.getAddress() != null) {
            if (isTrustedProxy(remoteAddress)) {
                for (String header : IP_HEADER_NAMES) {
                    String forwarded = firstForwardedIp(request.getHeaders().getFirst(header));
                    if (forwarded != null) {
                        return forwarded;
                    }
                }
            }
            return remoteAddress.getAddress().getHostAddress();
        }
        return UNKNOWN;
    }

    private static boolean isTrustedProxy(InetSocketAddress remoteAddress) {
        InetAddress address = remoteAddress.getAddress();
        return address.isAnyLocalAddress() || address.isLoopbackAddress()
            || address.isLinkLocalAddress() || address.isSiteLocalAddress()
            || (address.getAddress().length == 16 && (address.getAddress()[0] & 0xfe) == 0xfc);
    }

    /**
     * 取代理链里最可信的一跳，并只做字面量解析。
     *
     * <p>两个安全要点：
     * <ol>
     *   <li>取最右值而不是最左值。{@code X-Forwarded-For} 由客户端逐跳追加，最左值客户端可以
     *       随意填写，用它做限流 key 等于把限流关掉；最右值是可信代理亲眼看到的对端地址。</li>
     *   <li>不做任何 DNS 解析。原实现对 {@code [0-9a-fA-F:]+} 的输入直接调用
     *       {@code InetAddress.getByName}，像 {@code X-Forwarded-For: beef} 这种既不是 IP
     *       又长得像十六进制的值会进入域名解析；本方法在 Netty 事件循环上同步执行，
     *       几十个并发就能把全站接口拖住。</li>
     * </ol>
     */
    private static String firstForwardedIp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] hops = value.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String normalized = normalizeLiteral(hops[i].trim());
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    /**
     * 只接受 IPv4/IPv6 字面量，返回规范化的字符串（仅用于限流 key，不做网络查询）。
     *
     * @return 规范化地址；不是字面量时返回 {@code null}
     */
    private static String normalizeLiteral(String candidate) {
        if (candidate.isEmpty() || candidate.length() > IPv6_MAX_LENGTH) {
            return null;
        }
        String value = candidate;
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        if (value.indexOf(':') < 0) {
            return normalizeIpv4(value);
        }
        // IPv6：结构化校验后统一小写，避免同一地址多种写法被当成不同 key
        if (value.contains("%") || value.contains(":::")) {
            return null;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean allowed = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F') || c == ':' || c == '.';
            if (!allowed) {
                return null;
            }
        }
        if (value.indexOf(':') < 2) {
            return null;
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static String normalizeIpv4(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        StringBuilder builder = new StringBuilder(15);
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty() || part.length() > 3) {
                return null;
            }
            for (int j = 0; j < part.length(); j++) {
                if (part.charAt(j) < '0' || part.charAt(j) > '9') {
                    return null;
                }
            }
            int octet = Integer.parseInt(part);
            if (octet > 255) {
                return null;
            }
            if (i > 0) {
                builder.append('.');
            }
            builder.append(octet);
        }
        return builder.toString();
    }


    /**
     * Gets the ip address from request.
     *
     * @param request http request
     * @return ip address if found, otherwise {@link #UNKNOWN}.
     */
    public static String getIpAddress(ServerRequest request) {
        try {
            return getClientIp(request.exchange().getRequest());
        } catch (Exception e) {
            log.warn("Failed to obtain client IP, and fallback to unknown.", e);
            return UNKNOWN;
        }
    }

}
