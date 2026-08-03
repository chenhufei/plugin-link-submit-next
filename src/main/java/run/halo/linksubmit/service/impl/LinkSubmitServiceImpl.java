package run.halo.linksubmit.service.impl;

import run.halo.linksubmit.LinkSubmitQuery;
import run.halo.linksubmit.endpoint.AnonymousEndpoint;
import run.halo.linksubmit.endpoint.LinkSubmitEndpoint;
import run.halo.linksubmit.extension.Link;
import run.halo.linksubmit.extension.LinkSubmit;
import run.halo.linksubmit.service.HealthCheckService;
import run.halo.linksubmit.service.LinkService;
import run.halo.linksubmit.service.LinkSubmitService;
import run.halo.linksubmit.service.SettingConfigLinkSubmit;
import run.halo.linksubmit.utils.CommonUtil;
import run.halo.linksubmit.utils.LinkUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import lombok.extern.slf4j.Slf4j;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.MetadataUtil;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import org.springframework.web.server.ServerWebInputException;
import run.halo.app.extension.router.selector.FieldSelector;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static run.halo.linksubmit.extension.LinkSubmit.REVIEW_DESCRIPTION;
import static run.halo.app.extension.index.query.Queries.and;
import static run.halo.app.extension.index.query.Queries.contains;
import static run.halo.app.extension.index.query.Queries.equal;
import static run.halo.app.extension.index.query.Queries.greaterThan;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkSubmitServiceImpl implements LinkSubmitService {

    private final ReactiveExtensionClient client;

    private final LinkService linkService;

    private final SettingConfigLinkSubmit settingConfigLinkSubmit;

    private final CommonUtil commonUtil;

    private final HealthCheckService healthCheckService;

    @Override
    public Mono<ListResult<LinkSubmit>> listLinkSubmit(LinkSubmitQuery query) {
        return client.listBy(LinkSubmit.class, query.toListOptions(),
            PageRequestImpl.of(query.getPage(), query.getSize(), query.getSort()));
    }

    @Override
    public Mono<LinkSubmit> createLinkSubmit(AnonymousEndpoint.CreateLinkSubmitRequest createLinkSubmitRequest, String clientIp) {

        String url = createLinkSubmitRequest.getUrl();
        String displayName = createLinkSubmitRequest.getDisplayName();
        String logo = createLinkSubmitRequest.getLogo();
        String email = createLinkSubmitRequest.getEmail();
        LinkSubmit.LinkSubmitType type = createLinkSubmitRequest.getType();
        String oldUrl = createLinkSubmitRequest.getOldUrl();

        if (StringUtils.isEmpty(url)) {
            return Mono.error(new ServerWebInputException("网站地址不能为空！"));
        }
        if (StringUtils.isEmpty(displayName)) {
            return Mono.error(new ServerWebInputException("网站名称不能为空！"));
        }
        if (!LinkUtil.isValidUrl(url)) {
            return Mono.error(new ServerWebInputException("网站地址格式有误！"));
        }
        if (!StringUtils.isEmpty(logo) && !LinkUtil.isValidUrl(createLinkSubmitRequest.getLogo())) {
            return Mono.error(new ServerWebInputException("网站Logo地址格式有误！"));
        }
        if (!StringUtils.isEmpty(email) && !commonUtil.isValidEmail(email)) {
            return Mono.error(new ServerWebInputException("邮箱格式有误！"));
        }

        return settingConfigLinkSubmit.getBasicConfig()
            .flatMap(basicConfig -> {
                int dailyLimit = basicConfig.getDailySubmitLimit();
                if (dailyLimit > 0 && StringUtils.isNotEmpty(clientIp)) {
                    return countTodaySubmissions(clientIp)
                        .flatMap(todayCount -> {
                            if (todayCount >= dailyLimit) {
                                return Mono.<LinkSubmit>error(new ServerWebInputException(
                                    "今日提交次数已达上限（" + dailyLimit + "次），请明天再试！"));
                            }
                            return processCreateLinkSubmit(createLinkSubmitRequest, clientIp,
                                type, oldUrl, url, email, displayName, logo);
                        });
                }
                return processCreateLinkSubmit(createLinkSubmitRequest, clientIp,
                    type, oldUrl, url, email, displayName, logo);
            });
    }

    private Mono<LinkSubmit> processCreateLinkSubmit(
        AnonymousEndpoint.CreateLinkSubmitRequest createLinkSubmitRequest,
        String clientIp, LinkSubmit.LinkSubmitType type, String oldUrl,
        String url, String email, String displayName, String logo) {

        String domain = LinkUtil.getDomain(url);
        if (type.equals(LinkSubmit.LinkSubmitType.update)) {
            if (StringUtils.isEmpty(oldUrl)) {
                return Mono.error(new ServerWebInputException("请填写旧的站点链接！"));
            }
            domain = LinkUtil.getDomain(oldUrl);
        }
        String finalDomain = domain;

        return linkService.isExists(domain)
            .flatMap(exists -> {
                if (type.equals(LinkSubmit.LinkSubmitType.add)) {
                    if (exists) {
                        return Mono.error(new ServerWebInputException("链接已存在！"));
                    }
                }
                if (type.equals(LinkSubmit.LinkSubmitType.update)) {
                    if (!exists) {
                        return Mono.error(new ServerWebInputException("链接不存在，请提交链接，而不是提交修改链接！"));
                    }
                }

                LinkSubmit linkSubmit = new LinkSubmit();
                Metadata metadata = new Metadata();
                metadata.setGenerateName("link-submit-");
                if (StringUtils.isNotEmpty(clientIp)) {
                    metadata.setAnnotations(new java.util.HashMap<>(java.util.Map.of("submitter-ip", clientIp)));
                }
                linkSubmit.setMetadata(metadata);
                LinkSubmit.LinkSubmitSpec linkSubmitSpec = new LinkSubmit.LinkSubmitSpec();
                linkSubmitSpec.setUrl(url);
                linkSubmitSpec.setDisplayName(displayName);
                linkSubmitSpec.setLogo(logo);
                linkSubmitSpec.setDescription(createLinkSubmitRequest.getDescription());
                if (type.equals(LinkSubmit.LinkSubmitType.update)) {
                    linkSubmitSpec.setOldUrl(createLinkSubmitRequest.getOldUrl());
                }
                linkSubmitSpec.setEmail(email);
                linkSubmitSpec.setGroupName(createLinkSubmitRequest.getGroupName());
                linkSubmitSpec.setRssUrl(createLinkSubmitRequest.getRssUrl());
                linkSubmitSpec.setMessage(createLinkSubmitRequest.getMessage());
                linkSubmitSpec.setType(createLinkSubmitRequest.getType());
                linkSubmitSpec.setStatus(LinkSubmit.ReviewStatus.pending);
                linkSubmit.setSpec(linkSubmitSpec);

                LinkSubmit.SubmitterProfile profile = new LinkSubmit.SubmitterProfile();
                profile.setClientIp(clientIp);
                profile.setSubmittedAt(Instant.now().atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                linkSubmit.setSubmitterProfile(profile);

                return createNewLink(finalDomain, linkSubmit);
            });
    }

    @Override
    public Mono<LinkSubmit> checkLink(String name, LinkSubmitEndpoint.CheckLinkSubmitRequest checkLinkSubmitRequest) {
        return client.fetch(LinkSubmit.class, name)
            .filter(linkSubmit -> linkSubmit.getSpec().getStatus().equals(LinkSubmit.ReviewStatus.pending))
            .switchIfEmpty(Mono.error(new ServerWebInputException("已审核或不存在！")))
            .flatMap(linkSubmit -> {
                var spec = linkSubmit.getSpec();
                Boolean checkStatus = checkLinkSubmitRequest.getCheckStatus();
                String linkName = checkLinkSubmitRequest.getLinkName();
                String reason = checkLinkSubmitRequest.getReason();
                var annotations = MetadataUtil.nullSafeAnnotations(linkSubmit);
                if (StringUtils.isNotEmpty(reason)) {
                    annotations.put(REVIEW_DESCRIPTION, reason);
                }

                spec.setStatus(checkStatus ? LinkSubmit.ReviewStatus.review : LinkSubmit.ReviewStatus.refuse);
                if (spec.getType().equals(LinkSubmit.LinkSubmitType.add)) {
                    if (checkStatus) {
                        return linkService.create(linkSubmit)
                            .then(client.update(linkSubmit));
                    }
                    return client.update(linkSubmit);
                } else {
                    if (checkStatus) {
                        return linkService.getName(linkName)
                            .switchIfEmpty(Mono.error(new ServerWebInputException("链接不存在！")))
                            .flatMap(link -> updateLink(link, linkSubmit))
                            .then(client.update(linkSubmit));
                    }
                    return client.update(linkSubmit);
                }
            });
    }

    private Mono<Link> updateLink(Link link, LinkSubmit linkSubmit) {
        var linkSubmitSpec = linkSubmit.getSpec();
        var spec = link.getSpec();
        spec.setUrl(linkSubmitSpec.getUrl());
        spec.setDisplayName(linkSubmitSpec.getDisplayName());
        spec.setLogo(linkSubmitSpec.getLogo());
        spec.setGroupName(linkSubmitSpec.getGroupName());
        spec.setDescription(linkSubmitSpec.getDescription());
        var annotations = MetadataUtil.nullSafeAnnotations(link);
        if (StringUtils.isNotEmpty(linkSubmitSpec.getEmail())) {
            annotations.put("email", linkSubmitSpec.getEmail());
        }
        if (StringUtils.isNotEmpty(linkSubmitSpec.getRssUrl())) {
            annotations.put("rss_url", linkSubmitSpec.getRssUrl());
        }
        return linkService.update(link);
    }

    private Mono<LinkSubmit> createNewLink(String submitDomain, LinkSubmit linkSubmit) {
        var basicConfig = settingConfigLinkSubmit.getBasicConfig();

        return basicConfig.flatMap(basic -> {
            String domain = commonUtil.getDomain();
            var spec = linkSubmit.getSpec();

            if (LinkUtil.hasLinkByUrl(spec.getUrl(), domain)) {
                return Mono.error(new ServerWebInputException("请不要输入本站地址！"));
            }

            return linkSubmitExistence(submitDomain, spec.getType().name())
                .flatMap(exists -> {
                    boolean checkFlag = basic.isAutoAudit();
                    if (exists) {
                        return Mono.error(new ServerWebInputException("请勿重复提交，请等待审核！"));
                    }

                    if (basic.isEnableHealthCheck()) {
                        return healthCheckService.checkLinkHealth(spec.getUrl())
                            .onErrorResume(e -> {
                                log.warn("Health check failed for {}, continuing without health check: {}",
                                    spec.getUrl(), e.getMessage());
                                return Mono.just(new LinkSubmit.HealthStatus());
                            })
                            .flatMap(health -> {
                                linkSubmit.setHealthStatus(health);
                                // 在非响应式线程中获取 favicon
                                return Mono.fromCallable(() -> {
                                        if (StringUtils.isEmpty(spec.getLogo())) {
                                            String favicon = LinkUtil.getFavicon(spec.getUrl());
                                            if (!StringUtils.isEmpty(favicon) && LinkUtil.checkFavicon(favicon)) {
                                                spec.setLogo(favicon);
                                            }
                                        }
                                        return true;
                                    }).subscribeOn(Schedulers.boundedElastic())
                                    .flatMap(ignored -> {
                                        if (checkFlag && spec.getType().equals(LinkSubmit.LinkSubmitType.add)) {
                                            return linkService.create(linkSubmit).flatMap(linkNew -> {
                                                spec.setStatus(LinkSubmit.ReviewStatus.review);
                                                return client.create(linkSubmit);
                                            });
                                        } else {
                                            return client.create(linkSubmit);
                                        }
                                    });
                            });
                    }

                    // 无健康检测时也在独立线程中获取 favicon
                    return Mono.fromCallable(() -> {
                            if (StringUtils.isEmpty(spec.getLogo())) {
                                String favicon = LinkUtil.getFavicon(spec.getUrl());
                                if (!StringUtils.isEmpty(favicon) && LinkUtil.checkFavicon(favicon)) {
                                    spec.setLogo(favicon);
                                }
                            }
                            return true;
                        }).subscribeOn(Schedulers.boundedElastic())
                        .flatMap(ignored -> {
                            if (checkFlag && spec.getType().equals(LinkSubmit.LinkSubmitType.add)) {
                                return linkService.create(linkSubmit).flatMap(linkNew -> {
                                    spec.setStatus(LinkSubmit.ReviewStatus.review);
                                    return client.create(linkSubmit);
                                });
                            } else {
                                return client.create(linkSubmit);
                            }
                        });
                });
        }).onErrorResume(e -> {
            if (e instanceof ServerWebInputException) {
                return Mono.error(e);
            }
            log.error("Failed to create link submit: {}", e.getMessage(), e);
            return Mono.error(new ServerWebInputException("提交失败，请稍后重试！"));
        });
    }

    public Mono<Boolean> linkSubmitExistence(String url, String type) {
        var listOptions = new ListOptions();
        FieldSelector fieldSelector = FieldSelector.of(and(equal("spec.type", type),
            equal("spec.status", LinkSubmit.ReviewStatus.pending.name())));
        if (type.equals(LinkSubmit.LinkSubmitType.add.name())) {
            fieldSelector = fieldSelector.andQuery(contains("spec.url", url));
        } else {
            fieldSelector = fieldSelector.andQuery(contains("spec.oldUrl", url));
        }

        listOptions.setFieldSelector(fieldSelector);
        return client.listAll(LinkSubmit.class, listOptions, Sort.unsorted()).hasElements();
    }

    private Mono<Long> countTodaySubmissions(String clientIp) {
        var todayStart = Instant.now().atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant();
        var listOptions = ListOptions.builder()
            .fieldQuery(and(
                equal("metadata.annotations.submitter-ip", clientIp),
                greaterThan("metadata.creationTimestamp", todayStart, true)
            ))
            .build();
        return client.listAll(LinkSubmit.class, listOptions, Sort.unsorted()).count();
    }
}
