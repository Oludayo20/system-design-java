#!/usr/bin/env bash
#
# Fires many concurrent POST /rides requests and reports latency percentiles. The point of the
# demo: because the producer only inserts a row and publishes to RabbitMQ — never awaits the
# email/analytics/loyalty workers — latency should stay flat whether 1 worker or 20 workers are
# running, and whether 10 or 100,000 rides are in flight.
#
# Bash + curl port of scripts/load-test.ts (see the README's "Kafka/load-test" section for why:
# no Node/JBang runtime is assumed to be present, and curl -w '%{time_total}' gives us
# per-request latency without needing to hand-roll timing).
#
# Usage:
#   ./scripts/load-test.sh
#   LOAD_TEST_REQUESTS=1000 LOAD_TEST_CONCURRENCY=100 ./scripts/load-test.sh
set -euo pipefail

BASE_URL="${LOAD_TEST_BASE_URL:-http://localhost:3000}"
TOTAL_REQUESTS="${LOAD_TEST_REQUESTS:-500}"
CONCURRENCY="${LOAD_TEST_CONCURRENCY:-500}"

echo "[load-test] firing ${TOTAL_REQUESTS} POST /rides requests (concurrency=${CONCURRENCY}) at ${BASE_URL}"

TMP_DIR="$(mktemp -d)"
RESULTS_FILE="${TMP_DIR}/results.txt"
: > "${RESULTS_FILE}"
trap 'rm -rf "${TMP_DIR}"' EXIT

fire_one() {
  local i="$1"
  local driver_id=$(( i % 50 ))
  local fare
  fare=$(awk -v seed="$i" 'BEGIN { srand(seed); printf "%.2f", (rand() * 40) + 5 }')
  local body
  body=$(printf '{"riderId":"rider-%s","driverId":"driver-%s","fare":%s,"pickupLocation":"Ikeja, Lagos","dropoffLocation":"Lekki, Lagos"}' \
    "${i}" "${driver_id}" "${fare}")

  # %{http_code} and %{time_total} (seconds, sub-ms precision) come straight from curl, so we
  # don't need to hand-roll a stopwatch. Appends are short single lines, so concurrent writers
  # interleaving into the same file is safe in practice for a demo script.
  curl -s -o /dev/null -w '%{http_code} %{time_total}\n' -X POST "${BASE_URL}/rides" \
    -H 'Content-Type: application/json' \
    -d "${body}" >> "${RESULTS_FILE}" || echo "000 0" >> "${RESULTS_FILE}"
}
export -f fire_one
export BASE_URL RESULTS_FILE

STARTED_AT_S=$(date +%s)

seq 0 $(( TOTAL_REQUESTS - 1 )) | xargs -P "${CONCURRENCY}" -I{} bash -c 'fire_one "$@"' _ {}

DURATION_MS=$(( ($(date +%s) - STARTED_AT_S) * 1000 ))

COMPLETED=$(wc -l < "${RESULTS_FILE}" | tr -d ' ')
FAILED=$(awk '$1 != "201" { c++ } END { print c + 0 }' "${RESULTS_FILE}")

percentile() {
  local p="$1"
  awk '{ printf "%d\n", ($2 * 1000) }' "${RESULTS_FILE}" | sort -n | awk -v p="${p}" '
    { a[NR] = $1 }
    END {
      idx = int(NR * p)
      if (idx >= NR) idx = NR - 1
      if (idx < 0) idx = 0
      print a[idx + 1] + 0
    }'
}

P50=$(percentile 0.5)
P95=$(percentile 0.95)
P99=$(percentile 0.99)

echo "[load-test] done in ${DURATION_MS}ms — completed=${COMPLETED} failed=${FAILED}"
echo "[load-test] latency p50=${P50}ms p95=${P95}ms p99=${P99}ms"
echo '[load-test] flat latency here is the point: POST /rides only inserts a row + publishes, it never waits on workers.'
