package run.halo.linksubmit;

import run.halo.linksubmit.extension.LinkSubmit;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class LinkSubmitEvent extends ApplicationEvent {

    private final LinkSubmit linkSubmit;

    public LinkSubmitEvent(Object source, LinkSubmit linkSubmit) {
        super(source);
        this.linkSubmit = linkSubmit;
    }
}
