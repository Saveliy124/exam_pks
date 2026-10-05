#!/bin/sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PG_BIN=/opt/homebrew/opt/postgresql@16/bin
DB_DIR="$PROJECT_DIR/.local-db"
DB_PORT=55432

if [ ! -f "$DB_DIR/PG_VERSION" ]; then
    "$PG_BIN/initdb" -D "$DB_DIR" -U postgres -A trust --encoding=UTF8 --locale=en_US.UTF-8
fi

if ! "$PG_BIN/pg_ctl" -D "$DB_DIR" status >/dev/null 2>&1; then
    "$PG_BIN/pg_ctl" -D "$DB_DIR" -l "$DB_DIR/server.log" \
        -o "-p $DB_PORT -k /private/tmp -h 127.0.0.1" start
fi

DB_EXISTS=$("$PG_BIN/psql" -h /private/tmp -p "$DB_PORT" -U postgres -d postgres -tA \
    -c "SELECT 1 FROM pg_database WHERE datname='exam_center'")
if [ "$DB_EXISTS" != 1 ]; then
    "$PG_BIN/createdb" -h /private/tmp -p "$DB_PORT" -U postgres exam_center
fi

# Схема обновляет и старую базу. Учебные записи добавляются один раз.
"$PG_BIN/psql" -h /private/tmp -p "$DB_PORT" -U postgres -d exam_center \
    -v ON_ERROR_STOP=1 -q -f "$PROJECT_DIR/sql/schema.sql"
if [ ! -f "$DB_DIR/.seed-v2" ]; then
    "$PG_BIN/psql" -h /private/tmp -p "$DB_PORT" -U postgres -d exam_center \
        -v ON_ERROR_STOP=1 -q -f "$PROJECT_DIR/sql/seed.sql"
    touch "$DB_DIR/.seed-v2"
fi

export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
export EXAM_DB_URL="jdbc:postgresql://127.0.0.1:$DB_PORT/exam_center"
export EXAM_DB_USER=postgres
export EXAM_DB_PASSWORD=

cd "$PROJECT_DIR"
exec /opt/homebrew/bin/mvn -Dmaven.repo.local="$PROJECT_DIR/.maven-cache" compile exec:java
