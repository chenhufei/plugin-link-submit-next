package com.kunkunyu.link.submit.service.impl;

import com.kunkunyu.link.submit.extension.Link;
import com.kunkunyu.link.submit.service.HealthCheckService;
import com.kunkunyu.link.submit.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.router.selector.FieldSelector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final ReactiveExtensionClient client;

    private final HealthCheckService healthCheckService;

    @Override
    public List<Map<String, Object>> getLinkRanking(int limit) {
        var listOptions = new ListOptions();
        listOptions.setFieldSelector(FieldSelector.all());

        List<Link> links = client.listAll(Link.class, listOptions, Sort.unsorted()).collectList().block();

        if (links == null || links.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> rankings = new ArrayList<>();

        for (Link link : links) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", link.getMetadata().getName());
            item.put("displayName", link.getSpec().getDisplayName());
            item.put("url", link.getSpec().getUrl());
            item.put("logo", link.getSpec().getLogo());
            item.put("groupName", link.getSpec().getGroupName());

            int score = calculateScore(link.getSpec().getUrl());
            item.put("score", score);

            rankings.add(item);
        }

        rankings.sort(Comparator.comparingInt((Map<String, Object> m) -> (int) m.get("score")).reversed());

        return rankings.subList(0, Math.min(limit, rankings.size()));
    }

    @Override
    public int calculateScore(String url) {
        int score = 50;

        try {
            var health = healthCheckService.checkLinkHealth(url).block();
            if (health != null) {
                if (Boolean.TRUE.equals(health.getReachable())) {
                    score += 20;
                }
                if (Boolean.TRUE.equals(health.getSslValid())) {
                    score += 10;
                }
                if (health.getResponseTimeMs() != null) {
                    if (health.getResponseTimeMs() < 1000) {
                        score += 15;
                    } else if (health.getResponseTimeMs() < 3000) {
                        score += 10;
                    } else if (health.getResponseTimeMs() < 5000) {
                        score += 5;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Health check failed for scoring: {}", url);
        }

        if (url.startsWith("https")) {
            score += 5;
        }

        return Math.min(100, score);
    }
}
