package org.foxtrot.hermetrics.pipeline;

import org.foxtrot.hermetrics.config.CompareConfig;
import org.foxtrot.hermetrics.config.ConfigLoader;
import org.foxtrot.hermetrics.decode.DecodeException;
import org.foxtrot.hermetrics.decode.DecoderRegistry;
import org.foxtrot.hermetrics.decode.RawMessage;
import org.foxtrot.hermetrics.diff.StructuralDiffer;
import org.foxtrot.hermetrics.match.Env;
import org.foxtrot.hermetrics.match.GuidState;
import org.foxtrot.hermetrics.match.MatchEngine;
import org.foxtrot.hermetrics.match.MatchPolicy;
import org.foxtrot.hermetrics.match.Observation;
import org.foxtrot.hermetrics.match.Verdict;
import org.foxtrot.hermetrics.match.VerdictStatus;
import org.foxtrot.hermetrics.rules.Normalizer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObservationFactoryTest {

    private static final String CONFIG = """
            {
              "policy": {"quietMillis": 1000, "maxWaitMillis": 5000, "cohortMode": "ENTRY_TOPICS"},
              "ruleSets": {
                "default": {"rules": [{"type": "ignore", "path": "**.traceId"}]}
              },
              "topics": [
                {"name": "pipeline.in", "role": "BOTH", "format": "json", "guidPath": "guid"},
                {"name": "orders.enriched", "role": "OUTPUT", "format": "json", "guidPath": "doc.guid"},
                {"name": "orders.versioned", "role": "OUTPUT", "format": "json",
                 "guidPath": "doc.guid", "sequencePath": "doc.version"}
              ]
            }
            """;

    private final CompareConfig config = new ConfigLoader().load(CONFIG);
    private final ObservationFactory factory =
            new ObservationFactory(config, DecoderRegistry.withDefaults(), new Normalizer());

    @Test
    void loadsPolicyTopicsAndRuleSets() {
        assertThat(config.policy().quietMillis()).isEqualTo(1000);
        assertThat(config.policy().cohortMode()).isEqualTo(MatchPolicy.CohortMode.ENTRY_TOPICS);
        assertThat(config.topics()).containsKeys("pipeline.in", "orders.enriched");
        assertThat(config.ruleSetFor("orders.enriched").rewriteRules()).hasSize(1);
        assertThat(config.ruleSetFor("unknown-topic")).isSameAs(config.ruleSetFor("orders.enriched"));
    }

    @Test
    void rejectsBrokenConfigs() {
        assertThatThrownBy(() -> new ConfigLoader().load("""
                {"topics": [
                  {"name": "a", "format": "json", "guidPath": "guid"},
                  {"name": "a", "format": "json", "guidPath": "guid"}
                ]}
                """)).hasMessageContaining("duplicate topic");
        assertThatThrownBy(() -> new ConfigLoader().load("""
                {"topics": [{"name": "a", "format": "json", "guidPath": "guid", "ruleSet": "nope"}]}
                """)).hasMessageContaining("unknown rule set");
        assertThatThrownBy(() -> new ConfigLoader().load("{}")).hasMessageContaining("no topics");
    }

    @Test
    void unconfiguredTopicsYieldNothing() {
        assertThat(factory.observe(RawMessage.of("other.topic", "{\"guid\": \"g\"}"), Env.MAIN)).isEmpty();
    }

    @Test
    void bothRoleEmitsEntryAndOutput() {
        List<Observation> observations =
                factory.observe(RawMessage.of("pipeline.in", "{\"guid\": \"g1\", \"v\": 1}", 42L), Env.MAIN);
        assertThat(observations).hasSize(2);
        assertThat(observations.get(0).kind()).isEqualTo(Observation.Kind.ENTRY);
        assertThat(observations.get(1).kind()).isEqualTo(Observation.Kind.OUTPUT);
        assertThat(observations).allSatisfy(o -> {
            assertThat(o.guid()).isEqualTo("g1");
            assertThat(o.eventTimeMillis()).isEqualTo(42L);
        });
    }

    @Test
    void outputStateIsNormalized() {
        List<Observation> observations = factory.observe(
                RawMessage.of("orders.enriched", "{\"doc\": {\"guid\": \"g1\"}, \"traceId\": \"t-123\"}"), Env.LOAD);
        Observation output = observations.get(0);
        List<Observation> other = factory.observe(
                RawMessage.of("orders.enriched", "{\"doc\": {\"guid\": \"g1\"}, \"traceId\": \"t-999\"}"), Env.MAIN);
        assertThat(output.stateHash()).isEqualTo(other.get(0).stateHash());
    }

    @Test
    void missingGuidIsADecodeError() {
        assertThatThrownBy(() -> factory.observe(
                RawMessage.of("orders.enriched", "{\"doc\": {}}"), Env.MAIN))
                .isInstanceOf(DecodeException.class)
                .hasMessageContaining("doc.guid");
    }

    @Test
    void sequenceIsExtractedForConfiguredTopics() {
        Observation plain = factory.observe(
                RawMessage.of("orders.enriched", "{\"doc\": {\"guid\": \"g1\"}}"), Env.MAIN).get(0);
        assertThat(plain.sequence()).isNull();

        Observation versioned = factory.observe(
                RawMessage.of("orders.versioned", "{\"doc\": {\"guid\": \"g1\", \"version\": 3}}"), Env.MAIN).get(0);
        assertThat(versioned.sequence()).isEqualTo("3");
    }

    @Test
    void missingSequenceIsADecodeError() {
        assertThatThrownBy(() -> factory.observe(
                RawMessage.of("orders.versioned", "{\"doc\": {\"guid\": \"g1\"}}"), Env.MAIN))
                .isInstanceOf(DecodeException.class)
                .hasMessageContaining("sequence");
    }

    @Test
    void compositeSequencePathsJoinTheirValues() {
        CompareConfig composite = new ConfigLoader().load("""
                {"topics": [{"name": "t", "format": "json", "guidPath": "guid",
                             "sequencePath": ["eventType", "version"]}]}
                """);
        ObservationFactory compositeFactory =
                new ObservationFactory(composite, DecoderRegistry.withDefaults(), new Normalizer());
        Observation observation = compositeFactory.observe(
                RawMessage.of("t", "{\"guid\": \"g1\", \"eventType\": \"created\", \"version\": 2}"), Env.MAIN).get(0);
        assertThat(observation.sequence()).isEqualTo("created|2");
    }

    @Test
    void endToEndParityScenario() {
        MatchEngine engine = new MatchEngine(config.policy(), config::ruleSetFor, new StructuralDiffer());
        Map<String, GuidState> states = new HashMap<>();

        feed(engine, states, "pipeline.in", "{\"guid\": \"g1\", \"order\": 7}", Env.MAIN);
        feed(engine, states, "pipeline.in", "{\"guid\": \"g1\", \"order\": 7}", Env.LOAD);
        feed(engine, states, "orders.enriched",
                "{\"doc\": {\"guid\": \"g1\"}, \"total\": 10.0, \"traceId\": \"m-1\"}", Env.MAIN);
        feed(engine, states, "orders.enriched",
                "{\"doc\": {\"guid\": \"g1\"}, \"total\": 10, \"traceId\": \"l-1\"}", Env.LOAD);

        feed(engine, states, "pipeline.in", "{\"guid\": \"test-9\", \"order\": 1}", Env.LOAD);
        feed(engine, states, "orders.enriched",
                "{\"doc\": {\"guid\": \"test-9\"}, \"total\": 1, \"traceId\": \"l-2\"}", Env.LOAD);

        List<Verdict> mirrored = engine.decide("g1", states.get("g1"), 99_000L);
        assertThat(mirrored).hasSize(2);
        assertThat(mirrored).extracting(Verdict::topic)
                .containsExactlyInAnyOrder("pipeline.in", "orders.enriched");
        assertThat(mirrored).allSatisfy(v -> assertThat(v.status()).isEqualTo(VerdictStatus.EQUAL));

        List<Verdict> injected = engine.decide("test-9", states.get("test-9"), 99_000L);
        assertThat(injected).singleElement()
                .extracting(Verdict::status).isEqualTo(VerdictStatus.TEST_TRAFFIC);
    }

    private void feed(MatchEngine engine, Map<String, GuidState> states,
                      String topic, String payload, Env env) {
        for (Observation observation : factory.observe(RawMessage.of(topic, payload), env)) {
            engine.onObservation(states.computeIfAbsent(observation.guid(), g -> new GuidState()), observation);
        }
    }
}
