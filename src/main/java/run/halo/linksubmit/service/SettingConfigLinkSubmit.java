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
        private boolean autoAudit;
        private int dailySubmitLimit;
        private boolean enableHealthCheck;
        private boolean enableLinkPreview;

        // notification group
        private boolean sendEmail;
        private String adminEmail;
        private boolean enableWebhook;
        private String webhookUrl;
        private String webhookSecret;

        // link group
        private String groupName;
        private List<String> forbidSelectedGroupName;
    }

    @Data
    class BasicGroupConfig {
        public static final String GROUP = "basic";
        private boolean loadPlugInResources = true;
        private boolean displayTheSubmitButton = true;
        private boolean autoAudit;
        private int dailySubmitLimit;
        private boolean enableHealthCheck = true;
        private boolean enableLinkPreview = true;
    }

    @Data
    class NotificationGroupConfig {
        public static final String GROUP = "notification";
        private boolean sendEmail;
        private String adminEmail = "";
        private boolean enableWebhook;
        private String webhookUrl = "";
        private String webhookSecret = "";
    }

    @Data
    class LinkGroupConfig {
        public static final String GROUP = "link";
        private String groupName = "";
        private List<String> forbidSelectedGroupName = List.of();
    }
}
