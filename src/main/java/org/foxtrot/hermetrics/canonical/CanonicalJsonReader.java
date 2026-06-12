package org.foxtrot.hermetrics.canonical;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public final class CanonicalJsonReader {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    private CanonicalJsonReader() {
    }

    public static CanonicalValue parse(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root == null || root.isMissingNode()) {
                throw new IllegalArgumentException("empty JSON");
            }
            return fromJackson(root);
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid JSON", e);
        }
    }

    public static CanonicalValue fromJackson(JsonNode node) {
        if (node.isObject()) {
            TreeMap<String, CanonicalValue> fields = new TreeMap<>();
            for (var entry : node.properties()) {
                fields.put(entry.getKey(), fromJackson(entry.getValue()));
            }
            return new CanonicalObject(fields);
        }
        if (node.isArray()) {
            List<CanonicalValue> items = new ArrayList<>(node.size());
            for (JsonNode item : node) {
                items.add(fromJackson(item));
            }
            return new CanonicalArray(items);
        }
        if (node.isTextual()) {
            return new CanonicalString(node.textValue());
        }
        if (node.isNumber()) {
            return new CanonicalNumber(node.decimalValue());
        }
        if (node.isBoolean()) {
            return new CanonicalBool(node.booleanValue());
        }
        if (node.isNull()) {
            return CanonicalNull.INSTANCE;
        }
        throw new IllegalArgumentException("unsupported JSON node type: " + node.getNodeType());
    }
}
