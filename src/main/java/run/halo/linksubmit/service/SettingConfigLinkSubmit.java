package run.halo.linksubmit.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import reactor.core.publisher.Mono;

public interface SettingConfigLinkSubmit {

    Mono<BasicConfig> getBasicConfig();

    @Data
    class BasicConfig {
        // basic group
        private boolean loadPlugInResources;
        private boolean enableLinkPreview;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class BasicGroupConfig {
        public static final String GROUP = "basic";
        private boolean loadPlugInResources = true;
        private boolean enableLinkPreview = true;
    }
}
