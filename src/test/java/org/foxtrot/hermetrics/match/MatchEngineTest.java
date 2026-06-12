package org.foxtrot.hermetrics.match;

import org.foxtrot.hermetrics.diff.StructuralDiffer;
import org.foxtrot.hermetrics.rules.RuleSet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.foxtrot.hermetrics.testutil.Trees.json;

class MatchEngineTest {

    private static final String TOPIC = "orders.enriched";
    private static final String GUID = "g1";
    private static final long NOW = 100_000L;

    private static MatchEngine engine(MatchPolicy.CohortMode mode, boolean strict) {
        return new MatchEngine(new MatchPolicy(1_000, 10_000, strict, mode),
                topic -> RuleSet.EMPTY, new StructuralDiffer());
    }

    private static GuidState fold(MatchEngine engine, Observation... observations) {
        GuidState state = new GuidState();
        for (Observation observation : observations) {
            engine.onObservation(state, observation);
        }
        return state;
    }

    private static Observation out(Env env, String stateJson, long ts) {
        return Observation.output(env, TOPIC, GUID, json(stateJson), ts);
    }

    private static Observation entry(Env env, long ts) {
        return Observation.entry(env, "pipeline.in", GUID, ts);
    }

    @Test
    void equalWhenFinalStateAndPathMatch() {
        MatchEngine engine = engine(MatchPolicy.CohortMode.ENTRY_TOPICS, false);
        GuidState state = fold(engine,
                entry(Env.MAIN, 1), entry(Env.LOAD, 2),
                out(Env.MAIN, "{\"v\": 1}", 3), out(Env.LOAD, "{\"v\": 1}", 4));
        List<Verdict> verdicts = engine.decide(GUID, state, NOW);
        assertThat(verdicts).singleElement().satisfies(v -> {
            assertThat(v.status()).isEqualTo(VerdictStatus.EQUAL);
            assertThat(v.severity()).isEqualTo(Severity.OK);
            assertThat(v.stats().mainMessages()).isEqualTo(1);
            assertThat(v.stats().loadMessages()).isEqualTo(1);
        });
    }

    @Test
    void duplicateDeliveryCollapsesIntoOneState() {
        MatchEngine engine = engine(MatchPolicy.CohortMode.ASSUME_ALL, false);
        GuidState state = fold(engine,
                out(Env.MAIN, "{\"v\": 1}", 1), out(Env.MAIN, "{\"v\": 1}", 2),
                out(Env.LOAD, "{\"v\": 1}", 3));
        List<Verdict> verdicts = engine.decide(GUID, state, NOW);
        assertThat(verdicts).singleElement().satisfies(v -> {
            assertThat(v.status()).isEqualTo(VerdictStatus.EQUAL);
            assertThat(v.stats().mainMessages()).isEqualTo(2);
            assertThat(v.stats().mainDuplicates()).isEqualTo(1);
            assertThat(v.stats().mainDistinctStates()).isEqualTo(1);
        });
    }

    @Test
    void coalescedUpdatesAreEqualDiverged() {
        MatchEngine engine = engine(MatchPolicy.CohortMode.ASSUME_ALL, false);
        GuidState state = fold(engine,
                out(Env.MAIN, "{\"v\": 1}", 1), out(Env.MAIN, "{\"v\": 2}", 2),
                out(Env.LOAD, "{\"v\": 2}", 3));
        assertThat(engine.decide(GUID, state, NOW)).singleElement().satisfies(v -> {
            assertThat(v.status()).isEqualTo(VerdictStatus.EQUAL_DIVERGED);
            assertThat(v.severity()).isEqualTo(Severity.INFO);
        });
    }

    @Test
    void strictPolicyElevatesDivergedPaths() {
        MatchEngine engine = engine(MatchPolicy.CohortMode.ASSUME_ALL, true);
        GuidState state = fold(engine,
                out(Env.MAIN, "{\"v\": 1}", 1), out(Env.MAIN, "{\"v\": 2}", 2),
                out(Env.LOAD, "{\"v\": 2}", 3));
        assertThat(engine.decide(GUID, state, NOW).get(0).severity()).isEqualTo(Severity.WARN);
    }

    @Test
    void staleResendDoesNotMoveTheFinalState() {
        MatchEngine engine = engine(MatchPolicy.CohortMode.ASSUME_ALL, false);
        GuidState state = fold(engine,
                out(Env.MAIN, "{\"v\": 1}", 1), out(Env.MAIN, "{\"v\": 2}", 2),
                out(Env.MAIN, "{\"v\": 1}", 3), // exact duplicate of the first send
                out(Env.LOAD, "{\"v\": 2}", 4));
        Verdict verdict = engine.decide(GUID, state, NOW).get(0);
        assertThat(verdict.status()).isNotEqualTo(VerdictStatus.DIFF);
        assertThat(verdict.stats().mainDuplicates()).isEqualTo(1);
    }

    @Test
    void diffCarriesSignatureAndFieldDiffs() {
        MatchEngine engine = engine(MatchPolicy.CohortMode.ASSUME_ALL, false);
        GuidState state = fold(engine,
                out(Env.MAIN, "{\"total\": 10, \"state\": \"done\"}", 1),
                out(Env.LOAD, "{\"total\": 12, \"state\": \"done\"}", 2));
        assertThat(engine.decide(GUID, state, NOW)).singleElement().satisfies(v -> {
            assertThat(v.status()).isEqualTo(VerdictStatus.DIFF);
            assertThat(v.severity()).isEqualTo(Severity.ERROR);
            assertThat(v.signature().entries()).containsExactly("total: value-changed");
            assertThat(v.fieldDiffs()).hasSize(1);
        });
    }

