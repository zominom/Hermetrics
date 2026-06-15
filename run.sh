#!/usr/bin/env bash
# macOS / Linux equivalent of run.bat — brings up the full stack via docker compose.
set -e
cd "$(dirname "$0")"

echo "Starting the full hermetrics stack (Kafka + Flink job + API + UI)..."
docker compose -f deploy/compose/docker-compose.yml up --build -d

echo
echo "Starting. Allow ~30-60s for Flink + the seed to flow."
echo "  UI       : http://localhost:5173"
echo "  API docs : http://localhost:8080/docs"
echo "  Flink UI : http://localhost:8081"
echo "Stop with: docker compose -f deploy/compose/docker-compose.yml down"

( command -v open >/dev/null && sleep 6 && open http://localhost:5173 ) >/dev/null 2>&1 &
