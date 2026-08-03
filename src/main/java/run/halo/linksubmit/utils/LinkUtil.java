package run.halo.linksubmit.utils;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpMethod;
import java.io.IOException;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class LinkUtil {

    public static final String HTTP_PROTOCOL = "http://";
    public static final String HTTPS_PROTOCOL = "https://";

    private static final int DEFAULT_FACICON_MAX_SIZE = 1024 * 5;
    private static final int DEFAULT_FACICON_MIN_SIZE = 1;

    private static final Pattern[] ICON_PATTERNS = new Pattern[] {
        Pattern.compile("rel=[\"']icon[\"'][^\r\n>]+?((?<=href=[\"']).+?(?=[\"']))"),
        Pattern.compile("((?<=href=[\"']).+?(?=[\"']))[^\r\n<]+?rel=[\"']icon[\"']")
    };

    @Deprecated
    public static String getFavicon(String url) {
        SafeUrlValidator.requirePublicHttpUrl(url);
        if (!url.startsWith(HTTP_PROTOCOL) && !url.startsWith(HTTPS_PROTOCOL)) {
            url = HTTP_PROTOCOL + url;
        }
        String html;
        try {
            html = HttpRequest.get(url)
                .setConnectionTimeout(3000)
                .setReadTimeout(5000)
                .setFollowRedirects(false)
                .execute().body();
        } catch (Exception e) {
            log.warn("Failed to fetch favicon for {}: {}", url, e.getMessage());
            return null;
        }
        if (CharSequenceUtil.isEmpty(html)) {
            return null;
        }
        for (Pattern iconPattern : ICON_PATTERNS) {
            Matcher matcher = iconPattern.matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    public static boolean checkFavicon(String faviconUrl) {
        int faviconLength = getFaviconSize(faviconUrl);
        return faviconLength >= DEFAULT_FACICON_MIN_SIZE
            && faviconLength < DEFAULT_FACICON_MAX_SIZE;
    }

    private static int getFaviconSize(String faviconUrl) {
        int contentLength = 0;
        try {
            final URL url = SafeUrlValidator.requirePublicHttpUrl(faviconUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(HttpMethod.GET.name());
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(false);
            connection.setInstanceFollowRedirects(false);
            contentLength = connection.getContentLength();
            log.debug("Favicon size: {}", contentLength);
        } catch (MalformedURLException e) {
            log.error("Invalid favicon URL: {}", faviconUrl, e);
        } catch (ProtocolException e) {
            log.error("Protocol error for favicon: {}", faviconUrl, e);
        } catch (IOException e) {
            log.error("IO error fetching favicon: {}", faviconUrl, e);
        }
        return contentLength;
    }

    private static Element getElementById(Document htmlDocument, String id) {
        if (htmlDocument == null || id == null || id.isEmpty()) {
            return null;
        }
        return htmlDocument.getElementById(id);
    }

    public static boolean hasLinkByHtml(String url, String domainName) {
        SafeUrlValidator.requirePublicHttpUrl(url);
        if (!url.startsWith(HTTP_PROTOCOL) && !url.startsWith(HTTPS_PROTOCOL)) {
            url = HTTP_PROTOCOL + url;
        }
        String html;
        try {
            html = HttpRequest.get(url)
                .setConnectionTimeout(3000)
                .setReadTimeout(5000)
                .setFollowRedirects(false)
                .execute().body();
        } catch (Exception e) {
            log.warn("Failed to fetch page for link check: {}", url, e.getMessage());
            return false;
        }
        return CharSequenceUtil.isNotEmpty(html) && html.contains(domainName);
    }

    public static boolean hasLinkByUrl(String url, String domainName) {
        if (domainName == null || domainName.isEmpty()) {
            return false;
        }
        if (domainName.startsWith(HTTP_PROTOCOL) || domainName.startsWith(HTTPS_PROTOCOL)) {
            domainName = domainName.replace(HTTP_PROTOCOL, "");
            domainName = domainName.replace(HTTPS_PROTOCOL, "");
        }
        return url.contains(domainName);
    }

    public static String getDomain(String urlString) {
        try {
            URL url = new URL(urlString);
            return url.getHost();
        } catch (MalformedURLException e) {
            log.warn("Invalid URL format: {}", urlString);
            return "Invalid URL: " + e.getMessage();
        }
    }

    public static boolean isValidUrl(String urlString) {
        return SafeUrlValidator.isPublicHttpUrl(urlString);
    }

    public static boolean urlChecker(String url) {
        try {
            SafeUrlValidator.requirePublicHttpUrl(url);
            HttpResponse response = HttpRequest.get(url)
                .setConnectionTimeout(3000)
                .setReadTimeout(5000)
                .setFollowRedirects(false)
                .execute();
            int statusCode = response.getStatus();
            return statusCode == 200 || statusCode == 301 || statusCode == 302;
        } catch (Exception e) {
            return false;
        }
    }
}
