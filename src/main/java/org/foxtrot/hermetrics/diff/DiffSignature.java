package org.foxtrot.hermetrics.diff;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public record DiffSignature(List<String> entries, String id) implements Serializable {

    public DiffSignature {
        entries = List.copyOf(entries);
    }

    public static DiffSignature of(List<FieldDiff> diffs) {
        if (diffs.isEmpty()) {
            throw new IllegalArgumentException("cannot build a signature from an empty diff");
        }
        List<String> entries = diffs.stream()
                .map(diff -> diff.path().generalized() + ": " + diff.kind().label())
                .distinct()
                .sorted()
                .toList();
        return new DiffSignature(entries, shortHash(String.join("\n", entries)));
    }

    private static String shortHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String hex = HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
            return hex.substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
