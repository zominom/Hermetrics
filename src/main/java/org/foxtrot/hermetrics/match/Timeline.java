package org.foxtrot.hermetrics.match;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class Timeline implements Serializable {

    public List<String> distinctHashes = new ArrayList<>();
    public String lastHash;
    public String lastStateJson;
    public long messageCount;
    public long duplicateCount;

    public boolean isEmpty() {
        return messageCount == 0;
    }

    void observe(String hash, String stateJson) {
        messageCount++;
        if (alreadySeen(hash)) {
            duplicateCount++;
            return;
        }
        distinctHashes.add(hash);
        lastHash = hash;
        lastStateJson = stateJson;
    }

    private boolean alreadySeen(String hash) {
        return distinctHashes.contains(hash);
    }
}
