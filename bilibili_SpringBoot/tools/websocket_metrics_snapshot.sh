#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${1:-${BASE_URL:-http://127.0.0.1:8080}}"

METRICS=(
  "im.ws.sessions.active"
  "im.ws.users.online"
  "im.ws.heartbeat.received"
  "im.ws.heartbeat.ack.sent"
  "im.ws.heartbeat.ack.failed"
)

fetch_metric() {
  local metric_name="$1"
  local metric_url="${BASE_URL%/}/actuator/metrics/${metric_name}"

  echo "===== ${metric_name} ====="
  curl -sS "${metric_url}"
  echo
  echo
}

echo "WebSocket metrics snapshot"
echo "base_url=${BASE_URL}"
echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S %Z')"
echo

for metric in "${METRICS[@]}"; do
  fetch_metric "${metric}"
done
