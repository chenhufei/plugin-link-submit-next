package run.halo.linksubmit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import run.halo.linksubmit.extension.LinkSubmit;
import run.halo.linksubmit.service.SettingConfigLinkSubmit;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import run.halo.app.core.extension.notification.Reason;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.MetadataUtil;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.notification.NotificationReasonEmitter;
import run.halo.app.notification.UserIdentity;
import java.util.Map;

import static run.halo.linksubmit.Constant.ADMIN_LINK_SUBMIT;
import static run.halo.linksubmit.Constant.MARK_AS_NOTIFIED;
import static run.halo.linksubmit.Constant.REVIEW_LINK_SUBMIT;
import static run.halo.linksubmit.Constant.USER_LINK_SUBMIT;
import static run.halo.linksubmit.extension.LinkSubmit.REVIEW_DESCRIPTION;

@Component
@RequiredArgsConstructor
public class NotificationReasonPublisher {

    private final ExtensionClient client;

    private final SettingConfigLinkSubmit settingConfigLinkSubmit;

    private final AdminLinkSubmitNoticeReasonPublisher adminLinkSubmitNoticeReasonPublisher;

    private final UserLinkSubmitNoticeReasonPublisher userLinkSubmitNoticeReasonPublisher;

    private final ReviewLinkSubmitNoticeReasonPublisher reviewLinkSubmitNoticeReasonPublisher;


    @Async
    @EventListener(LinkSubmitEvent.class)
    public void onPostPublished(LinkSubmitEvent event) {
        LinkSubmit linkSubmit = event.getLinkSubmit();
        var basicConfig = settingConfigLinkSubmit.getBasicConfig().blockOptional();
        if (basicConfig.isEmpty() || !basicConfig.get().isSendEmail()) {
            return;
        }
        if (StringUtils.isNotEmpty(basicConfig.get().getAdminEmail())) {
            adminLinkSubmitNoticeReasonPublisher.publishReasonBy(linkSubmit, basicConfig.get().getAdminEmail());
        }
        var spec = linkSubmit.getSpec();
        if (spec == null) {
            return;
        }
        var status = spec.getStatus();
        String email = spec.getEmail();
        if (StringUtils.isNotEmpty(email) && status == LinkSubmit.ReviewStatus.pending) {
            userLinkSubmitNoticeReasonPublisher.publishReasonBy(linkSubmit, email);
        }
    }

    @Async
    @EventListener(ReviewLinkSubmitEvent.class)
    public void onUserCouponSend(ReviewLinkSubmitEvent event) {
        LinkSubmit linkSubmit = event.getLinkSubmit();
        var basicConfig = settingConfigLinkSubmit.getBasicConfig().blockOptional();
        if (basicConfig.isEmpty() || !basicConfig.get().isSendEmail() || linkSubmit.getSpec() == null) {
            return;
        }
        String email = linkSubmit.getSpec().getEmail();
        if (StringUtils.isNotEmpty(email)) {
            var annotations = MetadataUtil.nullSafeAnnotations(linkSubmit);
            String marker = reviewNotificationMarker(linkSubmit);
            String existingMarker = annotations.get(MARK_AS_NOTIFIED);
            if (marker.equals(existingMarker)) {
                return;
            }
            reviewLinkSubmitNoticeReasonPublisher.publishReasonBy(linkSubmit, email);
            ReviewLinkSubmitMarkAsNotified(linkSubmit.getMetadata().getName(), marker);
        }
    }

    private void ReviewLinkSubmitMarkAsNotified(String name, String marker) {
        client.fetch(LinkSubmit.class, name).ifPresent(linkSubmit -> {
            var annotations = MetadataUtil.nullSafeAnnotations(linkSubmit);
            annotations.put(MARK_AS_NOTIFIED, marker);
            client.update(linkSubmit);
        });
    }

    static String reviewNotificationMarker(LinkSubmit linkSubmit) {
        var annotations = MetadataUtil.nullSafeAnnotations(linkSubmit);
        var statusValue = linkSubmit.getSpec() == null ? null : linkSubmit.getSpec().getStatus();
        String status = statusValue == null ? "" : statusValue.name();
        String description = StringUtils.defaultString(annotations.get(REVIEW_DESCRIPTION));
        return status + ":" + Integer.toHexString(description.hashCode());
    }

    @Component
    @RequiredArgsConstructor
    static class AdminLinkSubmitNoticeReasonPublisher {
        private final NotificationReasonEmitter notificationReasonEmitter;

        private final ExternalLinkProcessor externalLinkProcessor;


