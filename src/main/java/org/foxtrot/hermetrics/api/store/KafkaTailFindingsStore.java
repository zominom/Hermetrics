package org.foxtrot.hermetrics.api.store;

import com.fasterxml.jackson.databind.JsonNode;
import org.foxtrot.hermetrics.api.ApiConfig;
import org.foxtrot.hermetrics.api.FindingsQuery;
import org.foxtrot.hermetrics.api.FindingsStore;
import org.foxtrot.hermetrics.api.TopicTail;

import java.util.ArrayList;
import java.util.List;

/**
 * The default store: a bounded in-memory tail per topic, consumed live from Kafka.
 * Shows a recent sample (capped at tailSize per topic), not full history.
 */
public final class KafkaTailFindingsStore implements FindingsStore {

    private final TopicTail verdicts;
    private final TopicTail rollups;
    private final TopicTail deadLetters;

    public KafkaTailFindingsStore(ApiConfig config) {
        this.verdicts = new TopicTail(config, config.results(), "verdicts");
        this.rollups = new TopicTail(config, config.rollups(), "rollups");
        this.deadLetters = config.deadLetters() == null
                ? null
                : new TopicTail(config, config.deadLetters(), "deadletters");
    }

    @Override
    public List<JsonNode> verdicts(FindingsQuery query) {
        return select(verdicts, query);
    }

    @Override
    public List<JsonNode> rollups(FindingsQuery query) {
        return select(rollups, query);
    }

    @Override
    public List<JsonNode> deadLetters(FindingsQuery query) {
        return deadLetters == null ? List.of() : select(deadLetters, query);
    }

    private static List<JsonNode> select(TopicTail tail, FindingsQuery query) {
        List<JsonNode> all = tail.recent(Integer.MAX_VALUE);
        if (!query.hasTopic()) {
            return all.size() <= query.limit() ? all : all.subList(0, query.limit());
        }
        List<JsonNode> out = new ArrayList<>();
        for (JsonNode node : all) {
            if (query.topic().equals(node.path("topic").asText())) {
                out.add(node);
                if (out.size() >= query.limit()) {
                    break;
                }
            }
        }
        return out;
    }
}
