package org.foxtrot.hermetrics.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.foxtrot.hermetrics.config.ConfigLoader;
import org.foxtrot.hermetrics.rules.loader.RuleTypeRegistry;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HermetricsApi {

    private static final byte[] OPENAPI = loadResource("/openapi.yaml");
    private static final byte[] DOCS_HTML = ("""
            <!doctype html>
            <html>
              <head>
                <meta charset="utf-8"/>
                <title>hermetrics API</title>
                <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css"/>
              </head>
              <body>
                <div id="swagger-ui"></div>
                <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
                <script>window.ui = SwaggerUIBundle({ url: "/openapi.yaml", dom_id: "#swagger-ui" });</script>
              </body>
            </html>
            """).getBytes(StandardCharsets.UTF_8);

    private static byte[] loadResource(String path) {
        try (InputStream in = HermetricsApi.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("resource not found: " + path);
            }
            return in.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("failed to load " + path, e);
        }
    }

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
    private final FindingsStore store;

    public HermetricsApi(ApiConfig config) {
        this.config = config;
        this.control = new ControlState(config);
        this.publisher = new ControlPublisher(config, configLoader);
        this.store = FindingsStoreRegistry.withDefaults().create(config);
    }

    public void start() throws Exception {
        new HttpApi(config.port())
                .route("GET /api/health", request -> Map.of("ok", true))
                .route("GET /api/rule-types", request -> ruleCatalog.describe())
                .route("GET /api/config/active", request -> activeConfig())
                .route("POST /api/config/validate", request -> validate(request.body()))
                .route("POST /api/config/apply", this::apply)
                .route("GET /api/verdicts", request -> store.verdicts(query(request)))
                .route("GET /api/rollups", request -> store.rollups(query(request)))
                .route("GET /api/dead-letters", request -> store.deadLetters(query(request)))
                .route("GET /api/summary", request -> store.summary(query(request)))
                .route("GET /openapi.yaml", request -> new HttpApi.Raw("application/yaml", OPENAPI))
                .route("GET /docs", request -> new HttpApi.Raw("text/html; charset=utf-8", DOCS_HTML))
                .start();
    }

    private static FindingsQuery query(HttpApi.Request request) {
        return new FindingsQuery(request.query().get("topic"), request.intQuery("limit", 100));
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
}
