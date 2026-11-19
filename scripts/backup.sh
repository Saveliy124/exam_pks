#!/bin/sh
set -eu
EXAM_PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$EXAM_PROJECT_DIR"
. ./.env
umask 077
mkdir -p backups
EXAM_BACKUP_FILE="backups/${EXAM_DB_NAME}-$(date +%Y%m%d-%H%M%S).dump"
if docker compose exec -T postgres pg_dump -U "$EXAM_DB_USER" -d "$EXAM_DB_NAME" --format=custom > "$EXAM_BACKUP_FILE"; then
  echo "Резервная копия: $EXAM_BACKUP_FILE"
else
  rm -f "$EXAM_BACKUP_FILE"
  echo 'Резервная копия не создана.' >&2
  exit 1
fi
