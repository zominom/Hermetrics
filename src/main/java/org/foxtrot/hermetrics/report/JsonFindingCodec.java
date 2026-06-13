package org.foxtrot.hermetrics.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.foxtrot.hermetrics.diff.FieldDiff;
import org.foxtrot.hermetrics.match.Verdict;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class JsonFindingCodec implements FindingCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_VALUE_CHARS = 2_000;
    private static final int MAX_DIFFS = 100;
    private static final int MAX_PAYLOAD_PREVIEW_CHARS = 1_000;

    @Override
    public String verdict(Verdict verdict) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("type", "verdict");
        node.put("guid", verdict.guid());
        node.put("topic", verdict.topic());
        node.put("status", verdict.status().name());
        node.put("severity", verdict.severity().name());
        node.put("revision", verdict.revision());
        node.put("decidedAtMillis", verdict.decidedAtMillis());
        appendSignature(verdict, node);
        appendDiffs(verdict, node);
        appendStats(verdict, node);
        return node.toString();
    }

    @Override
    public String deadLetter(DeadLetter deadLetter) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("type", "dead-letter");
        node.put("env", deadLetter.env());
        node.put("topic", deadLetter.topic());
        node.put("timestampMillis", deadLetter.timestampMillis());
        node.put("error", deadLetter.error() == null ? "" : truncate(deadLetter.error(), MAX_VALUE_CHARS));
        node.put("payloadPreview", payloadPreview(deadLetter.payload()));
        return node.toString();
    }

    @Override
    public String rollup(Rollup rollup) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("type", "rollup");
        node.put("windowStartMillis", rollup.windowStartMillis());
        node.put("windowEndMillis", rollup.windowEndMillis());
        node.put("topic", rollup.topic());
        node.put("status", rollup.status());
        node.put("severity", rollup.severity());
        if (!rollup.signatureId().isEmpty()) {
            node.put("signatureId", rollup.signatureId());
            ArrayNode signature = node.putArray("signature");
            rollup.signature().forEach(signature::add);
        }
        node.put("count", rollup.count());
        ArrayNode samples = node.putArray("sampleGuids");
        rollup.sampleGuids().forEach(samples::add);
        return node.toString();
    }

    private static void appendSignature(Verdict verdict, ObjectNode node) {
        if (verdict.signature() == null) {
            return;
        }
        node.put("signatureId", verdict.signature().id());
        ArrayNode signature = node.putArray("signature");
        verdict.signature().entries().forEach(signature::add);
    }

    private static void appendDiffs(Verdict verdict, ObjectNode node) {
        List<FieldDiff> fieldDiffs = verdict.fieldDiffs();
        if (fieldDiffs.isEmpty()) {
            return;
        }
        ArrayNode diffs = node.putArray("diffs");
        for (int i = 0; i < Math.min(fieldDiffs.size(), MAX_DIFFS); i++) {
            appendDiff(fieldDiffs.get(i), diffs.addObject());
        }
        if (fieldDiffs.size() > MAX_DIFFS) {
            node.put("diffsTruncated", true);
        }
    }

    private static void appendDiff(FieldDiff fieldDiff, ObjectNode diff) {
        diff.put("path", fieldDiff.path().render());
        diff.put("kind", fieldDiff.kind().label());
        if (fieldDiff.mainValue() != null) {
            diff.put("main", truncate(fieldDiff.mainValue(), MAX_VALUE_CHARS));
        }
        if (fieldDiff.loadValue() != null) {
            diff.put("load", truncate(fieldDiff.loadValue(), MAX_VALUE_CHARS));
        }
    }

    private static void appendStats(Verdict verdict, ObjectNode node) {
        ObjectNode stats = node.putObject("stats");
        stats.put("mainMessages", verdict.stats().mainMessages());
        stats.put("loadMessages", verdict.stats().loadMessages());
        stats.put("mainDistinctStates", verdict.stats().mainDistinctStates());
        stats.put("loadDistinctStates", verdict.stats().loadDistinctStates());
        stats.put("mainDuplicates", verdict.stats().mainDuplicates());
        stats.put("loadDuplicates", verdict.stats().loadDuplicates());
        stats.put("missingSequencesInLoad", verdict.stats().missingSequencesInLoad());
        stats.put("extraSequencesInLoad", verdict.stats().extraSequencesInLoad());
    }

    private static String payloadPreview(byte[] payload) {
        if (payload == null) {
            return "";
        }
        return truncate(new String(payload, StandardCharsets.UTF_8), MAX_PAYLOAD_PREVIEW_CHARS);
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max) + "…(truncated)";
    }
}
