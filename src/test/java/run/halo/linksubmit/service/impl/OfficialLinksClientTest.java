package run.halo.linksubmit.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Unstructured;

class OfficialLinksClientTest {

    @Test
    void listsLinksThroughOfficialGvkIndex() {
        ReactiveExtensionClient extensionClient = mock(ReactiveExtensionClient.class);
        var queryEngine = mock(run.halo.app.extension.index.IndexedQueryEngine.class);
        var raw = rawLink();
        when(extensionClient.indexedQueryEngine()).thenReturn(queryEngine);
        when(queryEngine.retrieveAll(any(), any(), any())).thenReturn(List.of("link-a"));
        when(extensionClient.fetch(OfficialLinksClient.LINK_GVK, "link-a"))
            .thenReturn(Mono.just(raw));

        var links = new OfficialLinksClient(extensionClient)
            .listLinks(new ListOptions(), Sort.unsorted())
            .collectList()
            .block();

        assertNotNull(links);
        assertEquals(1, links.size());
        assertEquals("https://example.com", links.getFirst().getSpec().getUrl());
        assertEquals("link-a", links.getFirst().getMetadata().getName());
    }

    @Test
    void keepsPluginLinksFieldsWhenUpdatingKnownFields() {
        var raw = rawLink();
        var link = OfficialLinksClient.fromRawLink(raw);
        link.getSpec().setGroupName("new-group");
        link.getMetadata().setAnnotations(new LinkedHashMap<>(Map.of("email", "new@example.com")));

        OfficialLinksClient.applyLinkUpdate(raw, link);

        Map<String, Object> spec = Unstructured.getNestedMap(raw.getData(), "spec").orElseThrow();
        assertEquals("new-group", spec.get("groupName"));
        assertEquals(Map.of("enabled", true, "feedUrls", List.of("https://example.com/feed")),
            spec.get("rss"));
        assertFalse(Unstructured.getNestedMap(raw.getData(), "status").orElseThrow().isEmpty());
        assertEquals("new@example.com", raw.getMetadata().getAnnotations().get("email"));
        assertEquals("keep-me", raw.getMetadata().getLabels().get("official-field"));
    }

    private static Unstructured rawLink() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", "link-a");
        metadata.put("labels", Map.of("official-field", "keep-me"));
        metadata.put("annotations", Map.of("email", "old@example.com"));

        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("url", "https://example.com");
        spec.put("displayName", "Example");
        spec.put("groupName", "old-group");
        spec.put("rss", Map.of(
            "enabled", true,
            "feedUrls", List.of("https://example.com/feed")
        ));
        spec.put("verification", Map.of("backlinkScanUrl", "https://example.com/links"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("apiVersion", "core.halo.run/v1alpha1");
        data.put("kind", "Link");
        data.put("metadata", metadata);
        data.put("spec", spec);
        data.put("status", Map.of("verification", Map.of("state", "ACCESSIBLE")));
        return new Unstructured(data);
    }
}
