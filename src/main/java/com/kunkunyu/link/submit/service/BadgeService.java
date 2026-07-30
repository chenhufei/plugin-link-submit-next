package com.kunkunyu.link.submit.service;

public interface BadgeService {

    String generateBadge(String siteName, String logoUrl, String verifiedAt);

    String generateCardHtml(String siteName, String siteUrl, String logoUrl, String description);
}
