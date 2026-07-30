package com.kunkunyu.link.submit;

import com.kunkunyu.link.submit.extension.LinkSubmit;
import com.kunkunyu.link.submit.service.SettingConfigLinkSubmit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import run.halo.app.core.extension.notification.Subscription;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import run.halo.app.notification.NotificationCenter;
import run.halo.app.notification.UserIdentity;
import java.util.Set;

import static com.kunkunyu.link.submit.Constant.FINALIZER_NAME;
import static run.halo.app.extension.ExtensionUtil.addFinalizers;
import static run.halo.app.extension.ExtensionUtil.removeFinalizers;

/**
 * Reconciler for {@link LinkSubmit}.
 */
@Slf4j
@Component
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
            if (config.isSendEmail() && StringUtils.isNotEmpty(config.getAdminEmail())) {
                subscribeNotification(config.getAdminEmail(), Constant.ADMIN_LINK_SUBMIT);
            }
        }

        if (StringUtils.isNotEmpty(email)
            && spec.getStatus().equals(LinkSubmit.ReviewStatus.pending)) {
            subscribeNotification(email, Constant.USER_LINK_SUBMIT);
        }

        eventPublisher.publishEvent(new LinkSubmitEvent(this, linkSubmit));
    }

    private void handleStatusChange(LinkSubmit linkSubmit, LinkSubmit.LinkSubmitSpec spec, String email) {
        if (spec.getStatus().equals(LinkSubmit.ReviewStatus.refuse)
            || spec.getStatus().equals(LinkSubmit.ReviewStatus.review)) {
            if (StringUtils.isNotEmpty(email)) {
                subscribeNotification(email, Constant.REVIEW_LINK_SUBMIT);
            }
            eventPublisher.publishEvent(new ReviewLinkSubmitEvent(this, linkSubmit));
        }
    }

    private void subscribeNotification(String email, String reasonType) {
        try {
            var interestReason = new Subscription.InterestReason();
            interestReason.setReasonType(reasonType);
            interestReason.setExpression("props.email == '%s'".formatted(email));
            var subscriber = new Subscription.Subscriber();
            subscriber.setName(UserIdentity.anonymousWithEmail(email).name());
            notificationCenter.subscribe(subscriber, interestReason).block();
        } catch (Exception e) {
            log.warn("Failed to subscribe notification for email={}, reasonType={}: {}",
                email, reasonType, e.getMessage());
        }
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return builder
            .extension(new LinkSubmit())
            .build();
    }
}
