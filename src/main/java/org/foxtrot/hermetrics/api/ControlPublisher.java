package org.foxtrot.hermetrics.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.foxtrot.hermetrics.config.ConfigLoader;

import java.util.Map;
import java.util.concurrent.ExecutionException;

public final class ControlPublisher {

    private static final String CONTROL_KEY = "compare-config";

    private final String controlTopic;
    private final ConfigLoader configLoader;
    private final Producer<String, String> producer;

    public ControlPublisher(ApiConfig config, ConfigLoader configLoader) {
        this.controlTopic = config.controlTopic();
        this.configLoader = configLoader;
        Map<String, Object> props = config.kafkaBase();
        props.put("key.serializer", StringSerializer.class.getName());
        props.put("value.serializer", StringSerializer.class.getName());
        props.put("acks", "all");
        this.producer = new KafkaProducer<>(props);
    }

    public void apply(JsonNode compareConfig) {
        String compareJson = compareConfig.toString();
        configLoader.load(compareJson);
        ObjectNode envelope = HttpApi.JSON.createObjectNode();
        envelope.put("type", "compare-config");
        envelope.set("compare", compareConfig);
        send(envelope.toString());
    }

    private void send(String message) {
        try {
            producer.send(new ProducerRecord<>(controlTopic, CONTROL_KEY, message)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted publishing control message", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("failed to publish control message: " + e.getCause().getMessage(), e);
        }
    }
}
