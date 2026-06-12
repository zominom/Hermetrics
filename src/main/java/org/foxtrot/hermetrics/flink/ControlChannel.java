package org.foxtrot.hermetrics.flink;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.foxtrot.hermetrics.config.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ControlChannel {

    public static final MapStateDescriptor<String, String> DESCRIPTOR =
            new MapStateDescriptor<>("hermetrics-control", String.class, String.class);
    public static final String COMPARE_CONFIG_KEY = "compare-config";
    public static final String TYPE_COMPARE_CONFIG = "compare-config";

    private static final Logger LOG = LoggerFactory.getLogger(ControlChannel.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ControlChannel() {
    }

    public static void apply(String message, BroadcastState<String, String> state, ConfigLoader configLoader) {
        try {
            JsonNode envelope = MAPPER.readTree(message);
            String type = envelope.path("type").asText("");
            if (!TYPE_COMPARE_CONFIG.equals(type)) {
                LOG.warn("ignoring control message of unknown type '{}'", type);
                return;
            }
            activate(compareSection(envelope), state, configLoader);
        } catch (Exception e) {
            LOG.error("ignoring invalid control message, keeping the active config", e);
        }
    }

    private static String compareSection(JsonNode envelope) {
        JsonNode compare = envelope.get("compare");
        if (compare == null || compare.isNull()) {
            throw new IllegalArgumentException("compare-config control message has no 'compare' section");
        }
        return compare.toString();
    }

    private static void activate(String compareJson, BroadcastState<String, String> state,
                                 ConfigLoader configLoader) throws Exception {
        configLoader.load(compareJson);
        state.put(COMPARE_CONFIG_KEY, compareJson);
        LOG.info("activated new compare config from control topic ({} chars)", compareJson.length());
    }
}
