package org.foxtrot.hermetrics.pipeline;

import org.foxtrot.hermetrics.canonical.value.CanonicalValue;
import org.foxtrot.hermetrics.canonical.path.Path;
import org.foxtrot.hermetrics.config.CompareConfig;
import org.foxtrot.hermetrics.config.TopicConfig;
import org.foxtrot.hermetrics.decode.DecodeException;
import org.foxtrot.hermetrics.decode.DecoderRegistry;
import org.foxtrot.hermetrics.decode.RawMessage;
import org.foxtrot.hermetrics.match.Env;
import org.foxtrot.hermetrics.match.Observation;
import org.foxtrot.hermetrics.rules.Normalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public final class ObservationFactory {

    private final CompareConfig config;
    private final DecoderRegistry decoders;
    private final Normalizer normalizer;

    public ObservationFactory(CompareConfig config, DecoderRegistry decoders, Normalizer normalizer) {
        this.config = config;
        this.decoders = decoders;
        this.normalizer = normalizer;
    }

    public List<Observation> observe(RawMessage message, Env env) {
        TopicConfig topic = config.topics().get(message.topic());
        if (topic == null) {
            return List.of();
        }
        CanonicalValue decoded = decoders.forFormat(topic.format()).decode(message);
        String guid = extractRequired(message, topic.guidPath(), decoded, "GUID");

        List<Observation> observations = new ArrayList<>(2);
        if (topic.role() != TopicConfig.Role.OUTPUT) {
            observations.add(Observation.entry(env, message.topic(), guid, message.timestampMillis()));
        }
        if (topic.role() != TopicConfig.Role.ENTRY) {
            String sequence = extractSequence(message, topic, decoded);
            CanonicalValue normalized = normalizer.normalize(decoded, config.ruleSetFor(message.topic()));
            observations.add(Observation.output(env, message.topic(), guid, sequence,
                    normalized, message.timestampMillis()));
        }
        return observations;
    }

    private static String extractSequence(RawMessage message, TopicConfig topic, CanonicalValue decoded) {
        if (topic.sequencePaths().isEmpty()) {
            return null;
        }
        StringJoiner sequence = new StringJoiner("|");
        for (Path path : topic.sequencePaths()) {
            sequence.add(extractRequired(message, path, decoded, "sequence"));
        }
        return sequence.toString();
    }

    private static String extractRequired(RawMessage message, Path path, CanonicalValue decoded, String what) {
        return FieldExtractor.extract(decoded, path)
                .orElseThrow(() -> new DecodeException(
                        "no " + what + " at '" + path + "' in message on topic " + message.topic()));
    }
}
