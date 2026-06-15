package org.foxtrot.hermetrics.api;

import org.foxtrot.hermetrics.api.store.ElasticsearchFindingsStore;
import org.foxtrot.hermetrics.api.store.KafkaTailFindingsStore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Maps the {@code findingsStore.type} api-config value to a {@link FindingsStore}.
 * Mirrors the decoder/sink/rule-type registries: register a custom store here to
 * read findings from your own database without touching the endpoints.
 */
public final class FindingsStoreRegistry {

    @FunctionalInterface
    public interface Factory {
        FindingsStore create(ApiConfig config);
    }

    private final Map<String, Factory> byType = new HashMap<>();

    public static FindingsStoreRegistry withDefaults() {
        return new FindingsStoreRegistry()
                .register("kafka-tail", KafkaTailFindingsStore::new)
                .register("elasticsearch", config -> new ElasticsearchFindingsStore(config.findingsStoreOptions()));
    }

    public FindingsStoreRegistry register(String type, Factory factory) {
        byType.put(type.toLowerCase(Locale.ROOT), factory);
        return this;
    }

    public FindingsStore create(ApiConfig config) {
        Factory factory = byType.get(config.findingsStoreType().toLowerCase(Locale.ROOT));
        if (factory == null) {
            throw new IllegalArgumentException(
                    "no findings store '" + config.findingsStoreType() + "', registered: " + byType.keySet());
        }
        return factory.create(config);
    }
}
