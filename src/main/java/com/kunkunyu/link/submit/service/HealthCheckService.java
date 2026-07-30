package com.kunkunyu.link.submit.service;

import com.kunkunyu.link.submit.extension.LinkSubmit;
import reactor.core.publisher.Mono;

public interface HealthCheckService {

    Mono<LinkSubmit.HealthStatus> checkLinkHealth(String url);

    Mono<Boolean> detectHiddenLink(String url, String ownDomain);

    Mono<Void> batchHealthCheck();
}
