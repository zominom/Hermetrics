package org.foxtrot.hermetrics.match;

import java.io.Serializable;

public record VerdictStats(long mainMessages, long loadMessages,
                           int mainDistinctStates, int loadDistinctStates,
                           long mainDuplicates, long loadDuplicates) implements Serializable {

    public static VerdictStats of(TopicPair pair) {
        return new VerdictStats(
                pair.main.messageCount, pair.load.messageCount,
                pair.main.distinctHashes.size(), pair.load.distinctHashes.size(),
                pair.main.duplicateCount, pair.load.duplicateCount);
    }

    public static VerdictStats aggregate(GuidState state) {
        long mainMessages = 0;
        long loadMessages = 0;
        int mainDistinct = 0;
        int loadDistinct = 0;
        long mainDuplicates = 0;
        long loadDuplicates = 0;
        for (TopicPair pair : state.topics.values()) {
            mainMessages += pair.main.messageCount;
            loadMessages += pair.load.messageCount;
            mainDistinct += pair.main.distinctHashes.size();
            loadDistinct += pair.load.distinctHashes.size();
            mainDuplicates += pair.main.duplicateCount;
            loadDuplicates += pair.load.duplicateCount;
        }
        return new VerdictStats(mainMessages, loadMessages, mainDistinct, loadDistinct,
                mainDuplicates, loadDuplicates);
    }
}
