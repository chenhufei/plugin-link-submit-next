package run.halo.linksubmit.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;
import run.halo.app.extension.PageRequestImpl;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.linksubmit.LinkSubmitQuery;
import run.halo.linksubmit.extension.LinkSubmit;
import run.halo.linksubmit.service.LinkSubmitService;

/**
 * Read-only access to legacy application records created before PluginLinks 2.3.0 integration.
 */
@Component
@RequiredArgsConstructor
public class LinkSubmitServiceImpl implements LinkSubmitService {

    private final ReactiveExtensionClient client;

    @Override
    public Mono<ListResult<LinkSubmit>> listLinkSubmit(LinkSubmitQuery query) {
        return client.listBy(LinkSubmit.class, query.toListOptions(),
            PageRequestImpl.of(query.getPage(), query.getSize(), query.getSort()));
    }
}
