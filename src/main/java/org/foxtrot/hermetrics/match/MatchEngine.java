package org.foxtrot.hermetrics.match;

import org.foxtrot.hermetrics.canonical.CanonicalJsonReader;
import org.foxtrot.hermetrics.diff.Differ;
import org.foxtrot.hermetrics.diff.DiffSignature;
import org.foxtrot.hermetrics.diff.FieldDiff;
import org.foxtrot.hermetrics.rules.RuleSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

public final class MatchEngine {

    private final MatchPolicy policy;
    private final Function<String, RuleSet> ruleSets;
    private final Differ differ;

    public MatchEngine(MatchPolicy policy, Function<String, RuleSet> ruleSetsByTopic, Differ differ) {
        this.policy = policy;
        this.ruleSets = ruleSetsByTopic;
        this.differ = differ;
    }

    public MatchPolicy policy() {
        return policy;
    }

    public void onObservation(GuidState state, Observation observation) {
        long ts = observation.eventTimeMillis();
        state.firstSeenMillis = state.firstSeenMillis < 0 ? ts : Math.min(state.firstSeenMillis, ts);
        state.lastSeenMillis = Math.max(state.lastSeenMillis, ts);
        if (observation.kind() == Observation.Kind.ENTRY) {
            recordEntry(state, observation.env());
            return;
        }
        state.topics.computeIfAbsent(observation.topic(), topic -> new TopicPair())
                .timeline(observation.env())
                .observe(observation.stateHash(), observation.stateJson());
    }

    public long nextDecisionTime(GuidState state) {
        if (state.lastSeenMillis < 0) {
            return -1;
        }
        return Math.min(state.lastSeenMillis + policy.quietMillis(),
                state.firstSeenMillis + policy.maxWaitMillis());
    }

    public List<Verdict> decide(String guid, GuidState state, long nowMillis) {
        if (withinCohort(state)) {
            return judgeTopics(guid, state, nowMillis);
        }
        if (state.enteredMain) {
            return List.of(cohortVerdict(guid, state, VerdictStatus.NOT_MIRRORED, nowMillis));
        }
        if (state.enteredLoad) {
            return List.of(cohortVerdict(guid, state, VerdictStatus.TEST_TRAFFIC, nowMillis));
        }
        return unanchoredVerdicts(guid, state, nowMillis);
    }

    private static void recordEntry(GuidState state, Env env) {
        if (env == Env.MAIN) {
            state.enteredMain = true;
        } else {
            state.enteredLoad = true;
        }
    }

    private boolean withinCohort(GuidState state) {
        return policy.cohortMode() == MatchPolicy.CohortMode.ASSUME_ALL
                || (state.enteredMain && state.enteredLoad);
    }

    private List<Verdict> judgeTopics(String guid, GuidState state, long nowMillis) {
        List<Verdict> verdicts = new ArrayList<>();
        for (var entry : state.topics.entrySet()) {
            Verdict verdict = judgeTopic(guid, entry.getKey(), entry.getValue(), nowMillis);
            if (verdict != null) {
                verdicts.add(verdict);
            }
        }
        return verdicts;
    }

    private Verdict judgeTopic(String guid, String topic, TopicPair pair, long nowMillis) {
        Timeline main = pair.main;
        Timeline load = pair.load;
        if (main.isEmpty() && load.isEmpty()) {
            return null;
        }
        int revision = ++pair.revision;
        VerdictStats stats = VerdictStats.of(pair);
        if (load.isEmpty()) {
            return new Verdict(guid, topic, VerdictStatus.MISSING_IN_LOAD, Severity.ERROR,
                    null, List.of(), stats, revision, nowMillis);
        }
        if (main.isEmpty()) {
            return new Verdict(guid, topic, VerdictStatus.EXTRA_IN_LOAD, Severity.WARN,
                    null, List.of(), stats, revision, nowMillis);
        }
        return compareFinalStates(guid, topic, pair, stats, revision, nowMillis);
    }

    private Verdict compareFinalStates(String guid, String topic, TopicPair pair,
                                       VerdictStats stats, int revision, long nowMillis) {
        List<FieldDiff> diffs = differ.diff(
                CanonicalJsonReader.parse(pair.main.lastStateJson),
                CanonicalJsonReader.parse(pair.load.lastStateJson),
                ruleSetFor(topic));
        if (!diffs.isEmpty()) {
            return new Verdict(guid, topic, VerdictStatus.DIFF, Severity.ERROR,
                    DiffSignature.of(diffs), diffs, stats, revision, nowMillis);
        }
        if (sameDistinctStates(pair)) {
            return new Verdict(guid, topic, VerdictStatus.EQUAL, Severity.OK,
                    null, List.of(), stats, revision, nowMillis);
        }
        Severity severity = policy.strictIntermediates() ? Severity.WARN : Severity.INFO;
        return new Verdict(guid, topic, VerdictStatus.EQUAL_DIVERGED, severity,
                null, List.of(), stats, revision, nowMillis);
    }

    private RuleSet ruleSetFor(String topic) {
        RuleSet rules = ruleSets.apply(topic);
        return rules == null ? RuleSet.EMPTY : rules;
    }

    private static boolean sameDistinctStates(TopicPair pair) {
        return new HashSet<>(pair.main.distinctHashes).equals(new HashSet<>(pair.load.distinctHashes));
    }

    private List<Verdict> unanchoredVerdicts(String guid, GuidState state, long nowMillis) {
        List<Verdict> verdicts = new ArrayList<>();
        for (var entry : state.topics.entrySet()) {
            TopicPair pair = entry.getValue();
            verdicts.add(new Verdict(guid, entry.getKey(), VerdictStatus.UNANCHORED,
                    Severity.WARN, null, List.of(), VerdictStats.of(pair),
                    ++pair.revision, nowMillis));
        }
        return verdicts;
    }

    private Verdict cohortVerdict(String guid, GuidState state, VerdictStatus status, long nowMillis) {
        return new Verdict(guid, Verdict.COHORT_TOPIC, status, Severity.INFO,
                null, List.of(), VerdictStats.aggregate(state), ++state.cohortRevision, nowMillis);
    }
}
