package run.halo.linksubmit.utils;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/** Validates outbound URLs before any server-side fetch. */
public final class SafeUrlValidator {

    private SafeUrlValidator() {
    }

    public static URI requirePublicHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("URL 不能为空");
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            if (uri.getHost() == null || uri.getUserInfo() != null
                || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("仅支持公网 HTTP 或 HTTPS URL");
            }
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (isPrivateOrLocal(address)) {
                    throw new IllegalArgumentException("不允许访问内网或本机地址");
                }
            }
            return uri;
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("URL 主机无法解析", e);
        }
    }

    public static boolean isPublicHttpUrl(String value) {
        try {
            requirePublicHttpUrl(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isPrivateOrLocal(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
            || address.isLinkLocalAddress() || address.isSiteLocalAddress()
            || address.isMulticastAddress()) {
            return true;
        }
        if (bytes.length == 4) {
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            return first == 100 && second >= 64 && second <= 127
                || first == 192 && second == 0 && (bytes[2] & 0xff) == 0
                || first == 198 && second >= 18 && second <= 19;
        }
        return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
    }
}
