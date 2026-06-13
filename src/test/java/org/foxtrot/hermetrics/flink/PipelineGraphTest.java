package org.foxtrot.hermetrics.flink;

import org.foxtrot.hermetrics.flink.record.RawEnvRecord;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.foxtrot.hermetrics.config.ConfigLoader;
import org.foxtrot.hermetrics.flink.config.JobConfig;
import org.foxtrot.hermetrics.flink.config.JobConfigLoader;
import org.foxtrot.hermetrics.match.Env;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineGraphTest {

    private static final String CONFIG = """
            {
              "main": {"bootstrapServers": "main:9092"},
              "load": {"bootstrapServers": "load:9092"},
              "control": {"bootstrapServers": "load:9092", "topic": "hermetrics.control"},
              "results": {"type": "logging"},
              "rollups": {"windowMillis": 1000, "sink": {"type": "logging"}},
              "compare": {
                "policy": {"quietMillis": 100, "maxWaitMillis": 1000, "cohortMode": "ASSUME_ALL"},
                "ruleSets": {"default": {"rules": []}},
                "topics": [
                  {"name": "pipeline.in", "role": "BOTH", "format": "json", "guidPath": "guid"}
                ]
              }
            }
            """;

    @Test
    void fullTopologyBuildsAndExtractsTypes() {
        JobConfig config = new JobConfigLoader(new ConfigLoader()).load(CONFIG);
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStream<RawEnvRecord> records = env.fromData(
                new RawEnvRecord(Env.MAIN, "pipeline.in", null,
                        "{\"guid\": \"g1\"}".getBytes(StandardCharsets.UTF_8), 1L),
                new RawEnvRecord(Env.LOAD, "pipeline.in", null,
                        "{\"guid\": \"g1\"}".getBytes(StandardCharsets.UTF_8), 2L));
        DataStream<String> control = env.fromData(
                "{\"type\": \"compare-config\", \"compare\": {\"topics\": ["
                        + "{\"name\": \"pipeline.in\", \"role\": \"BOTH\", \"format\": \"json\", \"guidPath\": \"guid\"}]}}");

        Topology.build(config, records, control, Plugins.defaults());

        String plan = env.getExecutionPlan();
        assertThat(plan).contains("normalize");
        assertThat(plan).contains("decide");
        assertThat(plan).contains("rollups");
    }
}
