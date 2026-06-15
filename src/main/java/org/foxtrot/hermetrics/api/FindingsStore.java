package org.foxtrot.hermetrics.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where the API reads findings from. The default backs onto the in-memory Kafka
 * tail; an Elasticsearch (or custom) store can replace it so the UI shows the full
 * history rather than a recent sample. Implementations are selected by the
 * {@code findingsStore.type} api-config value via {@link FindingsStoreRegistry}.
 */
public interface FindingsStore {

    List<JsonNode> verdicts(FindingsQuery query);

    List<JsonNode> rollups(FindingsQuery query);

    List<JsonNode> deadLetters(FindingsQuery query);

    default Map<String, Object> summary(FindingsQuery query) {
        Map<String, Long> counts = new LinkedHashMap<>();
        List<JsonNode> recent = verdicts(query);
        for (JsonNode verdict : recent) {
            counts.merge(verdict.path("status").asText("UNKNOWN"), 1L, Long::sum);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", recent.size());
        response.put("counts", counts);
        return response;
    }
}
