package org.foxtrot.hermetrics.flink.sink;

import org.apache.flink.api.connector.sink2.Sink;
import org.foxtrot.hermetrics.flink.record.KeyedRecord;
import org.foxtrot.hermetrics.flink.config.SinkConfig;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * {@code "type": "elasticsearch"} sink. Options: {@code url} (required),
 * {@code index} (default hermetrics-findings), and either {@code apiKey} or
 * {@code username}+{@code password}.
 */
public final class ElasticsearchSinkFactory implements FindingSinkFactory {

    @Override
    public String typeId() {
        return "elasticsearch";
    }

    @Override
    public Sink<KeyedRecord> create(SinkConfig config) {
        return new ElasticsearchSink(
                config.require("url"),
                config.options().getOrDefault("index", "hermetrics-findings"),
                authHeader(config));
    }

    private static String authHeader(SinkConfig config) {
        String apiKey = config.options().get("apiKey");
        if (apiKey != null) {
            return "ApiKey " + apiKey;
        }
        String user = config.options().get("username");
        String password = config.options().get("password");
        if (user != null && password != null) {
            return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }
}
