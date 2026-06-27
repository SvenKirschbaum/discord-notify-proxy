#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 1 ]]; then
  printf 'usage: %s <image-name>\n' "$0" >&2
  exit 64
fi

IMAGE_NAME="$1"
CONTAINER_NAME="discord-notify-proxy-smoke"
HOST_PORT="18080"
APP_DISCORD_TOKEN="${APP_DISCORD_TOKEN:-}"

if [[ -z "$APP_DISCORD_TOKEN" ]]; then
  printf 'APP_DISCORD_TOKEN must be set\n' >&2
  exit 64
fi

cleanup() {
  docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
}

log() {
  printf '[image-smoke] %s\n' "$1"
}

wait_for_ready() {
  local attempt
  local response

  log "Waiting for readiness endpoint"

  for attempt in {1..90}; do
    response="$(curl --silent --fail "http://127.0.0.1:${HOST_PORT}/actuator/health/readiness" || true)"
    if [[ "$response" == *'"status":"UP"'* ]]; then
      log "Container is ready"
      return 0
    fi
    sleep 2
  done

  printf 'container did not become ready in time\n' >&2
  docker logs "$CONTAINER_NAME" >&2 || true
  return 1
}

trap cleanup EXIT
cleanup

log "Starting container ${IMAGE_NAME}"
docker run --detach \
  --name "$CONTAINER_NAME" \
  --publish "${HOST_PORT}:8080" \
  --env "APP_DISCORD_TOKEN=${APP_DISCORD_TOKEN}" \
  "$IMAGE_NAME" >/dev/null

wait_for_ready

body_file="$(mktemp)"
log "Sending webhook request"
status_code="$(curl \
  --silent \
  --show-error \
  --output "$body_file" \
  --write-out '%{http_code}' \
  --header 'Content-Type: application/json' \
  --request POST \
  --data '{"notificationType":"MEDIA_AVAILABLE","event":"Your request is now available","subject":"Image smoke test","message":"Validate buildpack image startup and request handling."}' \
  "http://127.0.0.1:${HOST_PORT}/webhooks/seerr")"

body="$(<"$body_file")"
rm -f "$body_file"

if [[ "$status_code" != '202' ]]; then
  printf 'unexpected status code: %s\nresponse body: %s\n' "$status_code" "$body" >&2
  docker logs "$CONTAINER_NAME" >&2 || true
  exit 1
fi

if [[ "$body" != *'"status":"IGNORED"'* || "$body" != *'"recipientCount":0'* || "$body" != *'No Discord recipients'* ]]; then
  printf 'unexpected response body: %s\n' "$body" >&2
  docker logs "$CONTAINER_NAME" >&2 || true
  exit 1
fi

log "Smoke test passed"
