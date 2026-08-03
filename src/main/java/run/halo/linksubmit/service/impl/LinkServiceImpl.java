package run.halo.linksubmit.service.impl;

import run.halo.linksubmit.extension.Link;
import run.halo.linksubmit.extension.LinkSubmit;
import run.halo.linksubmit.service.LinkService;
import run.halo.linksubmit.service.SettingConfigLinkSubmit;
import run.halo.linksubmit.utils.LinkUtil;
import run.halo.linksubmit.vo.LinkGroupVo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.router.selector.FieldSelector;

import java.util.HashMap;

import static org.springframework.data.domain.Sort.Order.asc;
import static run.halo.app.extension.ExtensionUtil.notDeleting;

@Component
@RequiredArgsConstructor
public class LinkServiceImpl implements LinkService {

    private final OfficialLinksClient officialLinksClient;

    private final SettingConfigLinkSubmit settingConfigLinkSubmit;

    @Override
    public Mono<Link> getName(String name) {
        return officialLinksClient.fetchLink(name);
    }

    @Override
    public Flux<LinkGroupVo> listGroup() {
        var listOptions = new ListOptions();
        listOptions.setFieldSelector(FieldSelector.all());
        return officialLinksClient.listGroups(listOptions, defaultLinkSort())
            .map(LinkGroupVo::from);
    }

    @Override
    public Flux<Link> listLink() {
        var listOptions = new ListOptions();
        listOptions.setFieldSelector(FieldSelector.of(notDeleting()));
        return officialLinksClient.listLinks(listOptions, Sort.unsorted());
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

            return officialLinksClient.createLink(link);
        });
    }

    @Override
    public Mono<Link> delete(Link link) {
        return officialLinksClient.deleteLink(link);
    }

    @Override
    public Mono<Link> update(Link link) {
        return officialLinksClient.updateLink(link);
    }

    static Sort defaultLinkSort() {
        return Sort.by(asc("spec.priority"),
            asc("metadata.creationTimestamp"),
            asc("metadata.name")
        );
    }
}
