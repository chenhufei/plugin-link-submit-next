package run.halo.linksubmit;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import run.halo.linksubmit.extension.CronLinkSubmit;
import run.halo.linksubmit.extension.Link;
import run.halo.linksubmit.service.LinkService;
import run.halo.linksubmit.service.SettingConfigLinkSubmit;
import run.halo.linksubmit.utils.LinkUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.ExtensionUtil;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;
import run.halo.app.extension.router.selector.FieldSelector;

import static run.halo.linksubmit.Constant.DELETE;
import static run.halo.linksubmit.Constant.MOVE;
import static run.halo.linksubmit.Constant.ORIGINAL_GROUP_NAME;
import static run.halo.app.extension.ExtensionUtil.defaultSort;
import static run.halo.app.extension.ExtensionUtil.notDeleting;

@Slf4j
@Component
public class CronLinkSubmitReconciler implements Reconciler<Reconciler.Request> {

    private final ExtensionClient client;
    private Clock clock;
    public static final String TIME_ZONE = "Asia/Shanghai";
    private final LinkService linkService;
    private final SettingConfigLinkSubmit settingConfigLinkSubmit;

    public CronLinkSubmitReconciler(ExtensionClient client, LinkService linkService,
        SettingConfigLinkSubmit settingConfigLinkSubmit) {
        this.client = client;
        this.linkService = linkService;
        this.settingConfigLinkSubmit = settingConfigLinkSubmit;
        this.clock = Clock.systemDefaultZone();
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Result reconcile(Request request) {
        return this.client.fetch(CronLinkSubmit.class, request.name())
            .map(cronLinkSubmit -> {
                if (ExtensionUtil.isDeleted(cronLinkSubmit)) {
                    return Result.doNotRetry();
                }

                var spec = cronLinkSubmit.getSpec();
                if (spec == null || !spec.isSuspend()) {
                    return Result.doNotRetry();
                }

                return processCronSchedule(cronLinkSubmit, spec);
            })
            .orElseGet(Result::doNotRetry);
    }

    private Result processCronSchedule(CronLinkSubmit cronLinkSubmit,
        CronLinkSubmit.CronLinkSubmitSpec spec) {
        String cron = spec.getCron();
        ZoneId zoneId = parseZoneId();
        if (zoneId == null) {
            return Result.doNotRetry();
        }

        if (cron == null || !CronExpression.isValidExpression(cron)) {
            log.error("Cron expression {} is invalid.", cron);
            return Result.doNotRetry();
        }

        Instant now = Instant.now(this.clock);
        CronExpression cronExp = CronExpression.parse(cron);
        var status = cronLinkSubmit.getStatus();
        if (status == null) {
            status = new CronLinkSubmit.CronLinkSubmitStatus();
            cronLinkSubmit.setStatus(status);
        }
        Instant lastScheduledTimestamp = status.getLastScheduledTimestamp();
        if (lastScheduledTimestamp == null) {
            lastScheduledTimestamp = cronLinkSubmit.getMetadata().getCreationTimestamp();
        }
        if (lastScheduledTimestamp == null) {
            lastScheduledTimestamp = now;
        }

        ZonedDateTime nextFromNow = cronExp.next(now.atZone(zoneId));
        ZonedDateTime nextFromLast = cronExp.next(lastScheduledTimestamp.atZone(zoneId));

        if (nextFromNow == null || nextFromLast == null) {
            return Result.doNotRetry();
        }

        if (Objects.equals(nextFromNow, nextFromLast)) {
            log.info("Skip scheduling and next scheduled at {}", nextFromNow);
            status.setNextSchedulingTimestamp(nextFromNow.toInstant());
            this.client.update(cronLinkSubmit);
            return new Result(true, Duration.between(now, nextFromNow));
        }

        cleanLinks(spec);
        log.info("Executed scheduled cleanup of invalid links");

        ZonedDateTime zonedNow = now.atZone(zoneId);
        ZonedDateTime scheduleTimestamp = zonedNow;
        ZonedDateTime next;
        for (next = lastScheduledTimestamp.atZone(zoneId);
             next != null && next.isBefore(zonedNow);
             next = cronExp.next(next)) {
            scheduleTimestamp = next;
        }

        status.setLastScheduledTimestamp(scheduleTimestamp.toInstant());
        if (next != null) {
            status.setNextSchedulingTimestamp(next.toInstant());
        }

        this.client.update(cronLinkSubmit);
        log.info("Scheduled at {} and next scheduled at {}", scheduleTimestamp, next);
        return next == null
            ? Result.doNotRetry()
            : new Result(true, Duration.between(now, next));
    }

    private ZoneId parseZoneId() {
        try {
            return (ZoneId) ApplicationConversionService.getSharedInstance()
                .convert(TIME_ZONE, ZoneId.class);
        } catch (DateTimeException e) {
            log.error("Invalid zone ID {}", TIME_ZONE, e);
            return null;
        }
    }

    private void cleanLinks(CronLinkSubmit.CronLinkSubmitSpec spec) {
        var cleanConfig = spec.getCleanConfig();
        if (cleanConfig == null) {
            log.warn("Skip scheduled link cleanup because cleanConfig is missing");
            return;
        }
        Optional<SettingConfigLinkSubmit.BasicConfig> basicConfig =
            settingConfigLinkSubmit.getBasicConfig().blockOptional();

        var listOptions = new ListOptions();
        listOptions.setFieldSelector(FieldSelector.of(notDeleting()));

        client.listAll(Link.class, listOptions, defaultSort())
            .forEach(link -> processLink(link, cleanConfig, basicConfig));
    }

    private void processLink(Link link, CronLinkSubmit.CleanConfig cleanConfig,
        Optional<SettingConfigLinkSubmit.BasicConfig> basicConfig) {
        var linkGroupName = link.getSpec().getGroupName();
        var withoutCheckGroupNames = Optional.ofNullable(cleanConfig.getWithoutCheckGroupNames())
            .orElseGet(List::of);
        var moveGroupName = cleanConfig.getMoveGroupName();
        var type = cleanConfig.getType();

        boolean isExempt = withoutCheckGroupNames.contains(linkGroupName);
        boolean isAlreadyMoved = CharSequenceUtil.equals(moveGroupName, linkGroupName);
        boolean isReachable = LinkUtil.urlChecker(link.getSpec().getUrl());

        if (isExempt) {
            return;
        }

        if (!isAlreadyMoved && !isReachable) {
            handleUnreachableLink(link, type, moveGroupName);
        } else if (isAlreadyMoved && isReachable) {
            handleRestoredLink(link, basicConfig);
        }
    }

    private void handleUnreachableLink(Link link, String type, String moveGroupName) {
        try {
            if (CharSequenceUtil.equals(type, DELETE)) {
                linkService.delete(link).block();
                log.info("Deleted unreachable link: {}", link.getSpec().getUrl());
            } else if (CharSequenceUtil.equals(type, MOVE)) {
                ensureAnnotationsExist(link);
                link.getMetadata().getAnnotations().put(ORIGINAL_GROUP_NAME, link.getSpec().getGroupName());
                link.getSpec().setGroupName(moveGroupName);
                linkService.update(link).block();
                log.info("Moved unreachable link to group {}: {}", moveGroupName, link.getSpec().getUrl());
            }
        } catch (Exception e) {
            log.error("Failed to process unreachable link {}: {}", link.getSpec().getUrl(), e.getMessage());
        }
    }

    private void handleRestoredLink(Link link,
        Optional<SettingConfigLinkSubmit.BasicConfig> basicConfig) {
        try {
            var annotations = link.getMetadata().getAnnotations();
            if (ObjectUtil.isEmpty(annotations)) {
                annotations = new HashMap<>();
                link.getMetadata().setAnnotations(annotations);
            }

            String originalGroupName = annotations.get(ORIGINAL_GROUP_NAME);
            if (CharSequenceUtil.isBlank(originalGroupName)) {
                originalGroupName = basicConfig
                    .map(SettingConfigLinkSubmit.BasicConfig::getGroupName)
                    .orElse(null);
            }

            if (CharSequenceUtil.isNotBlank(originalGroupName)) {
                link.getSpec().setGroupName(originalGroupName);
                annotations.remove(ORIGINAL_GROUP_NAME);
                link.getMetadata().setAnnotations(annotations);
                linkService.update(link).block();
                log.info("Restored link to group {}: {}", originalGroupName, link.getSpec().getUrl());
            }
        } catch (Exception e) {
            log.error("Failed to restore link {}: {}", link.getSpec().getUrl(), e.getMessage());
        }
    }

    private void ensureAnnotationsExist(Link link) {
        if (ObjectUtil.isEmpty(link.getMetadata().getAnnotations())) {
            link.getMetadata().setAnnotations(new HashMap<>());
        }
    }

    public Controller setupWith(ControllerBuilder builder) {
        return builder.extension(new CronLinkSubmit()).workerCount(1).build();
    }
}
