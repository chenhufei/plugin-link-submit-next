package run.halo.linksubmit.utils;

import lombok.extern.slf4j.Slf4j;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.server.ServerRequest;

/**
 * Ip address utils.
 * Code from internet.
 */
@Slf4j
public class IpAddressUtils {
    public static final String UNKNOWN = "unknown";

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

    private static String firstForwardedIp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String candidate = value.split(",", 2)[0].trim();
        if (!isIpLiteral(candidate)) {
            return null;
        }
        try {
            return InetAddress.getByName(candidate).getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isIpLiteral(String value) {
        return value.matches("(?:\\d{1,3}\\.){3}\\d{1,3}|[0-9a-fA-F:]+")
            && !value.contains(" ");
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
