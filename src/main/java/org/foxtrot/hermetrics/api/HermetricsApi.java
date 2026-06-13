package org.foxtrot.hermetrics.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.foxtrot.hermetrics.config.ConfigLoader;
import org.foxtrot.hermetrics.rules.loader.RuleTypeRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HermetricsApi {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: hermetrics-api <api-config.json>");
            System.exit(2);
        }
        ApiConfig config = ApiConfig.load(args[0]);
        new HermetricsApi(config).start();
        System.out.println("hermetrics-api listening on :" + config.port());
    }

    private final ApiConfig config;
    private final ConfigLoader configLoader = new ConfigLoader();
    private final RuleCatalog ruleCatalog = new RuleCatalog(RuleTypeRegistry.withDefaults());
    private final ControlState control;
    private final ControlPublisher publisher;
    private final TopicTail verdicts;
    private final TopicTail rollups;
    private final TopicTail deadLetters;

    public HermetricsApi(ApiConfig config) {
        this.config = config;
        this.control = new ControlState(config);
        this.publisher = new ControlPublisher(config, configLoader);
        this.verdicts = new TopicTail(config, config.resultsTopic(), "verdicts");
        this.rollups = new TopicTail(config, config.rollupsTopic(), "rollups");
        this.deadLetters = config.deadLetterTopic() == null
                ? null
                : new TopicTail(config, config.deadLetterTopic(), "deadletters");
    }

    public void start() throws Exception {
        new HttpApi(config.port())
                .route("GET /api/health", request -> Map.of("ok", true))
                .route("GET /api/rule-types", request -> ruleCatalog.describe())
                .route("GET /api/config/active", request -> activeConfig())
                .route("POST /api/config/validate", request -> validate(request.body()))
                .route("POST /api/config/apply", this::apply)
                .route("GET /api/verdicts", request -> verdicts.recent(request.intQuery("limit", 100)))
                .route("GET /api/rollups", request -> rollups.recent(request.intQuery("limit", 100)))
                .route("GET /api/dead-letters", request -> recentDeadLetters(request.intQuery("limit", 100)))
                .route("GET /api/summary", request -> summary())
                .start();
    }

    private Map<String, Object> activeConfig() throws Exception {
        Map<String, Object> response = new LinkedHashMap<>();
        String active = control.activeCompareJson();
        response.put("active", active == null ? null : HttpApi.JSON.readTree(active));
        response.put("source", control.source());
        response.put("flinkUiUrl", config.flinkUiUrl());
        return response;
    }

    private Map<String, Object> validate(JsonNode compareConfig) {
        if (compareConfig == null) {
            return Map.of("valid", false, "error", "request body must be a compare config object");
        }
        try {
            configLoader.load(compareConfig.toString());
            return Map.of("valid", true);
        } catch (Exception e) {
            return Map.of("valid", false, "error", String.valueOf(e.getMessage()));
        }
    }

    private Map<String, Object> apply(HttpApi.Request request) {
        if (request.body() == null) {
            throw new HttpApi.HttpError(400, "request body must be a compare config object");
        }
        publisher.apply(request.body());
        return Map.of("applied", true);
    }

    private List<JsonNode> recentDeadLetters(int limit) {
        return deadLetters == null ? List.of() : deadLetters.recent(limit);
    }

    private Map<String, Object> summary() {
        Map<String, Long> counts = new LinkedHashMap<>();
        List<JsonNode> recent = verdicts.recent(config.tailSize());
        for (JsonNode verdict : recent) {
            String status = verdict.path("status").asText("UNKNOWN");
            counts.merge(status, 1L, Long::sum);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", recent.size());
        response.put("counts", counts);
        return response;
    }
}
