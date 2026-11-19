#!/bin/sh
set -eu
EXAM_PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$EXAM_PROJECT_DIR"
. ./scripts/java21.sh
export TZ=Europe/Moscow
# Client configuration is only an HTTP URL; never load the database password here.
if [ -z "${EXAM_API_URL:-}" ] && [ -f config/client.properties ]; then
  EXAM_API_URL=$(sed -n 's/^api.url=//p' config/client.properties)
  export EXAM_API_URL
fi
mvn -B -ntp -DskipTests -pl common,desktop -am install
exec mvn -f desktop/pom.xml javafx:run "$@"
