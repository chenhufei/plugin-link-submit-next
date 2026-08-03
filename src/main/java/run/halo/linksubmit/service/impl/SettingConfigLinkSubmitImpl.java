package run.halo.linksubmit.service.impl;

import run.halo.linksubmit.service.SettingConfigLinkSubmit;
import run.halo.linksubmit.service.SettingConfigLinkSubmit.BasicGroupConfig;
import run.halo.linksubmit.service.SettingConfigLinkSubmit.LinkGroupConfig;
import run.halo.linksubmit.service.SettingConfigLinkSubmit.NotificationGroupConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

@Component
@RequiredArgsConstructor
public class SettingConfigLinkSubmitImpl implements SettingConfigLinkSubmit {

    private final ReactiveSettingFetcher settingFetcher;

    @Override
    public Mono<BasicConfig> getBasicConfig() {
        return Mono.zip(
                settingFetcher.fetch(BasicGroupConfig.GROUP, BasicGroupConfig.class)
                    .defaultIfEmpty(new BasicGroupConfig()),
                settingFetcher.fetch(NotificationGroupConfig.GROUP, NotificationGroupConfig.class)
                    .defaultIfEmpty(new NotificationGroupConfig()),
                settingFetcher.fetch(LinkGroupConfig.GROUP, LinkGroupConfig.class)
                    .defaultIfEmpty(new LinkGroupConfig())
            )
            .map(tuple -> {
                var basic = tuple.getT1();
                var notification = tuple.getT2();
                var link = tuple.getT3();

                var config = new BasicConfig();
                // basic group
                config.setLoadPlugInResources(basic.isLoadPlugInResources());
                config.setDisplayTheSubmitButton(basic.isDisplayTheSubmitButton());
                config.setAutoAudit(basic.isAutoAudit());
                config.setDailySubmitLimit(basic.getDailySubmitLimit());
                config.setEnableHealthCheck(basic.isEnableHealthCheck());
                config.setEnableLinkPreview(basic.isEnableLinkPreview());
                // notification group
                config.setSendEmail(notification.isSendEmail());
                config.setAdminEmail(notification.getAdminEmail());
                config.setEnableWebhook(notification.isEnableWebhook());
                config.setWebhookUrl(notification.getWebhookUrl());
                config.setWebhookSecret(notification.getWebhookSecret());
                // link group
                config.setGroupName(link.getGroupName());
                config.setForbidSelectedGroupName(link.getForbidSelectedGroupName());
                return config;
            });
    }
}
