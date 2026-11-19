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
mvn -B -ntp -DskipTests install
docker compose build backend
EXAM_UI_CONTAINER="exam-center-ui-test-$$"
EXAM_POSTGRES_ID=$(docker compose ps -q postgres)
EXAM_UI_NETWORK=$(docker inspect --format '{{range $key,$value := .NetworkSettings.Networks}}{{$key}}{{end}}' "$EXAM_POSTGRES_ID")
cleanup() {
  docker logs "$EXAM_UI_CONTAINER" > backend/target/docker-ui-backend.log 2>&1 || true
  docker rm -f "$EXAM_UI_CONTAINER" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM
export EXAM_DB_URL="jdbc:postgresql://postgres:5432/${EXAM_DB_NAME}_test"
docker run -d --rm --name "$EXAM_UI_CONTAINER" --network "$EXAM_UI_NETWORK" -p 127.0.0.1::8081 --env EXAM_DB_URL --env EXAM_DB_USER --env EXAM_DB_PASSWORD exam-center-backend:3.0.0 >/dev/null
EXAM_UI_ADDRESS=$(docker port "$EXAM_UI_CONTAINER" 8081/tcp)
export EXAM_TEST_API_URL="http://${EXAM_UI_ADDRESS}"
EXAM_UI_READY=0
for EXAM_UI_ATTEMPT in $(seq 1 30); do
  if curl --fail --silent "$EXAM_TEST_API_URL/actuator/health" >/dev/null; then EXAM_UI_READY=1; break; fi
  sleep 1
done
[ "$EXAM_UI_READY" = 1 ] || { docker logs "$EXAM_UI_CONTAINER"; exit 1; }
mvn -B -ntp verify -Pui-test
