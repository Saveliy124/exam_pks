#!/bin/sh
set -eu
EXAM_PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$EXAM_PROJECT_DIR"
[ -f .env ] || { echo 'Сначала выполните ./scripts/setup.sh' >&2; exit 1; }
set -a
. ./.env
set +a
EXAM_DEMO_NAME="${EXAM_DB_NAME}_demo"
docker compose up -d --wait postgres
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "$EXAM_DB_USER" -d postgres -v demo_db="$EXAM_DEMO_NAME" <<'SQL'
SELECT format('CREATE DATABASE %I', :'demo_db') WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname=:'demo_db')\gexec
SQL
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "$EXAM_DB_USER" -d "$EXAM_DEMO_NAME" < sql/schema.sql
EXAM_DEMO_COUNT=$(docker compose exec -T postgres psql -At -U "$EXAM_DB_USER" -d "$EXAM_DEMO_NAME" -c 'SELECT (SELECT count(*) FROM applicants)+(SELECT count(*) FROM examiners)+(SELECT count(*) FROM exam_applications)')
if [ "$EXAM_DEMO_COUNT" = 0 ]; then
  docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "$EXAM_DB_USER" -d "$EXAM_DEMO_NAME" < sql/demo.sql
fi
docker compose --profile demo up -d --build --wait backend-demo
export EXAM_API_URL="http://127.0.0.1:${EXAM_DEMO_API_PORT:-8082}"
export EXAM_APP_MODE=demo
if [ "${1:-}" = --prepare-only ]; then
  echo "Демонстрационный backend готов: $EXAM_API_URL"
  exit 0
fi
unset EXAM_DB_URL EXAM_DB_USER EXAM_DB_PASSWORD EXAM_TEST_DB_URL
exec ./run.sh
