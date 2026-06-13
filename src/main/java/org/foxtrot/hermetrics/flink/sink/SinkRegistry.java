package org.foxtrot.hermetrics.flink.sink;

import org.apache.flink.api.connector.sink2.Sink;
import org.foxtrot.hermetrics.flink.record.KeyedRecord;
import org.foxtrot.hermetrics.flink.config.SinkConfig;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class SinkRegistry {

    private final Map<String, FindingSinkFactory> byType = new HashMap<>();

    public static SinkRegistry withDefaults() {
        return new SinkRegistry()
                .register(new KafkaSinkFactory())
                .register(new LoggingSinkFactory());
    }

    public SinkRegistry register(FindingSinkFactory factory) {
        byType.put(factory.typeId().toLowerCase(Locale.ROOT), factory);
        return this;
    }

    public Sink<KeyedRecord> create(SinkConfig config) {
        FindingSinkFactory factory = byType.get(config.type().toLowerCase(Locale.ROOT));
        if (factory == null) {
            throw new IllegalArgumentException(
                    "no sink registered for type '" + config.type() + "', registered: " + byType.keySet());
        }
        return factory.create(config);
    }
}
