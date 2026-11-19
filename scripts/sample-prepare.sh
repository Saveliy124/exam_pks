#!/bin/sh
set -eu
EXAM_PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$EXAM_PROJECT_DIR"
EXAM_SAMPLE_DIR=student-hub-suite-rebuilt
[ -d "$EXAM_SAMPLE_DIR/backend" ] && [ -d "$EXAM_SAMPLE_DIR/javafx-client" ] && [ -d "$EXAM_SAMPLE_DIR/android-client" ] || {
  echo 'Распакуйте учебный архив целиком в student-hub-suite-rebuilt рядом с pom.xml.' >&2; exit 1;
}
if [ ! -f "$EXAM_SAMPLE_DIR/.env" ]; then
  umask 077
  [ -f "$EXAM_SAMPLE_DIR/backend/.env" ] || { echo 'В учебном архиве не найден .env.' >&2; exit 1; }
  sed 's/^POSTGRES_PORT=.*/POSTGRES_PORT=55434/' "$EXAM_SAMPLE_DIR/backend/.env" > "$EXAM_SAMPLE_DIR/.env"
fi
