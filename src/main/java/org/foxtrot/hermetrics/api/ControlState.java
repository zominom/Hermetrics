package org.foxtrot.hermetrics.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ControlState {

    private final ApiConfig config;
    private volatile String activeCompareJson;
    private volatile String source = "none";

    public ControlState(ApiConfig config) {
        this.config = config;
        this.activeCompareJson = bootstrapConfig();
        KafkaConsumer<String, String> consumer = consumer();
        consumer.subscribe(List.of(config.control().topic()));
        Thread thread = new Thread(() -> run(consumer), "control-state");
        thread.setDaemon(true);
        thread.start();
    }

    public String activeCompareJson() {
        return activeCompareJson;
    }

    public String source() {
        return source;
    }

    private KafkaConsumer<String, String> consumer() {
        Map<String, Object> props = config.control().kafkaBase();
        props.put("group.id", "hermetrics-api-control-" + UUID.randomUUID());
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());
        props.put("auto.offset.reset", "earliest");
        props.put("enable.auto.commit", "false");
        return new KafkaConsumer<>(props);
    }

    private void run(KafkaConsumer<String, String> consumer) {
        while (true) {
            try {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    accept(record.value());
                }
            } catch (Exception e) {
                sleep();
            }
        }
    }

    private void accept(String message) {
        try {
            JsonNode envelope = HttpApi.JSON.readTree(message);
            if (!"compare-config".equals(envelope.path("type").asText())) {
                return;
            }
            JsonNode compare = envelope.get("compare");
            if (compare != null && !compare.isNull()) {
                activeCompareJson = compare.toString();
                source = "control";
            }
        } catch (Exception ignored) {
        }
    }

    private String bootstrapConfig() {
        if (config.bootstrapComparePath() == null) {
            return null;
        }
        try {
            source = "bootstrap";
            return Files.readString(Path.of(config.bootstrapComparePath()));
        } catch (Exception e) {
            return null;
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
