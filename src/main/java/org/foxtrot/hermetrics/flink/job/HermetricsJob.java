package org.foxtrot.hermetrics.flink.job;

import org.foxtrot.hermetrics.flink.Plugins;
import org.foxtrot.hermetrics.flink.Topology;
import org.foxtrot.hermetrics.flink.source.KafkaSources;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.GlobalConfiguration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.foxtrot.hermetrics.flink.config.JobConfig;
import org.foxtrot.hermetrics.flink.config.JobConfigLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class HermetricsJob {

    private HermetricsJob() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: hermetrics <job-config.json>");
            System.exit(2);
        }
        Plugins plugins = Plugins.defaults();
        JobConfig config = new JobConfigLoader(plugins.configLoader()).load(Files.readString(Path.of(args[0])));

        StreamExecutionEnvironment env = executionEnvironment(config);
        configure(env, config);
        Topology.build(config,
                KafkaSources.envRecords(env, config),
                KafkaSources.controlStream(env, config),
                plugins);
        env.execute("hermetrics");
    }

    private static StreamExecutionEnvironment executionEnvironment(JobConfig config) {
        if (config.isLocal()) {
            return StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(flinkConfiguration());
        }
        return StreamExecutionEnvironment.getExecutionEnvironment();
    }

    private static Configuration flinkConfiguration() {
        return GlobalConfiguration.loadConfiguration(HermetricsJob.class.getResource("/").getPath());
    }

    private static void configure(StreamExecutionEnvironment env, JobConfig config) {
        if (config.parallelism() != null) {
            env.setParallelism(config.parallelism());
        }
        if (config.checkpointIntervalMillis() != null) {
            env.enableCheckpointing(config.checkpointIntervalMillis());
        }
    }
}
