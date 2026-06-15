package org.foxtrot.hermetrics.api.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.foxtrot.hermetrics.api.FindingsQuery;
import org.foxtrot.hermetrics.api.FindingsStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads findings from Elasticsearch over the REST API (JDK HttpClient, no ES client
 * dependency). Assumes all findings live in one index with default dynamic mappings,
 * so string fields have {@code .keyword} sub-fields for term filters and aggregations.
 * Options: {@code url} (required), {@code index} (default hermetrics-findings),
 * and either {@code apiKey} or {@code username}+{@code password}.
 *
 * <p>Untested against a live cluster — verify the index/mappings match before relying on it.
 */
public final class ElasticsearchFindingsStore implements FindingsStore {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http = HttpClient.newHttpClient();
    private final String searchUrl;
    private final String authHeader;

    public ElasticsearchFindingsStore(Map<String, String> options) {
        String url = require(options, "url");
        String index = options.getOrDefault("index", "hermetrics-findings");
        this.searchUrl = stripTrailingSlash(url) + "/" + index + "/_search";
        this.authHeader = authHeader(options);
    }

    @Override
    public List<JsonNode> verdicts(FindingsQuery query) {
        return search("verdict", "decidedAtMillis", query);
    }

    @Override
    public List<JsonNode> rollups(FindingsQuery query) {
        return search("rollup", "windowStartMillis", query);
    }

    @Override
    public List<JsonNode> deadLetters(FindingsQuery query) {
        return search("dead-letter", "timestampMillis", query);
    }

    @Override
    public Map<String, Object> summary(FindingsQuery query) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("size", 0);
        body.set("query", typeQuery("verdict", query));
        ObjectNode terms = body.putObject("aggs").putObject("status").putObject("terms");
        terms.put("field", "status.keyword");
        terms.put("size", 50);
        JsonNode response = post(body);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (JsonNode bucket : response.path("aggregations").path("status").path("buckets")) {
            counts.put(bucket.path("key").asText(), bucket.path("doc_count").asLong());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", response.path("hits").path("total").path("value").asLong(0));
        out.put("counts", counts);
        return out;
    }

    private List<JsonNode> search(String type, String sortField, FindingsQuery query) {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("size", Math.max(0, query.limit()));
        ArrayNode sort = body.putArray("sort");
        sort.addObject().putObject(sortField).put("order", "desc");
        body.set("query", typeQuery(type, query));
        List<JsonNode> out = new ArrayList<>();
        for (JsonNode hit : post(body).path("hits").path("hits")) {
            out.add(hit.path("_source"));
        }
        return out;
    }

    private static ObjectNode typeQuery(String type, FindingsQuery query) {
        ObjectNode bool = MAPPER.createObjectNode();
        ArrayNode filter = bool.putObject("bool").putArray("filter");
        filter.addObject().putObject("term").put("type.keyword", type);
        if (query.hasTopic()) {
            filter.addObject().putObject("term").put("topic.keyword", query.topic());
        }
        return bool;
    }

    private JsonNode post(ObjectNode body) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(searchUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
            if (authHeader != null) {
                request.header("Authorization", authHeader);
            }
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("elasticsearch " + response.statusCode() + ": " + response.body());
            }
            return MAPPER.readTree(response.body());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("elasticsearch query failed: " + e.getMessage(), e);
        }
    }

    private static String authHeader(Map<String, String> options) {
        String apiKey = options.get("apiKey");
        if (apiKey != null) {
            return "ApiKey " + apiKey;
        }
        String user = options.get("username");
        String password = options.get("password");
        if (user != null && password != null) {
            return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    private static String require(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("elasticsearch findings store requires option '" + key + "'");
        }
        return value;
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
