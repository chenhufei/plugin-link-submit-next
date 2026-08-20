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

    @Override
    public Mono<BasicConfig> getBasicConfig() {
        return settingFetcher.fetch(BasicGroupConfig.GROUP, BasicGroupConfig.class)
            .defaultIfEmpty(new BasicGroupConfig())
            .map(basic -> {
                var config = new BasicConfig();
                config.setLoadPlugInResources(basic.isLoadPlugInResources());
                config.setDisplayTheSubmitButton(basic.isDisplayTheSubmitButton());
                config.setEnableLinkPreview(basic.isEnableLinkPreview());
                return config;
            });
    }
}
