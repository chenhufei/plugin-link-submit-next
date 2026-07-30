package com.kunkunyu.link.submit.service.impl;

import com.kunkunyu.link.submit.extension.Link;
import com.kunkunyu.link.submit.extension.LinkGroup;
import com.kunkunyu.link.submit.extension.LinkSubmit;
import com.kunkunyu.link.submit.service.LinkService;
import com.kunkunyu.link.submit.service.SettingConfigLinkSubmit;
import com.kunkunyu.link.submit.utils.LinkUtil;
import com.kunkunyu.link.submit.vo.LinkGroupVo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.router.selector.FieldSelector;

import java.util.HashMap;

import static org.springframework.data.domain.Sort.Order.asc;

@Component
@RequiredArgsConstructor
public class LinkServiceImpl implements LinkService {

    private final ReactiveExtensionClient client;

    private final SettingConfigLinkSubmit settingConfigLinkSubmit;

    @Override
    public Mono<Link> getName(String name) {
        return client.fetch(Link.class, name);
    }

    @Override
    public Flux<LinkGroupVo> listGroup() {
        var listOptions = new ListOptions();
        listOptions.setFieldSelector(FieldSelector.all());
        return client.listAll(LinkGroup.class, listOptions, defaultLinkSort())
            .map(LinkGroupVo::from);
    }

    public Flux<Link> listLink() {
        var listOptions = new ListOptions();
        listOptions.setFieldSelector(FieldSelector.all());
        return client.listAll(Link.class, listOptions, Sort.unsorted());
    }

    @Override
    public Mono<Boolean> isExists(String url) {
        return listLink()
            .filter(link -> url.equals(LinkUtil.getDomain(link.getSpec().getUrl())))
            .hasElements();
    }

    @Override
    public Mono<Link> create(LinkSubmit linkSubmit) {
        var basicConfig = settingConfigLinkSubmit.getBasicConfig();

        return basicConfig.flatMap(basic -> {
            var linkSubmitSpec = linkSubmit.getSpec();

            Link link = new Link();
            Link.LinkSpec spec = new Link.LinkSpec();
            spec.setUrl(linkSubmitSpec.getUrl());
            spec.setDisplayName(linkSubmitSpec.getDisplayName());
            spec.setDescription(linkSubmitSpec.getDescription());
            spec.setLogo(linkSubmitSpec.getLogo());

            if (StringUtils.isEmpty(linkSubmitSpec.getGroupName())) {
                spec.setGroupName(basic.getGroupName());
            } else {
                spec.setGroupName(linkSubmitSpec.getGroupName());
            }

            Metadata metadata = new Metadata();
            metadata.setGenerateName("link-");
            var annotations = new HashMap<String, String>();
            if (StringUtils.isNotEmpty(linkSubmitSpec.getEmail())) {
                annotations.put("email", linkSubmitSpec.getEmail());
            }
            if (StringUtils.isNotEmpty(linkSubmitSpec.getRssUrl())) {
                annotations.put("rss_url", linkSubmitSpec.getRssUrl());
            }
            metadata.setAnnotations(annotations);
            link.setMetadata(metadata);
            link.setSpec(spec);

            return client.create(link);
        });
    }

    @Override
    public Mono<Link> delete(Link link) {
        return client.delete(link);
    }

    @Override
    public Mono<Link> update(Link link) {
        return client.update(link);
    }

    static Sort defaultLinkSort() {
        return Sort.by(asc("spec.priority"),
            asc("metadata.creationTimestamp"),
            asc("metadata.name")
        );
    }
}
