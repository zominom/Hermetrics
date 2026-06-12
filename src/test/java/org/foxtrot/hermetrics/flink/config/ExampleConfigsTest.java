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
        assertThat(config.main().clusters()).hasSize(2);
        ClusterConfig ingest = config.main().clusters().get(0);
        assertThat(ingest.name()).isEqualTo("ingest");
        assertThat(ingest.topics()).containsExactly("pipeline.input");
        assertThat(ingest.properties()).containsKey("security.protocol");
        assertThat(config.main().clusters().get(1).topics())
                .containsExactlyInAnyOrder("orders.enriched", "legacy.export");
        assertThat(config.load().clusters()).hasSize(1);
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
