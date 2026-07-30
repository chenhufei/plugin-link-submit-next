package com.kunkunyu.link.submit.service;

import com.kunkunyu.link.submit.LinkSubmitQuery;
import com.kunkunyu.link.submit.endpoint.AnonymousEndpoint;
import com.kunkunyu.link.submit.endpoint.LinkSubmitEndpoint;
import com.kunkunyu.link.submit.extension.LinkSubmit;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListResult;

public interface LinkSubmitService {

    Mono<ListResult<LinkSubmit>> listLinkSubmit(LinkSubmitQuery query);

    Mono<LinkSubmit> createLinkSubmit(AnonymousEndpoint.CreateLinkSubmitRequest createLinkSubmitRequest, String clientIp);

    Mono<LinkSubmit> checkLink(String name, LinkSubmitEndpoint.CheckLinkSubmitRequest checkLinkSubmitRequest);
}
