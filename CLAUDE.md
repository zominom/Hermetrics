# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

hermetrics is an always-on Flink job that compares Kafka messages between two
environments — **main** (production version) and **load** (candidate version) —
keyed by a document GUID, and emits parity verdicts. See README.md for the
judging model (cohort anchoring, canonical normalization, state-based comparison,
signature aggregation) and the calibration workflow.

## Build and test

There is **no system Maven** on this machine — use IntelliJ's bundled one:

```bash
MVN="/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn"
"$MVN" test                                            # full suite
"$MVN" test -Dtest=MatchEngineTest                     # one class
"$MVN" test -Dtest=MatchEngineTest#diffCarriesSignatureAndFieldDiffs  # one method
"$MVN" package                                         # shaded deployment jar
```

Java 21, Flink 2.1.0 (provided scope), flink-connector-kafka 5.0.0-2.1. The jar
runs with `flink run target/hermetrics-1.0-SNAPSHOT.jar <job-config.json>`
(see job-config.example.json); there is no real-Kafka integration test — Kafka
paths are verified only by unit tests plus `PipelineGraphTest`.
`dependency-reduced-pom.xml` is a shade-plugin artifact, not a source file.

## Architecture

**Layering invariant: the core never imports Flink.** The core packages
(`canonical.*`, `decode.*`, `rules.*`, `diff.*`, `match.*`, `pipeline`,
`config`, `report.*`) are pure JVM and unit-tested directly; only `flink` and
its subpackages touch Flink. New comparison logic belongs in the core with
tests; the Flink layer stays a thin shell of sources/state/timers/sinks.

**Package map** (one top-level type per file; nested types only for trivial
discriminators like `Observation.Kind`, `TopicConfig.Role`, `Path.Segment`):

```
canonical/value   sealed CanonicalValue + 6 variants   (move as a unit — sealed)
canonical/json    CanonicalJsonWriter, CanonicalJsonReader, ContentHasher
canonical/path    Path, PathPattern, PathPatternParser
decode            PayloadDecoder (SPI), DecoderRegistry, RawMessage, DecodeException
decode/format     JsonPayloadDecoder, XmlPayloadDecoder
rules             NormalizationRule, TreeRewriteRule, EquivalenceRule, RuleSet, Normalizer
rules/loader      RuleFactory, RuleSetLoader, RuleTypeRegistry
rules/builtin     Ignore/Mask/Unordered/NumberTolerance/TimeTolerance rules
diff              Differ (SPI), FieldDiff, DiffSignature
diff/algorithm    StructuralDiffer
match             MatchEngine, MatchPolicy, Env, Observation
match/state       GuidState, TopicPair, Timeline, StateVersion
match/verdict     Verdict, VerdictStatus, Severity, VerdictStats
pipeline          ObservationFactory, FieldExtractor
config            CompareConfig, TopicConfig, ConfigLoader
report            FindingCodec (SPI), DeadLetter, Rollup
report/codec      JsonFindingCodec
flink             HermetricsJob, Topology, Plugins, DefaultPlugins, ControlChannel
flink/source      KafkaSources, EnvRecordDeserializer
flink/record      RawEnvRecord, KeyedRecord, VerdictSummary
flink/config      JobConfig, EnvConfig, ClusterConfig, ControlConfig, SinkConfig, RollupConfig, JobConfigLoader
flink/operator    NormalizeFunction, DecideFunction, Rollup{Aggregator,Accumulator,Emitter}
flink/sink        FindingSinkFactory, SinkRegistry, KafkaSinkFactory, Logging{Sink,SinkFactory}
```

**Layout convention — every extension axis follows one shape: SPI + DTOs at the
package root, implementations in a subpackage** (`decode/format`, `rules/builtin`,
`diff/algorithm`, `report/codec`, `flink/sink`). Adding a format/rule/differ/
codec/sink = drop a file in the obvious subpackage and register it in `Plugins`.
The job entry is split by responsibility: `HermetricsJob` (main), `Topology`
(stream graph), `KafkaSources` (source builders).

**Sealed-type constraint:** `CanonicalValue` and its 6 permitted variants live
together in `canonical/value` — Java's unnamed module requires permitted
subtypes in the same package, so they always move as a unit.

