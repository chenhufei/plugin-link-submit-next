package run.halo.linksubmit.service;

import run.halo.linksubmit.LinkSubmitQuery;
import run.halo.linksubmit.extension.LinkSubmit;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

public interface LinkSubmitService {

    Mono<ListResult<LinkSubmit>> listLinkSubmit(LinkSubmitQuery query);
}
