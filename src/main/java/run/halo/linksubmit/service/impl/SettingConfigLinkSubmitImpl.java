package run.halo.linksubmit.service.impl;

import run.halo.linksubmit.service.SettingConfigLinkSubmit;
import run.halo.linksubmit.service.SettingConfigLinkSubmit.BasicGroupConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;

@Component
@RequiredArgsConstructor
public class SettingConfigLinkSubmitImpl implements SettingConfigLinkSubmit {

    private final ReactiveSettingFetcher settingFetcher;

    /**
     * 设置组缺失时回落到 {@link BasicGroupConfig} 的字段默认值（两项均为开启）。
     * 这份默认值必须和 {@code extensions/settings.yaml} 里声明的 {@code value} 一致：
     * 否则「装上插件、从没动过设置」的站点会因为读不到配置而整块功能消失，
     * 而站长在后台看到的开关明明是开着的。
     */
    @Override
    public Mono<BasicGroupConfig> getBasicConfig() {
        return settingFetcher.fetch(BasicGroupConfig.GROUP, BasicGroupConfig.class)
            .defaultIfEmpty(new BasicGroupConfig());
    }
}
