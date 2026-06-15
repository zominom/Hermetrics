package org.foxtrot.hermetrics.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public final class HttpApi {

    public static final ObjectMapper JSON = new ObjectMapper();

    @FunctionalInterface
    public interface Handler {
        Object handle(Request request) throws Exception;
    }

    public record Request(String method, String path, Map<String, String> query, JsonNode body) {
        public int intQuery(String name, int fallback) {
            String value = query.get(name);
            return value == null ? fallback : Integer.parseInt(value);
        }
    }

    public static final class HttpError extends RuntimeException {
        final int status;

        public HttpError(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    public record Raw(String contentType, byte[] body) {
    }

    private final HttpServer server;
    private final Map<String, Handler> routes = new HashMap<>();

    public HttpApi(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.setExecutor(Executors.newFixedThreadPool(8));
        this.server.createContext("/", this::dispatch);
    }

    public HttpApi route(String methodAndPath, Handler handler) {
        routes.put(methodAndPath, handler);
        return this;
    }

    public void start() {
        server.start();
    }

    private void dispatch(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        if (exchange.getRequestMethod().equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        String path = exchange.getRequestURI().getPath();
        Handler handler = routes.get(exchange.getRequestMethod() + " " + path);
        try {
            if (handler == null) {
                throw new HttpError(404, "no route for " + path);
            }
            Object result = handler.handle(request(exchange));
            respond(exchange, 200, result);
        } catch (HttpError e) {
            respond(exchange, e.status, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            respond(exchange, 400, Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    private static Request request(HttpExchange exchange) throws IOException {
        byte[] raw = exchange.getRequestBody().readAllBytes();
        JsonNode body = raw.length == 0 ? null : JSON.readTree(raw);
        return new Request(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                parseQuery(exchange.getRequestURI().getRawQuery()), body);
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> query = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return query;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                query.put(pair.substring(0, eq), pair.substring(eq + 1));
            }
        }
        return query;
    }

    private static void respond(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes;
        String contentType;
        if (body instanceof Raw raw) {
            bytes = raw.body();
            contentType = raw.contentType();
        } else {
            bytes = JSON.writeValueAsBytes(body);
            contentType = "application/json";
        }
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
