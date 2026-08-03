package run.halo.linksubmit.service;

import run.halo.linksubmit.LinkSubmitQuery;
import run.halo.linksubmit.endpoint.AnonymousEndpoint;
import run.halo.linksubmit.endpoint.LinkSubmitEndpoint;
import run.halo.linksubmit.extension.LinkSubmit;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

public interface LinkSubmitService {

    Mono<ListResult<LinkSubmit>> listLinkSubmit(LinkSubmitQuery query);

    Mono<LinkSubmit> createLinkSubmit(AnonymousEndpoint.CreateLinkSubmitRequest createLinkSubmitRequest, String clientIp);

    Mono<LinkSubmit> checkLink(String name, LinkSubmitEndpoint.CheckLinkSubmitRequest checkLinkSubmitRequest);
}
