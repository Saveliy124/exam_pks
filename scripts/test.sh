#!/bin/sh
set -eu
EXAM_PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$EXAM_PROJECT_DIR"
. ./scripts/java21.sh
set -a
. ./.env
set +a
export TZ=Europe/Moscow
export EXAM_TEST_DB_URL="jdbc:postgresql://127.0.0.1:${EXAM_DB_PORT}/${EXAM_DB_NAME}_test"
docker compose up -d --wait postgres
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "$EXAM_DB_USER" -d postgres -v test_db="${EXAM_DB_NAME}_test" <<'SQL'
SELECT format('CREATE DATABASE %I', :'test_db') WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname=:'test_db')\gexec
SQL
exec mvn -B -ntp verify -Pintegration "$@"
