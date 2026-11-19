#!/bin/sh
set -eu
EXAM_PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
"$EXAM_PROJECT_DIR/scripts/sample-prepare.sh"
cd "$EXAM_PROJECT_DIR/student-hub-suite-rebuilt"
# connection-info штатно завершается; в --wait запускаются только долгоживущие сервисы.
docker compose --env-file .env -f backend/docker-compose.yml -f ../docker/sample-override.yaml up -d --wait db backend
curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health
printf '\nПример: http://localhost:8080/\n'
