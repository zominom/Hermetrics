package org.foxtrot.hermetrics.flink.config;

import org.foxtrot.hermetrics.config.ConfigLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ExampleConfigsTest {

    private static JobConfig load(String file) throws Exception {
        return new JobConfigLoader(new ConfigLoader()).load(Files.readString(Path.of(file)));
    }

    @Test
    void exampleConfigLoads() throws Exception {
        JobConfig config = load("job-config.example.json");
        assertThat(config.results().type()).isEqualTo("kafka");
        assertThat(config.compare().topics())
                .containsKeys("pipeline.input", "orders.enriched", "legacy.export");
        assertThat(config.compare().ruleSetFor("orders.enriched").rules()).hasSize(3);
        assertThat(config.compare().topics().get("orders.enriched").sequencePaths()).hasSize(1);
        assertThat(config.compare().topics().get("pipeline.input").sequencePaths()).isEmpty();
        assertThat(config.main().clusters()).hasSize(2);
        ClusterConfig ingest = config.main().clusters().get(0);
        assertThat(ingest.name()).isEqualTo("ingest");
        assertThat(ingest.topics()).containsExactly("pipeline.input");
        assertThat(ingest.properties()).containsKey("security.protocol");
        assertThat(config.main().clusters().get(1).topics())
                .containsExactlyInAnyOrder("orders.enriched", "legacy.export");
        assertThat(config.load().clusters()).hasSize(2);
        ClusterConfig loadIngest = config.load().clusters().get(0);
        ClusterConfig loadCore = config.load().clusters().get(1);
        assertThat(loadIngest.bootstrapServers()).isEqualTo("load-ingest-kafka:9092");
        assertThat(loadIngest.physicalTopic("pipeline.input")).isEqualTo("load.pipeline.input");
        assertThat(loadCore.physicalTopic("orders.enriched")).isEqualTo("load.orders.enriched");
        assertThat(loadCore.physicalTopic("legacy.export")).isEqualTo("legacy-export-load");
    }

    @Test
    void localConfigLoads() throws Exception {
        JobConfig config = load("job-config.local.json");
        assertThat(config.results().type()).isEqualTo("logging");
        assertThat(config.main().clusters().get(0).physicalTopic("orders")).isEqualTo("main.orders");
        assertThat(config.load().clusters().get(0).physicalTopic("orders")).isEqualTo("load.orders");
        assertThat(config.compare().policy().quietMillis()).isEqualTo(5000);
    }
}
