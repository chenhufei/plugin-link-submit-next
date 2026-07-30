package com.kunkunyu.link.submit.service.impl;

import com.kunkunyu.link.submit.extension.Link;
import com.kunkunyu.link.submit.service.HealthCheckService;
import com.kunkunyu.link.submit.service.SettingConfigLinkSubmit;
import com.kunkunyu.link.submit.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.router.selector.FieldSelector;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyReportServiceImpl implements WeeklyReportService {

    private final ReactiveExtensionClient client;

    private final HealthCheckService healthCheckService;

    private final SettingConfigLinkSubmit settingConfigLinkSubmit;

    @Override
    public String generateWeeklyReport() {
        var listOptions = new ListOptions();
        listOptions.setFieldSelector(FieldSelector.all());

        var links = client.listAll(Link.class, listOptions, Sort.unsorted()).collectList().block();

        if (links == null || links.isEmpty()) {
            return "暂无友链数据";
        }

        AtomicInteger total = new AtomicInteger(0);
        AtomicInteger healthy = new AtomicInteger(0);
        AtomicInteger unhealthy = new AtomicInteger(0);

        StringBuilder report = new StringBuilder();
        report.append("# 友链健康周报\n\n");
        report.append("生成时间：").append(Instant.now().atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        report.append("## 统计概览\n\n");
        report.append("- 总链接数：").append(links.size()).append("\n\n");

        report.append("## 链接详情\n\n");
        report.append("| 网站名称 | URL | 状态 | 响应时间 | SSL |\n");
        report.append("|---------|-----|------|---------|-----|\n");

        for (Link link : links) {
            total.incrementAndGet();
            String url = link.getSpec().getUrl();
            String name = link.getSpec().getDisplayName();

            try {
                var health = healthCheckService.checkLinkHealth(url).block();
                if (health != null && Boolean.TRUE.equals(health.getReachable())) {
                    healthy.incrementAndGet();
                    report.append("| ").append(name).append(" | ").append(url)
                        .append(" | ✅ 正常 | ").append(health.getResponseTimeMs()).append("ms")
                        .append(" | ").append(Boolean.TRUE.equals(health.getSslValid()) ? "✅" : "❌")
                        .append(" |\n");
                } else {
                    unhealthy.incrementAndGet();
                    report.append("| ").append(name).append(" | ").append(url)
                        .append(" | ❌ 异常 | - | - |\n");
                }
            } catch (Exception e) {
                unhealthy.incrementAndGet();
                report.append("| ").append(name).append(" | ").append(url)
                    .append(" | ⚠️ 检测失败 | - | - |\n");
            }
        }

        report.append("\n## 健康度\n\n");
        report.append("- 健康：").append(healthy.get()).append("\n");
        report.append("- 异常：").append(unhealthy.get()).append("\n");
        report.append("- 健康率：").append(total.get() > 0 ?
            String.format("%.1f%%", healthy.get() * 100.0 / total.get()) : "N/A").append("\n");

        return report.toString();
    }

    @Override
    public void sendWeeklyReport(String email) {
        String report = generateWeeklyReport();
        log.info("Weekly report generated for {}, length: {}", email, report.length());
    }
}
