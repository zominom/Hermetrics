package org.foxtrot.hermetrics.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TopicTail {

    private final int capacity;
    private final Deque<JsonNode> buffer = new ArrayDeque<>();
    private final KafkaConsumer<String, String> consumer;

    public TopicTail(ApiConfig config, String topic, String role) {
        this.capacity = config.tailSize();
        Map<String, Object> props = config.kafkaBase();
        props.put("group.id", "hermetrics-api-" + role + "-" + UUID.randomUUID());
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());
        props.put("auto.offset.reset", config.findingsOffsetReset());
        props.put("enable.auto.commit", "false");
        this.consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(topic));
        Thread thread = new Thread(this::run, "tail-" + topic);
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized List<JsonNode> recent(int limit) {
        List<JsonNode> out = new ArrayList<>(Math.min(limit, buffer.size()));
        for (JsonNode node : buffer) {
            if (out.size() >= limit) {
                break;
            }
            out.add(node);
        }
        return out;
    }

    private void run() {
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

    private synchronized void accept(String value) {
        try {
            buffer.addFirst(HttpApi.JSON.readTree(value));
            while (buffer.size() > capacity) {
                buffer.removeLast();
            }
        } catch (Exception ignored) {
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
