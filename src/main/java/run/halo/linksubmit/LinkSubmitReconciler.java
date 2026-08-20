package run.halo.linksubmit;

import run.halo.linksubmit.extension.LinkSubmit;
import run.halo.linksubmit.service.SettingConfigLinkSubmit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import run.halo.app.core.extension.notification.Subscription;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import run.halo.app.notification.NotificationCenter;
import run.halo.app.notification.UserIdentity;
import java.util.Set;

import static run.halo.linksubmit.Constant.FINALIZER_NAME;
import static run.halo.app.extension.ExtensionUtil.addFinalizers;
import static run.halo.app.extension.ExtensionUtil.removeFinalizers;

/**
 * Reconciler for {@link LinkSubmit}.
 */
@Slf4j
@Deprecated(forRemoval = true)
@RequiredArgsConstructor
public class LinkSubmitReconciler implements Reconciler<Reconciler.Request> {

    private final ExtensionClient client;

    private final ApplicationEventPublisher eventPublisher;

    private final NotificationCenter notificationCenter;

    private final SettingConfigLinkSubmit settingConfigLinkSubmit;

    @Override
    public Result reconcile(Request request) {
        try {
            client.fetch(LinkSubmit.class, request.name())
                .ifPresent(linkSubmit -> {
                    if (ExtensionUtil.isDeleted(linkSubmit)) {
                        removeFinalizers(linkSubmit.getMetadata(), Set.of(FINALIZER_NAME));
                        client.update(linkSubmit);
                        return;
                    }

                    var spec = linkSubmit.getSpec();
                    if (spec == null || spec.getStatus() == null) {
                        log.warn("Skip link submit {} because spec or status is missing", request.name());
                        return;
                    }
                    String email = spec.getEmail();

                    if (addFinalizers(linkSubmit.getMetadata(), Set.of(FINALIZER_NAME))) {
                        handleNewSubmission(linkSubmit, spec, email);
                        client.update(linkSubmit);
                        return;
                    }

                    handleStatusChange(linkSubmit, spec, email);
                });
            return Result.doNotRetry();
        } catch (Exception e) {
            log.error("Reconcile failed for {}: {}", request.name(), e.getMessage());
            return Result.requeue(java.time.Duration.ofSeconds(30));
        }
    }

    private void handleNewSubmission(LinkSubmit linkSubmit, LinkSubmit.LinkSubmitSpec spec, String email) {
        var basicConfig = settingConfigLinkSubmit.getBasicConfig().blockOptional();
        if (basicConfig.isPresent()) {
            var config = basicConfig.get();
            if (config.isEnableAdminNotification()
                && StringUtils.isNotBlank(config.getAdminUsername())) {
                adminNoticeSubscription(config.getAdminUsername());
            }
            if (config.isSendEmail() && StringUtils.isNotEmpty(config.getAdminEmail())) {
                adminEmailSubscription(config.getAdminEmail());
            }

            if (config.isSendEmail() && StringUtils.isNotEmpty(email)
                && spec.getStatus().equals(LinkSubmit.ReviewStatus.pending)) {
                subscribeEmailNotification(email, Constant.USER_LINK_SUBMIT, "email");
            }
        }

        eventPublisher.publishEvent(new LinkSubmitEvent(this, linkSubmit));
    }

    private void handleStatusChange(LinkSubmit linkSubmit, LinkSubmit.LinkSubmitSpec spec, String email) {
        var basicConfig = settingConfigLinkSubmit.getBasicConfig().blockOptional();
        if (basicConfig.isEmpty() || !basicConfig.get().isSendEmail()) {
            return;
        }
        if (spec.getStatus().equals(LinkSubmit.ReviewStatus.refuse)
            || spec.getStatus().equals(LinkSubmit.ReviewStatus.review)) {
            if (StringUtils.isNotEmpty(email)) {
                subscribeEmailNotification(email, Constant.REVIEW_LINK_SUBMIT, "email");
            }
            eventPublisher.publishEvent(new ReviewLinkSubmitEvent(this, linkSubmit));
        }
    }

    void adminNoticeSubscription(String username) {
        subscribeUserNotification(username, Constant.ADMIN_LINK_SUBMIT, "adminUsername");
    }

    void adminEmailSubscription(String email) {
        subscribeEmailNotification(email, Constant.ADMIN_LINK_SUBMIT, "adminEmail");
    }

    private void subscribeUserNotification(String username, String reasonType, String propertyName) {
        subscribeNotification(UserIdentity.of(username).name(), username, reasonType, propertyName);
    }

    private void subscribeEmailNotification(String email, String reasonType, String propertyName) {
        subscribeNotification(UserIdentity.anonymousWithEmail(email).name(), email,
            reasonType, propertyName);
    }

    private void subscribeNotification(String subscriberName, String value,
        String reasonType, String propertyName) {
        try {
            var interestReason = new Subscription.InterestReason();
            interestReason.setReasonType(reasonType);
            String escapedValue = value.replace("'", "''");
            interestReason.setExpression("props.%s == '%s'".formatted(propertyName, escapedValue));
            var subscriber = new Subscription.Subscriber();
            subscriber.setName(subscriberName);
            notificationCenter.subscribe(subscriber, interestReason).block();
        } catch (Exception e) {
            log.warn("Failed to subscribe notification for reasonType={}: {}",
                reasonType, e.getMessage());
        }
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return builder
            .extension(new LinkSubmit())
            .build();
    }
}
