package run.halo.linksubmit.endpoint;

import run.halo.linksubmit.LinkSubmitQuery;
import run.halo.linksubmit.extension.LinkSubmit;
import run.halo.linksubmit.service.LinkSubmitService;
import lombok.RequiredArgsConstructor;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ListResult;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;

@Component
@RequiredArgsConstructor
public class LinkSubmitEndpoint implements CustomEndpoint {

    private static final String TAG = "console.api.link.submit.halo.run/v1alpha1/ListLinkSubmit";

    private final LinkSubmitService linkSubmitService;

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return SpringdocRouteBuilder.route()
            .GET("linksubmits", this::listLinkSubmits, builder -> {
                    builder.operationId("ListLinkSubmits")
                        .description("List LinkSubmits.")
                        .tag(TAG)
                        .response(responseBuilder()
                            .implementation(ListResult.generateGenericClass(LinkSubmit.class))
                        );
                    LinkSubmitQuery.buildParameters(builder);
                }
            )
            .build();
    }

    Mono<ServerResponse> listLinkSubmits(ServerRequest request) {
        LinkSubmitQuery query = new LinkSubmitQuery(request);
        return linkSubmitService.listLinkSubmit(query)
            .flatMap(linkSubmits -> ServerResponse.ok().bodyValue(linkSubmits));
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.link.submit.halo.run/v1alpha1");
    }
}
