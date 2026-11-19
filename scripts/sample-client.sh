#!/bin/sh
set -eu
EXAM_PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$EXAM_PROJECT_DIR"
. ./scripts/java21.sh
cd student-hub-suite-rebuilt/javafx-client
exec mvn -B -ntp javafx:run
