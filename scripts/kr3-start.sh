#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
. scripts/java21.sh
python3 scripts/kr3-env.py
docker info >/dev/null
KR3_COMPOSE=(docker compose --env-file .env -f web-backend/docker-compose.yml)
"${KR3_COMPOSE[@]}" up -d --wait postgres
# Existing KR1/KR2 data are copied only into an entirely empty KR3 database.
# The original container and its volume are never modified.
KR3_EMPTY=$("${KR3_COMPOSE[@]}" exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc "select count(*) from information_schema.tables where table_schema=chr(112)||chr(117)||chr(98)||chr(108)||chr(105)||chr(99)"')
if [ "$KR3_EMPTY" = 0 ] && docker inspect exam-center-kr2-postgres-1 >/dev/null 2>&1; then
  mkdir -p backups/kr3
  docker exec exam-center-kr2-postgres-1 sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --no-privileges -t applicants -t examiners -t exam_applications' > backups/kr3/kr1-for-kr3.sql
  "${KR3_COMPOSE[@]}" exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < backups/kr3/kr1-for-kr3.sql
  echo 'Existing KR1/KR2 rows copied into the separate KR3 database.'
fi
mvn -pl web-backend -am package -DskipTests
"${KR3_COMPOSE[@]}" up -d --build --wait
echo 'KR3: http://localhost:8080/  Health: http://localhost:8080/actuator/health'
echo 'Login and temporary password are in .env; replace the password on first login.'