    @Test
    void missingInLoadWithinCohortIsAnError() {
        MatchEngine engine = engine(MatchPolicy.CohortMode.ENTRY_TOPICS, false);
        GuidState state = fold(engine,
                entry(Env.MAIN, 1), entry(Env.LOAD, 2),
                out(Env.MAIN, "{\"v\": 1}", 3));
        assertThat(engine.decide(GUID, state, NOW)).singleElement().satisfies(v -> {
            assertThat(v.status()).isEqualTo(VerdictStatus.MISSING_IN_LOAD);
            assertThat(v.severity()).isEqualTo(Severity.ERROR);
        });
    }

    @Test
    void extraInLoadIsAWarning() {
        MatchEngine engine = engine(MatchPolicy.CohortMode.ENTRY_TOPICS, false);
        GuidState state = fold(engine,
                entry(Env.MAIN, 1), entry(Env.LOAD, 2),
                out(Env.LOAD, "{\"v\": 1}", 3));
        assertThat(engine.decide(GUID, state, NOW).get(0).status()).isEqualTo(VerdictStatus.EXTRA_IN_LOAD);
    }

    @Test
    void loadOnlyGuidIsTestTrafficNotExtra() {
        MatchEngine engine = engine(MatchPolicy.CohortMode.ENTRY_TOPICS, false);
        GuidState state = fold(engine,
                entry(Env.LOAD, 1),
                out(Env.LOAD, "{\"v\": 1}", 2), out(Env.LOAD, "{\"v\": 2}", 3));
        assertThat(engine.decide(GUID, state, NOW)).singleElement().satisfies(v -> {
            assertThat(v.status()).isEqualTo(VerdictStatus.TEST_TRAFFIC);
            assertThat(v.topic()).isEqualTo(Verdict.COHORT_TOPIC);
            assertThat(v.severity()).isEqualTo(Severity.INFO);
            assertThat(v.stats().loadMessages()).isEqualTo(2);
        });
    }

    @Test
    void mainOnlyGuidIsACoverageGapNotAFailure() {
        MatchEngine engine = engine(MatchPolicy.CohortMode.ENTRY_TOPICS, false);
        GuidState state = fold(engine,
                entry(Env.MAIN, 1),
                out(Env.MAIN, "{\"v\": 1}", 2));
        assertThat(engine.decide(GUID, state, NOW)).singleElement().satisfies(v -> {
            assertThat(v.status()).isEqualTo(VerdictStatus.NOT_MIRRORED);
            assertThat(v.severity()).isEqualTo(Severity.INFO);
        });
    }

    @Test
    void outputsWithoutAnyEntryAreUnanchored() {
        MatchEngine engine = engine(MatchPolicy.CohortMode.ENTRY_TOPICS, false);
        GuidState state = fold(engine,
                out(Env.MAIN, "{\"v\": 1}", 1), out(Env.LOAD, "{\"v\": 1}", 2));
        assertThat(engine.decide(GUID, state, NOW)).singleElement().satisfies(v -> {
            assertThat(v.status()).isEqualTo(VerdictStatus.UNANCHORED);
            assertThat(v.severity()).isEqualTo(Severity.WARN);
        });
    }

    @Test
    void assumeAllJudgesWithoutEntryTopics() {
        MatchEngine engine = engine(MatchPolicy.CohortMode.ASSUME_ALL, false);
        GuidState state = fold(engine,
                out(Env.MAIN, "{\"v\": 1}", 1), out(Env.LOAD, "{\"v\": 1}", 2));
        assertThat(engine.decide(GUID, state, NOW).get(0).status()).isEqualTo(VerdictStatus.EQUAL);
    }

    @Test
    void decisionTimeFollowsQuietPeriodThenHardCap() {
        MatchEngine engine = engine(MatchPolicy.CohortMode.ASSUME_ALL, false);
        GuidState state = fold(engine, out(Env.MAIN, "{\"v\": 1}", 100));
        assertThat(engine.nextDecisionTime(state)).isEqualTo(1_100);
        engine.onObservation(state, out(Env.MAIN, "{\"v\": 2}", 9_800));
        assertThat(engine.nextDecisionTime(state)).isEqualTo(10_100); // capped by maxWait
    }

    @Test
    void lateArrivalReDecidesWithHigherRevision() {
        MatchEngine engine = engine(MatchPolicy.CohortMode.ENTRY_TOPICS, false);
        GuidState state = fold(engine,
                entry(Env.MAIN, 1), entry(Env.LOAD, 2),
                out(Env.MAIN, "{\"v\": 1}", 3));
        Verdict first = engine.decide(GUID, state, NOW).get(0);
        assertThat(first.status()).isEqualTo(VerdictStatus.MISSING_IN_LOAD);
        assertThat(first.revision()).isEqualTo(1);

        engine.onObservation(state, out(Env.LOAD, "{\"v\": 1}", 5));
        Verdict second = engine.decide(GUID, state, NOW + 1).get(0);
        assertThat(second.status()).isEqualTo(VerdictStatus.EQUAL);
        assertThat(second.revision()).isEqualTo(2);
    }
}
