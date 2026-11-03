package run.halo.linksubmit.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.linksubmit.service.SettingConfigLinkSubmit.BasicGroupConfig;

/**
 * 设置读取的默认值契约。
 *
 * <p>「设置组缺失」和「用户显式关闭」必须给出不同结果：前者回落到 settings.yaml 声明的
 * 默认值（开启），后者必须如实关闭。混为一谈时，要么装上插件功能就消失，
 * 要么站长关了开关却关不掉。
 */
class SettingConfigLinkSubmitImplTest {

    private final ReactiveSettingFetcher settingFetcher = mock(ReactiveSettingFetcher.class);

    private final SettingConfigLinkSubmitImpl service =
        new SettingConfigLinkSubmitImpl(settingFetcher);

    private void stubGroup(Mono<BasicGroupConfig> result) {
        when(settingFetcher.fetch(eq(BasicGroupConfig.GROUP), eq(BasicGroupConfig.class)))
            .thenReturn(result);
    }

    @Test
    @DisplayName("设置组缺失时回落到与后台一致的默认值（均开启）")
    void missingGroupFallsBackToDeclaredDefaults() {
        stubGroup(Mono.empty());

        var config = service.getBasicConfig().block();

        assertThat(config).isNotNull();
        assertThat(config.isEnableLinkPreview()).isTrue();
        assertThat(config.isLoadPlugInResources()).isTrue();
    }

    @Test
    @DisplayName("用户显式关闭时不能被默认值覆盖")
    void explicitFalseIsRespected() {
        var group = new BasicGroupConfig();
        group.setEnableLinkPreview(false);
        group.setLoadPlugInResources(false);
        stubGroup(Mono.just(group));

        var config = service.getBasicConfig().block();

        assertThat(config).isNotNull();
        assertThat(config.isEnableLinkPreview()).isFalse();
        assertThat(config.isLoadPlugInResources()).isFalse();
    }
}
