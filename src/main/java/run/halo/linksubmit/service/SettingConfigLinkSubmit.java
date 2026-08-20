package run.halo.linksubmit.service;

import lombok.Data;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SettingConfigLinkSubmit {

    Mono<BasicConfig> getBasicConfig();

    @Data
    class BasicConfig {
        // basic group
        private boolean loadPlugInResources;
        private boolean displayTheSubmitButton;
        private boolean enableLinkPreview;

        // Legacy compatibility only. The enhancement plugin no longer consumes these settings.
        private boolean autoAudit;
        private int dailySubmitLimit;
        private boolean enableHealthCheck;
        private boolean enableAdminNotification;
        private String adminUsername = "";
        private boolean sendEmail;
        private String adminEmail = "";
        private String groupName = "";
        private List<String> forbidSelectedGroupName = List.of();
    }

    @Data
    class BasicGroupConfig {
        public static final String GROUP = "basic";
        private boolean loadPlugInResources = true;
        private boolean displayTheSubmitButton = true;
        private boolean enableLinkPreview = true;
    }
}
