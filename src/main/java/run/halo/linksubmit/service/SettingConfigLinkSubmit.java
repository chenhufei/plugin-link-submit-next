package run.halo.linksubmit.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import reactor.core.publisher.Mono;

public interface SettingConfigLinkSubmit {

    Mono<BasicGroupConfig> getBasicConfig();

    /**
     * 基本设置组。字段默认值只在这里定义一次——{@code extensions/settings.yaml} 里
     * 声明的 {@code value} 必须与之一致，否则「装上没动过设置」的站点会因为读不到
     * 配置而整块功能消失。这个类同时用作「读设置」的映射类型和「缺失时回落」的实例。
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class BasicGroupConfig {
        public static final String GROUP = "basic";
        private boolean loadPlugInResources = true;
        private boolean enableLinkPreview = true;
    }
}
