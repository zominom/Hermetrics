package org.foxtrot.hermetrics.match.state;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public final class Timeline implements Serializable {

    public ArrayList<StateVersion> versions = new ArrayList<>();
    public long messageCount;
    public long duplicateCount;

    public boolean isEmpty() {
        return messageCount == 0;
    }

    public int distinctStates() {
        return versions.size();
    }

    public boolean sequenced() {
        return versions.stream().anyMatch(version -> version.sequence != null);
    }

    public Set<String> hashes() {
        Set<String> hashes = new HashSet<>();
        versions.forEach(version -> hashes.add(version.hash));
        return hashes;
    }

    public StateVersion bySequence(String sequence) {
        for (StateVersion version : versions) {
            if (sequence.equals(version.sequence)) {
                return version;
            }
        }
        return null;
    }

    public StateVersion finalVersion() {
        if (versions.isEmpty()) {
            return null;
        }
        if (!sequenced()) {
            return versions.get(versions.size() - 1);
        }
        StateVersion latest = versions.get(0);
        for (StateVersion candidate : versions) {
            if (isNewer(candidate, latest)) {
                latest = candidate;
            }
        }
        return latest;
    }

    public static int compareSequences(String a, String b) {
        if (a == null || b == null) {
            return a == null && b == null ? 0 : (a == null ? -1 : 1);
        }
        try {
            return new BigDecimal(a).compareTo(new BigDecimal(b));
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }

    public void observe(String sequence, String hash, String stateJson, long eventTimeMillis) {
        messageCount++;
        if (sequence == null) {
            observeUnsequenced(hash, stateJson, eventTimeMillis);
        } else {
            observeSequenced(sequence, hash, stateJson, eventTimeMillis);
        }
    }

    private void observeUnsequenced(String hash, String stateJson, long eventTimeMillis) {
        if (hashes().contains(hash)) {
            duplicateCount++;
            return;
        }
        dropStoredStateOfTail();
        versions.add(new StateVersion(null, hash, stateJson, eventTimeMillis));
    }

    private void dropStoredStateOfTail() {
        if (!versions.isEmpty()) {
            versions.get(versions.size() - 1).stateJson = null;
        }
    }

    private void observeSequenced(String sequence, String hash, String stateJson, long eventTimeMillis) {
        StateVersion existing = bySequence(sequence);
        if (existing == null) {
            versions.add(new StateVersion(sequence, hash, stateJson, eventTimeMillis));
            return;
        }
        if (existing.hash.equals(hash)) {
            duplicateCount++;
            return;
        }
        if (eventTimeMillis >= existing.eventTimeMillis) {
            existing.hash = hash;
            existing.stateJson = stateJson;
            existing.eventTimeMillis = eventTimeMillis;
        }
    }

    private static boolean isNewer(StateVersion candidate, StateVersion current) {
        int bySequence = compareSequences(candidate.sequence, current.sequence);
        if (bySequence != 0) {
            return bySequence > 0;
        }
        return candidate.eventTimeMillis > current.eventTimeMillis;
    }
}
