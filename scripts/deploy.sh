#!/usr/bin/env bash

set -euo pipefail

APP_DIR="${APP_DIR:-$(cd "$(dirname "$0")/.." && pwd)}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"

if [[ -z "${APP_IMAGE:-}" ]]; then
  echo "APP_IMAGE 환경변수가 필요합니다."
  exit 1
fi

cd "${APP_DIR}"

if [[ ! -f "${COMPOSE_FILE}" ]]; then
  echo "Compose 파일을 찾을 수 없습니다: ${COMPOSE_FILE}"
  exit 1
fi

if [[ ! -f ".env" ]]; then
  echo "${APP_DIR} 경로에 .env 파일이 없습니다. 서버에 운영용 .env 파일을 먼저 생성해주세요."
  exit 1
fi

if [[ -n "${GHCR_USERNAME:-}" && -n "${GHCR_TOKEN:-}" ]]; then
  echo "${GHCR_TOKEN}" | docker login ghcr.io -u "${GHCR_USERNAME}" --password-stdin
fi

export APP_IMAGE

docker compose -f "${COMPOSE_FILE}" pull app
docker compose -f "${COMPOSE_FILE}" up -d postgres redis
docker compose -f "${COMPOSE_FILE}" up -d app

for _ in {1..30}; do
  if curl -fsS http://127.0.0.1:8080/actuator/health >/dev/null; then
    echo "애플리케이션이 정상적으로 실행 중입니다."
    docker compose -f "${COMPOSE_FILE}" ps
    exit 0
  fi
  sleep 2
done

echo "애플리케이션 헬스체크에 실패했습니다."
docker compose -f "${COMPOSE_FILE}" ps
exit 1
