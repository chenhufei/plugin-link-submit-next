package run.halo.linksubmit.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

import java.time.Instant;

import static run.halo.linksubmit.extension.LinkSubmit.KIND;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "link.submit.halo.run", version = "v1alpha1",
    kind = KIND, plural = "linksubmits", singular = "linksubmit")
@AllArgsConstructor
@NoArgsConstructor
public class LinkSubmit extends AbstractExtension {

    public static final String KIND = "LinkSubmit";

    public static final String REVIEW_DESCRIPTION = "link.submit.halo.run/review-description";

    @Schema(requiredMode = REQUIRED)
    private LinkSubmitSpec spec;

    private HealthStatus healthStatus;

    private SubmitterProfile submitterProfile;

    @Data
    public static class LinkSubmitSpec {

        @Schema(requiredMode = REQUIRED)
        private String url;

        @Schema(requiredMode = REQUIRED)
        private String displayName;

        private String logo;

        private String description;

        private String oldUrl;

        private String email;

        private String groupName;

        private String rssUrl;

        private String message;

        @Schema(requiredMode = REQUIRED)
        private LinkSubmitType type;

        @Schema(requiredMode = REQUIRED)
        private ReviewStatus status;
    }

    @Data
    public static class HealthStatus {
        private Boolean reachable;
        private Integer httpStatusCode;
        private Long responseTimeMs;
        private Boolean sslValid;
        private Boolean hiddenLinkDetected;
        private Instant lastCheckedAt;
        private String errorMessage;
    }

    @Data
    public static class SubmitterProfile {
        private String userAgent;
        private String language;
        private String timezone;
        private String screenSize;
        private String clientIp;
        private String submittedAt;
    }

    public enum LinkSubmitType {
        add,
        update;
    }

    public enum ReviewStatus {
        review,
        pending,
        refuse;
    }
}
