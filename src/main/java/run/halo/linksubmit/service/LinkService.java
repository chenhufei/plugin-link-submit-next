package run.halo.linksubmit.service;

import run.halo.linksubmit.extension.LinkSubmit;
import run.halo.linksubmit.extension.Link;
import run.halo.linksubmit.vo.LinkGroupVo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LinkService {

    Mono<Link> getName(String name);

    Flux<LinkGroupVo> listGroup();

    Mono<Boolean> isExists(String url);

    Mono<Link> create(LinkSubmit linkSubmit);

    Mono<Link> delete(Link link);

    Mono<Link> update(Link link);
}
