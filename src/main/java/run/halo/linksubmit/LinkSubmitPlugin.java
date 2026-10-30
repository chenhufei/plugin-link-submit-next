package run.halo.linksubmit;

import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;



@Component
public class LinkSubmitPlugin extends BasePlugin {
    public LinkSubmitPlugin(PluginContext pluginContext) {
        super(pluginContext);
    }

    @Override
    public void start() {
        // PluginLinks owns applications, review history and scheduled jobs.
        // Legacy LinkSubmit/CronLinkSubmit objects are intentionally left untouched in storage,
        // but this enhancement plugin no longer registers or operates a second data model.
    }

    @Override
    public void stop() {
    }
}
