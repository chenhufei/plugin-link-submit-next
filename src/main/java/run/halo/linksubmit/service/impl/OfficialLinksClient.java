package run.halo.linksubmit.service.impl;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.GroupVersionKind;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.MetadataOperator;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Unstructured;
import run.halo.linksubmit.extension.Link;
import run.halo.linksubmit.extension.LinkGroup;

/**
 * Accesses PluginLinks resources by GVK so this plugin does not need to register
 * duplicate Link and LinkGroup schemes.
 */
@Component
class OfficialLinksClient {

    static final GroupVersionKind LINK_GVK =
        GroupVersionKind.fromAPIVersionAndKind("core.halo.run/v1alpha1", "Link");
    static final GroupVersionKind LINK_GROUP_GVK =
        GroupVersionKind.fromAPIVersionAndKind("core.halo.run/v1alpha1", "LinkGroup");

    private final ReactiveExtensionClient client;

    OfficialLinksClient(ReactiveExtensionClient client) {
        this.client = client;
    }

    Flux<Link> listLinks(ListOptions options, Sort sort) {
        return listAll(LINK_GVK, options, sort, OfficialLinksClient::fromRawLink);
    }

    Flux<LinkGroup> listGroups(ListOptions options, Sort sort) {
        return listAll(LINK_GROUP_GVK, options, sort, OfficialLinksClient::fromRawGroup);
    }

    Mono<Link> fetchLink(String name) {
        return client.fetch(LINK_GVK, name).map(OfficialLinksClient::fromRawLink);
    }

    Mono<Link> createLink(Link link) {
        return client.create(toRaw(link, LINK_GVK)).map(OfficialLinksClient::fromRawLink);
    }

    Mono<Link> updateLink(Link link) {
        String name = Objects.requireNonNull(link.getMetadata().getName(),
            "Link metadata.name must not be null when updating");
        return client.fetch(LINK_GVK, name)
            .map(raw -> applyLinkUpdate(raw, link))
            .flatMap(client::update)
            .map(OfficialLinksClient::fromRawLink);
    }

    Mono<Link> deleteLink(Link link) {
        String name = Objects.requireNonNull(link.getMetadata().getName(),
            "Link metadata.name must not be null when deleting");
        return client.fetch(LINK_GVK, name)
            .flatMap(client::delete)
            .map(OfficialLinksClient::fromRawLink);
    }

    private <T> Flux<T> listAll(GroupVersionKind gvk, ListOptions options, Sort sort,
        Function<Unstructured, T> mapper) {
        return Flux.defer(() -> Flux.fromIterable(
                client.indexedQueryEngine().retrieveAll(gvk, options, sort)))
            .concatMap(name -> client.fetch(gvk, name))
            .map(mapper);
    }

    static Unstructured applyLinkUpdate(Unstructured raw, Link link) {
        Map<String, Object> spec = Unstructured.getNestedMap(raw.getData(), "spec")
            .map(LinkedHashMap::new)
            .orElseGet(LinkedHashMap::new);
        Link.LinkSpec source = Objects.requireNonNull(link.getSpec(), "Link spec must not be null");
        spec.put("url", source.getUrl());
        spec.put("displayName", source.getDisplayName());
        spec.put("logo", source.getLogo());
        spec.put("description", source.getDescription());
        spec.put("priority", source.getPriority());
        spec.put("groupName", source.getGroupName());
        Unstructured.setNestedValue(raw.getData(), spec, "spec");

        if (link.getMetadata() != null) {
            Map<String, String> annotations = link.getMetadata().getAnnotations();
            raw.getMetadata().setAnnotations(annotations == null
                ? null
                : new LinkedHashMap<>(annotations));
        }
        return raw;
    }

    private static Unstructured toRaw(Link link, GroupVersionKind gvk) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("apiVersion", gvk.groupVersion().toString());
        data.put("kind", gvk.kind());
        data.put("metadata", metadataToMap(link.getMetadata()));

        Link.LinkSpec source = Objects.requireNonNull(link.getSpec(), "Link spec must not be null");
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("url", source.getUrl());
        spec.put("displayName", source.getDisplayName());
        spec.put("logo", source.getLogo());
        spec.put("description", source.getDescription());
        spec.put("priority", source.getPriority());
        spec.put("groupName", source.getGroupName());
        data.put("spec", spec);
        return new Unstructured(data);
    }

    static Link fromRawLink(Unstructured raw) {
        Link link = new Link();
        link.setMetadata(copyMetadata(raw.getMetadata()));
        Map<String, Object> specData = Unstructured.getNestedMap(raw.getData(), "spec")
            .orElseGet(Map::of);
        Link.LinkSpec spec = new Link.LinkSpec();
        spec.setUrl(stringValue(specData.get("url")));
        spec.setDisplayName(stringValue(specData.get("displayName")));
        spec.setLogo(stringValue(specData.get("logo")));
        spec.setDescription(stringValue(specData.get("description")));
        spec.setPriority(integerValue(specData.get("priority")));
        spec.setGroupName(stringValue(specData.get("groupName")));
        link.setSpec(spec);
        return link;
    }

    static LinkGroup fromRawGroup(Unstructured raw) {
        LinkGroup group = new LinkGroup();
        group.setMetadata(copyMetadata(raw.getMetadata()));
        Map<String, Object> specData = Unstructured.getNestedMap(raw.getData(), "spec")
            .orElseGet(Map::of);
        LinkGroup.LinkGroupSpec spec = new LinkGroup.LinkGroupSpec();
        spec.setDisplayName(stringValue(specData.get("displayName")));
        spec.setPriority(integerValue(specData.get("priority")));
        group.setSpec(spec);
        return group;
    }

    private static Map<String, Object> metadataToMap(MetadataOperator metadata) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (metadata == null) {
            return result;
        }
        putIfNotNull(result, "name", metadata.getName());
        putIfNotNull(result, "generateName", metadata.getGenerateName());
        putIfNotNull(result, "labels", metadata.getLabels());
        putIfNotNull(result, "annotations", metadata.getAnnotations());
        return result;
    }

    private static Metadata copyMetadata(MetadataOperator source) {
        Metadata target = new Metadata();
        if (source == null) {
            return target;
        }
        target.setName(source.getName());
        target.setGenerateName(source.getGenerateName());
        target.setLabels(source.getLabels() == null ? null : new LinkedHashMap<>(source.getLabels()));
        target.setAnnotations(source.getAnnotations() == null
            ? null
            : new LinkedHashMap<>(source.getAnnotations()));
        target.setVersion(source.getVersion());
        target.setCreationTimestamp(source.getCreationTimestamp());
        target.setDeletionTimestamp(source.getDeletionTimestamp());
        target.setFinalizers(source.getFinalizers() == null
            ? null
            : new LinkedHashSet<>(source.getFinalizers()));
        return target;
    }

    private static void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer integerValue(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }
}
