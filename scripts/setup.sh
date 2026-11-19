#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$PROJECT_DIR"
command -v docker >/dev/null 2>&1 || { echo 'Установите и запустите Docker Desktop.' >&2; exit 1; }
docker info >/dev/null 2>&1 || { echo 'Запустите Docker Desktop и повторите команду.' >&2; exit 1; }
if [ ! -f .env ]; then
  umask 077
  EXAM_SETUP_PASSWORD=$(openssl rand -hex 24)
  cat > .env <<EOF
EXAM_DB_NAME=exam_center
EXAM_DB_USER=exam_center
EXAM_DB_PASSWORD=$EXAM_SETUP_PASSWORD
EXAM_DB_PORT=55433
EOF
fi
set -a
. ./.env
set +a
umask 077
mkdir -p config
cat > config/database.properties <<EOF
db.url=jdbc:postgresql://127.0.0.1:$EXAM_DB_PORT/$EXAM_DB_NAME
db.user=$EXAM_DB_USER
db.password=$EXAM_DB_PASSWORD
EOF
docker compose --env-file .env up -d --wait postgres
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "$EXAM_DB_USER" -d "$EXAM_DB_NAME" < sql/schema.sql
# Seed only a genuinely empty database; never replace existing KR1 data.
EXAM_EXISTING=$(docker compose exec -T postgres psql -At -U "$EXAM_DB_USER" -d "$EXAM_DB_NAME" -c 'SELECT (SELECT count(*) FROM applicants)+(SELECT count(*) FROM examiners)+(SELECT count(*) FROM exam_applications)')
if [ "$EXAM_EXISTING" = 0 ]; then
  docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "$EXAM_DB_USER" -d "$EXAM_DB_NAME" < sql/seed.sql
fi
 . ./scripts/java21.sh
mvn -B -ntp -DskipTests install
docker compose up -d --build --wait backend
printf 'api.url=http://127.0.0.1:%s\n' "${EXAM_API_PORT:-8081}" > config/client.properties
echo "PostgreSQL и Spring Boot готовы. API: http://127.0.0.1:${EXAM_API_PORT:-8081}. Запуск: ./run.sh"
