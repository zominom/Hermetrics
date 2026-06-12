# hermetrics

An always-on parity checker for Kafka pipelines: it compares the messages your
**load** environment (candidate version) produces against **main** (production
version) and reports semantic regressions — while staying quiet about
environmental noise, duplicate deliveries, upsert churn, and injected test data.

## How it judges

1. **Cohort anchoring** — entry topics are tapped in both environments; only
   document GUIDs that entered *both* pipelines are judged. GUIDs seen only in
   main are a mirroring coverage gap (`NOT_MIRRORED`, informational); GUIDs seen
   only in load are your injected test traffic (`TEST_TRAFFIC`, excluded from parity).
2. **Canonical normalization** — each payload is decoded (JSON, XML, pluggable)
   into one canonical tree and rewritten by per-topic rules: ignore noise fields,
   mask PII, sort order-insensitive arrays, with `null` ≡ absent by default.
3. **State comparison, not message comparison** — per (topic, GUID), messages fold
   into a timeline of distinct states. Exact re-sends count as duplicates and do
   not move the final state. The verdict compares **final states** (with numeric/
   time tolerances), and flags `EQUAL_DIVERGED` when finals match but the
   intermediate paths differed (e.g. coalesced updates).
4. **Signature aggregation** — every diff reduces to its shape (generalized paths +
   change kinds), so 40k documents differing the same way roll up into one finding
   with counts and sample GUIDs.

Verdict statuses: `EQUAL`, `EQUAL_DIVERGED`, `DIFF`, `MISSING_IN_LOAD`,
`EXTRA_IN_LOAD`, `NOT_MIRRORED`, `TEST_TRAFFIC`, `UNANCHORED`.

## Build & run

```bash
mvn package
flink run target/hermetrics-1.0-SNAPSHOT.jar /path/to/job-config.json
```

See `job-config.example.json` for the full configuration: the environments, the
control topic, sinks, and the `compare` section (policy, rule sets, topics).
Topics listed in `compare.topics` must exist in both environments. Both example
files are load-tested (`ExampleConfigsTest`), so they cannot drift from the loader.

An environment is one or more Kafka clusters. A pipeline that spans several
Kafkas lists them under `clusters`, each naming the topics it carries — every
compared topic must be assigned to exactly one cluster per environment:

```json
"main": {
  "clusters": [
    {"name": "ingest", "bootstrapServers": "main-ingest-kafka:9092",
     "topics": ["pipeline.input"]},
    {"name": "core", "bootstrapServers": "main-core-kafka:9092",
     "topics": ["orders.enriched", "legacy.export"]}
  ]
}
```

A single-cluster environment can use the flat shorthand instead
(`"load": {"bootstrapServers": "load-kafka:9092"}`) — it carries all topics.
Each cluster has its own `groupId`, `topicPrefix`/`topicOverrides`,
`startingOffsets`, and Kafka client `properties` (auth etc.).

For a first local run, `job-config.local.json` points both environments at
`localhost:9092` with `main.` / `load.` topic prefixes, logs verdicts instead of
producing them, and decides 5 seconds after a GUID goes quiet:

```bash
for t in main.orders load.orders hermetrics.control; do
  kafka-topics.sh --bootstrap-server localhost:9092 --create --topic $t
done
flink run target/hermetrics-1.0-SNAPSHOT.jar job-config.local.json

echo '{"guid":"g1","total":10,"traceId":"a"}' | kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --topic main.orders
echo '{"guid":"g1","total":10,"traceId":"b"}' | kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --topic load.orders
```

Five seconds later the task manager log shows `g1|orders` with status `EQUAL`
(the differing trace ids are ignored by rule); change `total` on one side and
the next verdict is a `DIFF` naming the path and both values.

Verdicts land on the results topic as JSON, keyed by `guid|topic` with increasing
revisions — compact the topic and the latest verdict wins. Optional rollups emit
windowed counts per (topic, status, diff signature).

## Updating rules without redeploying

The job broadcasts a control topic. Publish:

```json
{"type": "compare-config", "compare": { ...same shape as the compare section... }}
```

A valid message replaces the active rules/policy on the fly (new ignore rule,
tolerance change, expected-diff suppression). Invalid messages are logged and
ignored. Note: the set of consumed topics and their formats is fixed at job start.

## Calibration (do this before trusting it)

While load still runs the **same version** as production, let the comparator run.
Every diff signature it reports is by definition environment noise — turn those
into ignore/tolerance rules via the control topic until the stream is quiet.
Then deploy the release candidate to load: new signatures are the release's diffs.

## Extension points

Everything that can vary sits behind an interface, assembled in one composition
root: `Plugins` (flink package). Implement `Plugins` — or start from
`Plugins.defaults()` — and pass it to `HermetricsJob.build` to swap parts
without touching core:

- **Payload formats** — `PayloadDecoder`, registered in `DecoderRegistry`.
- **Rule types** — `TreeRewriteRule` / `EquivalenceRule` via a `RuleFactory`,
  registered in `RuleTypeRegistry` (rule paths support `a.b`, `items[]`,
  `items[2]`, `*`, `**`, and `\.` escapes).
- **Diff algorithm** — `Differ` (`StructuralDiffer` compares arrays positionally).
- **Results format** — `FindingCodec` (`JsonFindingCodec` emits the verdict,
  rollup, and dead-letter JSON).
- **Sinks** — `FindingSinkFactory`, registered in `SinkRegistry`
  (`kafka` and `logging` ship built in).

The comparison core (`canonical`, `decode`, `rules`, `diff`, `match`,
`pipeline`, `config`, `report` packages) has no Flink dependency and is fully
unit-testable; the `flink` package and its `config`/`operator`/`sink`
subpackages only wire sources, keyed state, timers, broadcast config, and sinks
around it.
