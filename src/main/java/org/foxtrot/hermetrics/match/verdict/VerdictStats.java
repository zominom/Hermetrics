package org.foxtrot.hermetrics.match.verdict;

import org.foxtrot.hermetrics.match.state.GuidState;
import org.foxtrot.hermetrics.match.state.TopicPair;

import java.io.Serializable;

public record VerdictStats(long mainMessages, long loadMessages,
                           int mainDistinctStates, int loadDistinctStates,
                           long mainDuplicates, long loadDuplicates,
                           int missingSequencesInLoad, int extraSequencesInLoad) implements Serializable {

    public static VerdictStats of(TopicPair pair) {
        return of(pair, 0, 0);
    }

    public static VerdictStats of(TopicPair pair, int missingSequencesInLoad, int extraSequencesInLoad) {
        return new VerdictStats(
                pair.main.messageCount, pair.load.messageCount,
                pair.main.distinctStates(), pair.load.distinctStates(),
                pair.main.duplicateCount, pair.load.duplicateCount,
                missingSequencesInLoad, extraSequencesInLoad);
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
            mainDistinct += pair.main.distinctStates();
            loadDistinct += pair.load.distinctStates();
            mainDuplicates += pair.main.duplicateCount;
            loadDuplicates += pair.load.duplicateCount;
        }
        return new VerdictStats(mainMessages, loadMessages, mainDistinct, loadDistinct,
                mainDuplicates, loadDuplicates, 0, 0);
    }
}
