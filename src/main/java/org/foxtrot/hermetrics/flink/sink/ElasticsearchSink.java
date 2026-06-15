package org.foxtrot.hermetrics.flink.sink;

import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.api.connector.sink2.WriterInitContext;
import org.foxtrot.hermetrics.flink.record.KeyedRecord;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Indexes each finding into Elasticsearch over the REST API (one PUT per record,
 * document id = the record key so re-deliveries upsert). No ES client dependency.
 *
 * <p>Synchronous one-by-one indexing is fine for low/moderate volume but is a
 * bottleneck at scale — for high throughput prefer Kafka Connect (results topic →
 * ES sink connector), which batches. Kept as a turnkey option, not the default.
 */
final class ElasticsearchSink implements Sink<KeyedRecord> {

    private final String url;
    private final String index;
    private final String authHeader;

    ElasticsearchSink(String url, String index, String authHeader) {
        this.url = url;
        this.index = index;
        this.authHeader = authHeader;
    }

    @Override
    public SinkWriter<KeyedRecord> createWriter(WriterInitContext context) {
        return new Writer(url, index, authHeader);
    }

    static final class Writer implements SinkWriter<KeyedRecord> {

        private final String base;
        private final String index;
        private final String authHeader;
        private final HttpClient http = HttpClient.newHttpClient();

        Writer(String url, String index, String authHeader) {
            this.base = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
            this.index = index;
            this.authHeader = authHeader;
        }

        @Override
        public void write(KeyedRecord record, Context context) throws IOException {
            String id = URLEncoder.encode(record.key(), StandardCharsets.UTF_8);
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + "/" + index + "/_doc/" + id))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(record.json(), StandardCharsets.UTF_8));
            if (authHeader != null) {
                request.header("Authorization", authHeader);
            }
            try {
                HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() / 100 != 2) {
                    throw new IOException("elasticsearch " + response.statusCode() + ": " + response.body());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("interrupted indexing to elasticsearch", e);
            }
        }

        @Override
        public void flush(boolean endOfInput) {
        }

        @Override
        public void close() {
        }
    }
}
