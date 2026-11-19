#!/bin/sh
set -eu
EXAM_PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$EXAM_PROJECT_DIR/student-hub-suite-rebuilt"
docker compose --env-file .env -f backend/docker-compose.yml -f ../docker/sample-override.yaml down
