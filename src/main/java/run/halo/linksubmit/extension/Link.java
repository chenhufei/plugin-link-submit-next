package run.halo.linksubmit.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;


@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "core.halo.run", version = "v1alpha1", kind = "Link", plural = "links", singular = "link")
public class Link extends AbstractExtension {

    private LinkSpec spec;

    @Data
    public static class LinkSpec {
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        private String url;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        private String displayName;

        private String logo;

        private String description;

        private Integer priority;

        private String groupName;
    }
}