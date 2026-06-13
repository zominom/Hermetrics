package org.foxtrot.hermetrics.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.foxtrot.hermetrics.canonical.Path;
import org.foxtrot.hermetrics.diff.DiffSignature;
import org.foxtrot.hermetrics.diff.FieldDiff;
import org.foxtrot.hermetrics.match.Severity;
import org.foxtrot.hermetrics.match.Verdict;
import org.foxtrot.hermetrics.match.VerdictStats;
import org.foxtrot.hermetrics.match.VerdictStatus;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonFindingCodecTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonFindingCodec codec = new JsonFindingCodec();

    private static JsonNode parse(String json) throws Exception {
        return MAPPER.readTree(json);
    }

    @Test
    void diffVerdictCarriesEverything() throws Exception {
        List<FieldDiff> diffs = List.of(
                new FieldDiff(Path.parse("total"), FieldDiff.Kind.VALUE_CHANGED, "10", "12"));
        Verdict verdict = new Verdict("g1", "orders", VerdictStatus.DIFF, Severity.ERROR,
                DiffSignature.of(diffs), diffs, new VerdictStats(2, 1, 2, 1, 0, 0, 1, 0), 3, 42L);

        JsonNode node = parse(codec.verdict(verdict));
        assertThat(node.get("type").asText()).isEqualTo("verdict");
        assertThat(node.get("guid").asText()).isEqualTo("g1");
        assertThat(node.get("topic").asText()).isEqualTo("orders");
        assertThat(node.get("status").asText()).isEqualTo("DIFF");
        assertThat(node.get("severity").asText()).isEqualTo("ERROR");
        assertThat(node.get("revision").asInt()).isEqualTo(3);
        assertThat(node.get("decidedAtMillis").asLong()).isEqualTo(42L);
        assertThat(node.get("signatureId").asText()).hasSize(12);
        assertThat(node.get("signature").get(0).asText()).isEqualTo("total: value-changed");
        JsonNode diff = node.get("diffs").get(0);
        assertThat(diff.get("path").asText()).isEqualTo("total");
        assertThat(diff.get("kind").asText()).isEqualTo("value-changed");
        assertThat(diff.get("main").asText()).isEqualTo("10");
        assertThat(diff.get("load").asText()).isEqualTo("12");
        assertThat(node.get("stats").get("mainMessages").asLong()).isEqualTo(2);
        assertThat(node.get("stats").get("missingSequencesInLoad").asInt()).isEqualTo(1);
    }

    @Test
    void equalVerdictStaysLean() throws Exception {
        Verdict verdict = new Verdict("g1", "orders", VerdictStatus.EQUAL, Severity.OK,
                null, List.of(), new VerdictStats(1, 1, 1, 1, 0, 0, 0, 0), 1, 42L);
        JsonNode node = parse(codec.verdict(verdict));
        assertThat(node.has("signatureId")).isFalse();
        assertThat(node.has("signature")).isFalse();
        assertThat(node.has("diffs")).isFalse();
    }

    @Test
    void sampleValuesAreTruncated() throws Exception {
        String huge = "x".repeat(3000);
        List<FieldDiff> diffs = List.of(
                new FieldDiff(Path.parse("blob"), FieldDiff.Kind.VALUE_CHANGED, huge, "small"));
        Verdict verdict = new Verdict("g1", "orders", VerdictStatus.DIFF, Severity.ERROR,
                DiffSignature.of(diffs), diffs, new VerdictStats(1, 1, 1, 1, 0, 0, 0, 0), 1, 42L);
        String main = parse(codec.verdict(verdict)).get("diffs").get(0).get("main").asText();
        assertThat(main).endsWith("…(truncated)");
        assertThat(main.length()).isLessThan(2100);
    }

    @Test
    void deadLetterShape() throws Exception {
        String json = codec.deadLetter(new DeadLetter("MAIN", "orders", 7L, "no GUID at 'doc.guid'",
                "{\"broken\": true}".getBytes(StandardCharsets.UTF_8)));
        JsonNode node = parse(json);
        assertThat(node.get("type").asText()).isEqualTo("dead-letter");
        assertThat(node.get("env").asText()).isEqualTo("MAIN");
        assertThat(node.get("topic").asText()).isEqualTo("orders");
        assertThat(node.get("error").asText()).contains("doc.guid");
        assertThat(node.get("payloadPreview").asText()).contains("broken");
    }

    @Test
    void rollupShape() throws Exception {
        String json = codec.rollup(new Rollup("orders", "DIFF", "ERROR", "abc123def456",
                List.of("a: added", "b: removed"), 41_203, List.of("g1", "g2"), 1000L, 2000L));
        JsonNode node = parse(json);
        assertThat(node.get("type").asText()).isEqualTo("rollup");
        assertThat(node.get("count").asLong()).isEqualTo(41_203);
        assertThat(node.get("signature")).hasSize(2);
        assertThat(node.get("sampleGuids")).hasSize(2);
        assertThat(node.get("windowStartMillis").asLong()).isEqualTo(1000L);
    }

    @Test
    void rollupWithoutSignatureOmitsIt() throws Exception {
        String json = codec.rollup(new Rollup("orders", "EQUAL", "OK", "",
                List.of(), 10, List.of("g1"), 0L, 1L));
        JsonNode node = parse(json);
        assertThat(node.has("signatureId")).isFalse();
        assertThat(node.has("signature")).isFalse();
    }
}
