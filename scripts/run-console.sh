#!/bin/sh
set -eu
EXAM_PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$EXAM_PROJECT_DIR"
. ./scripts/java21.sh
set -a
. ./.env
set +a
export EXAM_DB_URL="jdbc:postgresql://127.0.0.1:${EXAM_DB_PORT}/${EXAM_DB_NAME}"
mvn -B -ntp -DskipTests install
exec mvn -f backend/pom.xml org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass=ru.mirea.examcenter.Main
