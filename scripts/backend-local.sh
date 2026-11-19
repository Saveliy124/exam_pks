#!/bin/sh
set -eu
EXAM_PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$EXAM_PROJECT_DIR"
. ./scripts/java21.sh
set -a
. ./.env
set +a
export TZ=Europe/Moscow
export EXAM_DB_URL="${EXAM_DB_URL:-jdbc:postgresql://127.0.0.1:${EXAM_DB_PORT}/${EXAM_DB_NAME}}"
export EXAM_SERVER_PORT="${EXAM_SERVER_PORT:-8083}"
mvn -B -ntp -DskipTests package
exec java -Duser.timezone=Europe/Moscow -jar backend/target/exam-backend-3.0.0-exec.jar
