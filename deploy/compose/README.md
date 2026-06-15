# Run the full stack locally

One command brings up everything — Kafka, the Flink job (JobManager + TaskManager
in Application Mode), the control-plane API, and the UI — plus a tiny seed so the
dashboard isn't empty.

## Quickest path

From the repo root:

- **Windows:** double-click `run.bat` (stop with `stop.bat`)
- **macOS / Linux:** `./run.sh`
- **Any OS, directly:**
  ```bash
  docker compose -f deploy/compose/docker-compose.yml up --build -d
  ```

Requires Docker Desktop (or Docker Engine + Compose v2) running. First run builds
three images and can take a few minutes.

Then open:

| | URL |
|---|---|
| UI | http://localhost:5173 |
| API docs (Swagger) | http://localhost:8080/docs |
| Flink dashboard | http://localhost:8081 |

## What it does

`kafka-setup` creates the topics and seeds four messages on the `orders` pipeline:
`g1` is identical in main and load (→ **EQUAL**), `g2` differs (`total` 20 vs 25 →
**DIFF**); the differing `traceId` is ignored by a rule. With `quietMillis: 5000`
and `cohortMode: ASSUME_ALL`, verdicts appear within ~10s, so the dashboard shows
one EQUAL and one DIFF, and the **orders** topic page shows the breakdown.

Produce more to watch it live:

```bash
docker exec -it hermetrics-kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server kafka:9092 --topic load.orders
> {"guid":"g3","total":99,"traceId":"x"}
```

Edit rules/policy from the UI's **Config** tab (Apply publishes to the control
topic and hot-swaps the running job).

## Files

- `docker-compose.yml` — the stack (builds from `Dockerfile`, `Dockerfile.api`, `ui/Dockerfile`).
- `job-config.json` — demo job config (both envs on one Kafka via `main.`/`load.` prefixes, short timers).
- `api-config.json` — demo API config (reads findings from the Kafka tail, `earliest`).

Stop and remove: `docker compose -f deploy/compose/docker-compose.yml down`.
