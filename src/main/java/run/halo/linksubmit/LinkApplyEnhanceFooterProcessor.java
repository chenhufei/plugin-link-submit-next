package run.halo.linksubmit;

import java.util.Properties;
import com.google.common.base.Throwables;
import run.halo.linksubmit.service.SettingConfigLinkSubmit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.RouteMatcher;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;
import org.springframework.web.util.pattern.PatternParseException;
import org.thymeleaf.context.Contexts;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.web.IWebRequest;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.PluginContext;
import run.halo.app.theme.dialect.TemplateFooterProcessor;

/**
 * Injects the site-info auto fill enhancement into the official PluginLinks application form.
 * The form markup, submission, captcha and review all belong to PluginLinks and the theme.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LinkApplyEnhanceFooterProcessor implements TemplateFooterProcessor {

    static final PropertyPlaceholderHelper PROPERTY_PLACEHOLDER_HELPER = new PropertyPlaceholderHelper("${", "}");

    private final SettingConfigLinkSubmit settingConfigLinkSubmit;

    private final PluginContext pluginContext;

    private final RouteMatcher routeMatcher = createRouteMatcher();

    @Override
    public Mono<Void> process(ITemplateContext context, IProcessableElementTag tag,
        IElementTagStructureHandler structureHandler, IModel model) {
        return settingConfigLinkSubmit.getBasicConfig()
            .doOnNext(basicConfig -> {
                if (!basicConfig.isLoadPlugInResources()) {
                    return;
                }
                if (!isRouteMatched(context, "/links")) {
                    return;
                }
                IModelFactory modelFactory = context.getModelFactory();
                model.add(modelFactory.createText(enhanceScript(basicConfig.isEnableLinkPreview())));
            }).onErrorResume(e -> {
            log.error("LinkApplyEnhanceFooterProcessor process failed", e);
            return Mono.empty();
        }).then();
    }

    private String enhanceScript(boolean linkPreviewEnabled) {
        String html = """
            <!-- LinkSubmitNext site info enhancement start -->
            <script src="/plugins/link-submit-next/assets/static/link-apply-enhance.iife.js?version=${version}"
                data-link-enhance
                data-link-preview-enabled="${linkPreviewEnabled}" defer></script>
            <!-- LinkSubmitNext site info enhancement end -->
            """;

        final Properties properties = new Properties();
        properties.setProperty("version", pluginContext.getVersion());
        properties.setProperty("linkPreviewEnabled", Boolean.toString(linkPreviewEnabled));
        return PROPERTY_PLACEHOLDER_HELPER.replacePlaceholders(html, properties);
    }

    public boolean isRouteMatched(ITemplateContext context, String rule) {
        if (!Contexts.isWebContext(context)) {
            return false;
        }
        IWebRequest request = Contexts.asWebContext(context).getExchange().getRequest();
        String requestPath = request.getRequestPath();
        RouteMatcher.Route requestRoute = routeMatcher.parseRoute(requestPath);

        return isMatchedRoute(requestRoute, rule);
    }

    private boolean isMatchedRoute(RouteMatcher.Route requestRoute, String rule) {
        try {
            return routeMatcher.match(rule, requestRoute);
        } catch (PatternParseException e) {
            // ignore
            log.warn("Parse route pattern [{}] failed", rule, Throwables.getRootCause(e));
        }
        return false;
    }

    RouteMatcher createRouteMatcher() {
        var parser = new PathPatternParser();
        parser.setPathOptions(PathContainer.Options.HTTP_PATH);
        return new PathPatternRouteMatcher(parser);
    }
}
