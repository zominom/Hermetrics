package org.foxtrot.hermetrics.flink.config;

import org.foxtrot.hermetrics.config.ConfigLoader;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobConfigLoaderTest {

    private static final String CONFIG = """
            {
              "main": {"bootstrapServers": "main:9092"},
              "load": {"bootstrapServers": "load:9092", "topicPrefix": "load."},
              "control": {"bootstrapServers": "load:9092", "topic": "hermetrics.control"},
              "results": {"type": "logging"},
              "rollups": {"windowMillis": 1000, "sink": {"type": "logging"}},
              "compare": {
                "policy": {"quietMillis": 100, "maxWaitMillis": 1000, "cohortMode": "ASSUME_ALL"},
                "ruleSets": {"default": {"rules": []}},
                "topics": [
                  {"name": "pipeline.in", "role": "BOTH", "format": "json", "guidPath": "guid"},
                  {"name": "orders.out", "role": "OUTPUT", "format": "json", "guidPath": "guid"}
                ]
              }
            }
            """;

    private static final String MULTI_CLUSTER_MAIN = """
            {"clusters": [
              {"name": "ingest", "bootstrapServers": "ingest:9092", "topics": ["pipeline.in"]},
              {"name": "core", "bootstrapServers": "core:9092", "topics": ["orders.out"]}
            ]}""";

    private static JobConfig load(String json) {
        return new JobConfigLoader(new ConfigLoader()).load(json);
    }

    private static String withMain(String mainSection) {
        return CONFIG.replace("{\"bootstrapServers\": \"main:9092\"}", mainSection);
    }

    @Test
    void flatShorthandIsOneClusterCarryingAllTopics() {
        JobConfig config = load(CONFIG);
        assertThat(config.main().clusters()).hasSize(1);
        ClusterConfig main = config.main().clusters().get(0);
        assertThat(main.bootstrapServers()).isEqualTo("main:9092");
        assertThat(main.groupId()).isEqualTo("hermetrics-main");
        assertThat(main.startingOffsets()).isEqualTo("latest");
        assertThat(main.topics()).containsExactlyInAnyOrder("pipeline.in", "orders.out");
        assertThat(config.deadLetters().type()).isEqualTo("logging");
        assertThat(config.stateTtlMillis()).isEqualTo(7_200_000L);
        assertThat(config.emitEqualVerdicts()).isTrue();
        assertThat(config.rollups().windowMillis()).isEqualTo(1000);
        assertThat(config.compareJson()).contains("ASSUME_ALL");
    }

    @Test
    void clustersSplitTheTopicsOfOneEnvironment() {
        JobConfig config = load(withMain(MULTI_CLUSTER_MAIN));
        assertThat(config.main().clusters()).hasSize(2);
        ClusterConfig ingest = config.main().clusters().get(0);
        ClusterConfig core = config.main().clusters().get(1);
        assertThat(ingest.name()).isEqualTo("ingest");
        assertThat(ingest.topics()).containsExactly("pipeline.in");
        assertThat(core.bootstrapServers()).isEqualTo("core:9092");
        assertThat(core.topics()).containsExactly("orders.out");
    }

    @Test
    void prefixesAndOverridesMapPerCluster() {
        JobConfig config = load(CONFIG);
        ClusterConfig loadCluster = config.load().clusters().get(0);
        assertThat(loadCluster.physicalTopic("pipeline.in")).isEqualTo("load.pipeline.in");
        assertThat(loadCluster.physicalToLogical()).containsEntry("load.pipeline.in", "pipeline.in");

        JobConfig overridden = load(CONFIG.replace(
                "\"topicPrefix\": \"load.\"",
                "\"topicPrefix\": \"load.\", \"topicOverrides\": {\"pipeline.in\": \"custom.input\"}"));
        assertThat(overridden.load().clusters().get(0).physicalTopic("pipeline.in")).isEqualTo("custom.input");
    }

    @Test
    void everyTopicMustBeAssignedToExactlyOneCluster() {
        assertThatThrownBy(() -> load(withMain("""
                {"clusters": [{"name": "ingest", "bootstrapServers": "i:9092", "topics": ["pipeline.in"]}]}""")))
                .hasMessageContaining("assigns no cluster for topics")
                .hasMessageContaining("orders.out");

        assertThatThrownBy(() -> load(withMain("""
                {"clusters": [
                  {"name": "a", "bootstrapServers": "a:9092", "topics": ["pipeline.in", "orders.out"]},
                  {"name": "b", "bootstrapServers": "b:9092", "topics": ["orders.out"]}
                ]}""")))
                .hasMessageContaining("assigned to clusters 'a' and 'b'");

        assertThatThrownBy(() -> load(withMain("""
                {"clusters": [
                  {"name": "a", "bootstrapServers": "a:9092", "topics": ["pipeline.in", "orders.out", "ghost"]}
                ]}""")))
                .hasMessageContaining("unknown topic 'ghost'");

        assertThatThrownBy(() -> load(withMain("""
                {"clusters": [{"name": "a", "bootstrapServers": "a:9092"}]}""")))
                .hasMessageContaining("must list the topics it carries");
    }

    @Test
    void collidingPhysicalTopicsAreRejected() {
        String config = """
                {
                  "main": {"bootstrapServers": "m:9092",
                           "topicOverrides": {"a": "same", "b": "same"}},
                  "load": {"bootstrapServers": "l:9092"},
                  "control": {"bootstrapServers": "l:9092", "topic": "c"},
                  "results": {"type": "logging"},
                  "compare": {"topics": [
                    {"name": "a", "format": "json", "guidPath": "guid"},
                    {"name": "b", "format": "json", "guidPath": "guid"}
                  ]}
                }
                """;
        assertThatThrownBy(() -> load(config))
                .hasMessageContaining("same physical topic");
    }

    @Test
    void missingSectionsFailWithTheFieldName() {
        assertThatThrownBy(() -> load("{}")).hasMessageContaining("compare");
        assertThatThrownBy(() -> load(CONFIG.replace("\"results\"", "\"resultsX\"")))
                .hasMessageContaining("results");
        assertThatThrownBy(() -> load(CONFIG.replace("\"control\"", "\"controlX\"")))
                .hasMessageContaining("control");
    }

    @Test
    void unknownOffsetModeIsRejected() {
        assertThatThrownBy(() -> load(withMain(
                "{\"bootstrapServers\": \"main:9092\", \"startingOffsets\": \"yesterday\"}")))
                .hasMessageContaining("startingOffsets");
    }
}