        public void publishReasonBy(LinkSubmit linkSubmit, String adminEmail) {
            String url = externalLinkProcessor.processLink("/console/tools/link-submit-next");
            var spec = linkSubmit.getSpec();
            var reasonSubject = Reason.Subject.builder()
                .apiVersion(linkSubmit.getApiVersion())
                .kind(linkSubmit.getKind())
                .name(linkSubmit.getMetadata().getName())
                .title(linkSubmit.getSpec().getDisplayName())
                .url(url)
                .build();
            notificationReasonEmitter.emit(ADMIN_LINK_SUBMIT,
                builder -> {
                    var attributes = ReasonData.builder()
                        .adminEmail(adminEmail)
                        .email(spec.getEmail())
                        .displayName(spec.getDisplayName())
                        .url(spec.getUrl())
                        .description(spec.getDescription())
                        .logo(spec.getLogo())
                        .oldUrl(spec.getOldUrl())
                        .groupName(spec.getGroupName())
                        .rssUrl(spec.getRssUrl())
                        .message(spec.getMessage())
                        .submittedAt(linkSubmit.getSubmitterProfile() == null
                            ? "" : linkSubmit.getSubmitterProfile().getSubmittedAt())
                        .type(spec.getType().name())
                        .review(spec.getStatus().equals(LinkSubmit.ReviewStatus.review))
                        .reviewUrl(url)
                        .build();
                    builder.attributes(ReasonDataConverter.toAttributeMap(attributes))
                        .author(UserIdentity.anonymousWithEmail(adminEmail))
                        .subject(reasonSubject);
                }).block();
        }


        @Builder
        record ReasonData(String adminEmail, String email, String displayName, String url, String description,
                          String logo, String oldUrl, String groupName, String rssUrl, String message,
                          String submittedAt, String type, Boolean review, String reviewUrl) {
        }
    }


    @Component
    @RequiredArgsConstructor
    static class UserLinkSubmitNoticeReasonPublisher {
        private final NotificationReasonEmitter notificationReasonEmitter;

        private final ExternalLinkProcessor externalLinkProcessor;


        public void publishReasonBy(LinkSubmit linkSubmit, String email) {
            String url = externalLinkProcessor.processLink("/links");
            var spec = linkSubmit.getSpec();
            var reasonSubject = Reason.Subject.builder()
                .apiVersion(linkSubmit.getApiVersion())
                .kind(linkSubmit.getKind())
                .name(linkSubmit.getMetadata().getName())
                .title(linkSubmit.getSpec().getDisplayName())
                .url(url)
                .build();
            notificationReasonEmitter.emit(USER_LINK_SUBMIT,
                builder -> {
                    var attributes = ReasonData.builder()
                        .email(email)
                        .type(spec.getType().name())
                        .displayName(spec.getDisplayName())
                        .url(spec.getUrl())
                        .groupName(spec.getGroupName())
                        .description(spec.getDescription())
                        .build();
                    builder.attributes(ReasonDataConverter.toAttributeMap(attributes))
                        .author(UserIdentity.anonymousWithEmail(email))
                        .subject(reasonSubject);
                }).block();
        }


        @Builder
        record ReasonData(String email, String type, String displayName, String url,
                          String groupName, String description) {
        }
    }

    @Component
    @RequiredArgsConstructor
    static class ReviewLinkSubmitNoticeReasonPublisher {
        private final NotificationReasonEmitter notificationReasonEmitter;

        private final ExternalLinkProcessor externalLinkProcessor;


        public void publishReasonBy(LinkSubmit linkSubmit, String email) {
            var annotations = MetadataUtil.nullSafeAnnotations(linkSubmit);
            String reviewDescription = annotations.get(REVIEW_DESCRIPTION);
            String url = externalLinkProcessor.processLink("/links");
            var spec = linkSubmit.getSpec();
            var reasonSubject = Reason.Subject.builder()
                .apiVersion(linkSubmit.getApiVersion())
                .kind(linkSubmit.getKind())
                .name(linkSubmit.getMetadata().getName())
                .title(linkSubmit.getSpec().getDisplayName())
                .url(url)
                .build();
            notificationReasonEmitter.emit(REVIEW_LINK_SUBMIT,
                builder -> {
                    var attributes = ReasonData.builder()
                        .email(email)
                        .type(spec.getType().name())
                        .displayName(spec.getDisplayName())
                        .url(spec.getUrl())
                        .oldUrl(spec.getOldUrl())
                        .groupName(spec.getGroupName())
                        .description(spec.getDescription())
                        .reviewDescription(reviewDescription)
                        .through(spec.getStatus().equals(LinkSubmit.ReviewStatus.review))
                        .build();
                    builder.attributes(ReasonDataConverter.toAttributeMap(attributes))
                        .author(UserIdentity.anonymousWithEmail(email))
                        .subject(reasonSubject);
                }).block();
        }


        @Builder
        record ReasonData(String email, String type, String displayName, String url, String oldUrl,
                          String groupName, String description, String reviewDescription,
                          Boolean through) {
        }
    }

    @UtilityClass
    static class ReasonDataConverter {
        public static <T> Map<String, Object> toAttributeMap(T data) {
            Assert.notNull(data, "Reason attributes must not be null");
            return new ObjectMapper().convertValue(data, new TypeReference<Map<String, Object>>() {
            });
        }
    }
}
