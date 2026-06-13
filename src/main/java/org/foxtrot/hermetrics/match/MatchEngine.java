package org.foxtrot.hermetrics.match;

import org.foxtrot.hermetrics.match.state.GuidState;
import org.foxtrot.hermetrics.match.state.StateVersion;
import org.foxtrot.hermetrics.match.state.Timeline;
import org.foxtrot.hermetrics.match.state.TopicPair;
import org.foxtrot.hermetrics.match.verdict.Severity;
import org.foxtrot.hermetrics.match.verdict.Verdict;
import org.foxtrot.hermetrics.match.verdict.VerdictStats;
import org.foxtrot.hermetrics.match.verdict.VerdictStatus;

import org.foxtrot.hermetrics.canonical.json.CanonicalJsonReader;
import org.foxtrot.hermetrics.canonical.value.CanonicalValue;
import org.foxtrot.hermetrics.diff.Differ;
import org.foxtrot.hermetrics.diff.DiffSignature;
import org.foxtrot.hermetrics.diff.FieldDiff;
import org.foxtrot.hermetrics.rules.RuleSet;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
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
                .observe(observation.sequence(), observation.stateHash(),
                        observation.stateJson(), observation.eventTimeMillis());
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
        if (pair.main.isEmpty() && pair.load.isEmpty()) {
            return null;
        }
        int revision = ++pair.revision;
        if (pair.load.isEmpty()) {
            return new Verdict(guid, topic, VerdictStatus.MISSING_IN_LOAD, Severity.ERROR,
                    null, List.of(), VerdictStats.of(pair), revision, nowMillis);
        }
        if (pair.main.isEmpty()) {
            return new Verdict(guid, topic, VerdictStatus.EXTRA_IN_LOAD, Severity.WARN,
                    null, List.of(), VerdictStats.of(pair), revision, nowMillis);
        }
        return compareStates(guid, topic, pair, revision, nowMillis);
    }

    private Verdict compareStates(String guid, String topic, TopicPair pair, int revision, long nowMillis) {
        RuleSet rules = ruleSetFor(topic);
        List<FieldDiff> finalDiffs = differ.diff(
                parse(pair.main.finalVersion().stateJson),
                parse(pair.load.finalVersion().stateJson),
                rules);
        if (!finalDiffs.isEmpty()) {
            return new Verdict(guid, topic, VerdictStatus.DIFF, Severity.ERROR,
                    DiffSignature.of(finalDiffs), finalDiffs, VerdictStats.of(pair), revision, nowMillis);
        }
        if (pair.main.sequenced() || pair.load.sequenced()) {
            return judgeSequencedPath(guid, topic, pair, rules, revision, nowMillis);
        }
        return judgeUnsequencedPath(guid, topic, pair, revision, nowMillis);
    }

    private Verdict judgeUnsequencedPath(String guid, String topic, TopicPair pair, int revision, long nowMillis) {
        if (pair.main.hashes().equals(pair.load.hashes())) {
            return new Verdict(guid, topic, VerdictStatus.EQUAL, Severity.OK,
                    null, List.of(), VerdictStats.of(pair), revision, nowMillis);
        }
        return new Verdict(guid, topic, VerdictStatus.EQUAL_DIVERGED, divergedSeverity(),
                null, List.of(), VerdictStats.of(pair), revision, nowMillis);
    }

    private Verdict judgeSequencedPath(String guid, String topic, TopicPair pair, RuleSet rules,
                                       int revision, long nowMillis) {
        List<FieldDiff> pairDiffs = new ArrayList<>();
        int missingInLoad = 0;
        int extraInLoad = 0;
        boolean everyMessageEqual = true;

        for (String sequence : allSequences(pair)) {
            StateVersion main = pair.main.bySequence(sequence);
            StateVersion load = pair.load.bySequence(sequence);
            if (main == null) {
                extraInLoad++;
                everyMessageEqual = false;
            } else if (load == null) {
                missingInLoad++;
                everyMessageEqual = false;
            } else if (!main.hash.equals(load.hash)) {
                List<FieldDiff> diffs = differ.diff(parse(main.stateJson), parse(load.stateJson), rules);
                if (!diffs.isEmpty()) {
                    everyMessageEqual = false;
                    pairDiffs.addAll(diffs);
                }
            }
        }

        VerdictStats stats = VerdictStats.of(pair, missingInLoad, extraInLoad);
        if (everyMessageEqual) {
            return new Verdict(guid, topic, VerdictStatus.EQUAL, Severity.OK,
                    null, List.of(), stats, revision, nowMillis);
        }
        DiffSignature signature = pairDiffs.isEmpty() ? null : DiffSignature.of(pairDiffs);
        return new Verdict(guid, topic, VerdictStatus.EQUAL_DIVERGED, divergedSeverity(),
                signature, pairDiffs, stats, revision, nowMillis);
    }

    private static TreeSet<String> allSequences(TopicPair pair) {
        TreeSet<String> sequences = new TreeSet<>(Timeline::compareSequences);
        for (StateVersion version : pair.main.versions) {
            if (version.sequence != null) {
                sequences.add(version.sequence);
            }
        }
        for (StateVersion version : pair.load.versions) {
            if (version.sequence != null) {
                sequences.add(version.sequence);
            }
        }
        return sequences;
    }

    private Severity divergedSeverity() {
        return policy.strictIntermediates() ? Severity.WARN : Severity.INFO;
    }

    private RuleSet ruleSetFor(String topic) {
        RuleSet rules = ruleSets.apply(topic);
        return rules == null ? RuleSet.EMPTY : rules;
    }

    private static CanonicalValue parse(String stateJson) {
        return CanonicalJsonReader.parse(stateJson);
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
