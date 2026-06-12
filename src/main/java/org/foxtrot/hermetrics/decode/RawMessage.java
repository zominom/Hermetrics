package org.foxtrot.hermetrics.decode;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public record RawMessage(String topic, byte[] key, byte[] value, Map<String, byte[]> headers, long timestampMillis) {

    public RawMessage {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public static RawMessage of(String topic, String value) {
        return new RawMessage(topic, null, value.getBytes(StandardCharsets.UTF_8), Map.of(), 0L);
    }

    public static RawMessage of(String topic, String value, long timestampMillis) {
        return new RawMessage(topic, null, value.getBytes(StandardCharsets.UTF_8), Map.of(), timestampMillis);
    }
}
