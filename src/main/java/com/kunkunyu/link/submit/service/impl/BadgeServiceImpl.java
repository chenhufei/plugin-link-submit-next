package com.kunkunyu.link.submit.service.impl;

import com.kunkunyu.link.submit.service.BadgeService;
import org.springframework.stereotype.Component;

@Component
public class BadgeServiceImpl implements BadgeService {

    @Override
    public String generateBadge(String siteName, String logoUrl, String verifiedAt) {
        String safeSiteName = escapeHtml(siteName);
        return """
            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="60" viewBox="0 0 200 60">
              <defs>
                <linearGradient id="grad" x1="0%%" y1="0%%" x2="100%%" y2="0%%">
                  <stop offset="0%%" style="stop-color:#667eea;stop-opacity:1" />
                  <stop offset="100%%" style="stop-color:#764ba2;stop-opacity:1" />
                </linearGradient>
              </defs>
              <rect width="200" height="60" rx="8" fill="url(#grad)"/>
              <circle cx="28" cy="30" r="16" fill="white" fill-opacity="0.2"/>
              <text x="28" y="35" font-family="Arial,sans-serif" font-size="14" fill="white" text-anchor="middle">✓</text>
              <text x="52" y="24" font-family="Arial,sans-serif" font-size="11" fill="white" fill-opacity="0.8">友链已认证</text>
              <text x="52" y="42" font-family="Arial,sans-serif" font-size="10" fill="white">%s</text>
            </svg>
            """.formatted(safeSiteName);
    }

    @Override
    public String generateCardHtml(String siteName, String siteUrl, String logoUrl, String description) {
        String safeSiteName = escapeHtml(siteName);
        String safeSiteUrl = escapeHtml(siteUrl);
        String safeLogoUrl = escapeHtml(logoUrl);
        String safeDescription = escapeHtml(description != null ? description : "");
        String initial = safeSiteName.isEmpty() ? "?" : String.valueOf(safeSiteName.charAt(0));

        return """
            <div style="display:inline-flex;align-items:center;gap:12px;padding:12px 16px;border:1px solid #e5e7eb;border-radius:8px;background:#fff;font-family:system-ui,sans-serif;max-width:320px;">
              <img src="%s" alt="%s" style="width:48px;height:48px;border-radius:8px;object-fit:cover;" onerror="this.style.display='none'"/>
              <div style="flex:1;min-width:0;">
                <a href="%s" target="_blank" style="font-weight:600;font-size:14px;color:#111827;text-decoration:none;display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">%s</a>
                <p style="margin:4px 0 0;font-size:12px;color:#6b7280;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">%s</p>
              </div>
            </div>
            """.formatted(safeLogoUrl, safeSiteName, safeSiteUrl, safeSiteName, safeDescription);
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