Data flow: bytes → `PayloadDecoder` → `CanonicalValue` tree → GUID extraction →
`Normalizer` (rule rewrites) → canonical JSON string + content hash
(`Observation`) → keyBy(guid) → `MatchEngine` folds `GuidState` → timers fire →
`Verdict`s → `FindingCodec` → sinks. Two configs exist: the job config
(clusters, per-cluster topic prefix/overrides, sinks, TTL) and the embedded
`compare` section (policy, ruleSets, topics) — the latter is also what the
control topic hot-swaps at runtime via
`{"type": "compare-config", "compare": {...}}` (validated before activation;
consumed topics and formats are fixed at job start regardless).

An environment (`EnvConfig`) is a list of `ClusterConfig`s — pipelines span
multiple Kafka clusters, each carrying a subset of the compared topics. The
loader enforces that every compare topic is assigned to exactly one cluster per
environment; the flat single-cluster JSON shorthand maps to one cluster carrying
all topics. `KafkaSources` builds one source per (environment, cluster) and
unions them, with uids derived from the cluster `name` — renaming a cluster
breaks source-state recovery from checkpoints.

**Wire-safety invariant:** anything crossing a shuffle or living in keyed state
(`Observation`, `GuidState`) carries only strings, primitives, enums and plain
JDK collections — canonical *JSON strings*, never `CanonicalValue` trees. The
engine re-parses states at decide time. State classes use public non-final
fields deliberately (Flink POJO/Kryo rules). If you touch these shapes, keep
them flat; `PipelineGraphTest` forces type extraction over the whole topology
and is the early-warning test for breakage here.

## Deliberate semantics (not bugs)

- Update comparison has two modes per topic. With `sequencePath` (string or
  array → composite key, extracted pre-normalization like the GUID and required
  on every message): states are paired by sequence and each pair diffed —
  `EQUAL` needs every pair equal; final state = max sequence (numeric-aware,
  event-time tiebreak); same sequence + same hash = duplicate, same sequence +
  new content = latest event time wins; pair mismatches with equal finals =
  `EQUAL_DIVERGED` **with** diffs and missing/extra sequence counts in stats.
  Without `sequencePath`: a re-seen content hash is a duplicate delivery that
  does not move the final state (an A→B→A re-send compares as final B), finals
  are last-by-arrival, and only the last unsequenced payload is retained in
  state (earlier `StateVersion.stateJson` are nulled); sequenced topics retain
  every version's payload to allow pair diffs — that's the memory trade-off.
  Do not `ignore` the sequence field in rules, or a lagging load looks EQUAL.
- GUID extraction runs on the **pre-normalization** tree so ignore rules cannot
  remove the matching key.
- Equivalence rules (tolerances) run **at diff time only** and cannot affect
  content hashes; anything hash-relevant must be a `TreeRewrite` rule.
  `Unordered` is post-order so it sorts already-normalized elements.
- A topic's named rule set **replaces** the default; there is no merging.
- XML decodes to strings only (no numeric coercion); root element name is kept;
  a repeated tag becomes an array but a single one doesn't — a known mapping quirk.
- Cohort logic: GUIDs entering only main → `NOT_MIRRORED` (coverage gap, info);
  only load → `TEST_TRAFFIC` (injected test data, excluded from parity).
  `EQUAL_DIVERGED` = same final state via different intermediate states.
- Verdicts form an **upsert stream** keyed `guid|topic`: late arrivals re-decide
  and emit higher revisions. Timers are processing-time (quiet period capped by
  maxWait); only the newest registered timer is honored via `nextFire` state.

## Extension points (keep them open)

The user explicitly wants OCP/DIP: every variation point is an interface wired
through the `Plugins` composition root (flink package) — payload formats
(`PayloadDecoder` + `DecoderRegistry`), rule types (`RuleFactory` +
`RuleTypeRegistry`), diff algorithm (`Differ`), results format
(`FindingCodec`), sinks (`FindingSinkFactory` + `SinkRegistry`). Operators take
`Plugins` in their constructors and build collaborators from it; never
hard-construct a registry or loader inside an operator. The results-topic JSON
emitted by `JsonFindingCodec` (types `verdict`, `rollup`, `dead-letter`) is a
consumer-facing contract — change it additively.

## Code style (user requirement)

No comments anywhere — the code must explain itself through names and small
extracted methods. Conceptual documentation lives here and in README.md;
behavioral documentation lives in tests. Constructor-inject collaborators.
No Spring: Flink serializes operator closures to task managers, so DI containers
don't travel — `Plugins` plus `HermetricsJob.main` is the composition root.

## Test conventions

Build canonical trees from JSON literals via `Trees.json(...)`
(src/test/.../testutil). Match-engine scenarios construct `Observation`s
directly and drive `MatchEngine` without Flink.
