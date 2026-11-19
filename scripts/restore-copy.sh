#!/bin/sh
set -eu
EXAM_PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$EXAM_PROJECT_DIR"
[ "$#" -eq 2 ] || { echo 'Использование: ./scripts/restore-copy.sh backup.dump имя_базы_restore' >&2; exit 1; }
EXAM_RESTORE_FILE=$1
EXAM_RESTORE_DB=$2
[ -f "$EXAM_RESTORE_FILE" ] || { echo 'Файл резервной копии не найден.' >&2; exit 1; }
case "$EXAM_RESTORE_DB" in *[!a-zA-Z0-9_]*) echo 'В имени БД разрешены только латиница, цифры и _.' >&2; exit 1;; esac
case "$EXAM_RESTORE_DB" in *_restore) ;; *) echo 'Для новой копии используйте имя с суффиксом _restore.' >&2; exit 1;; esac
. ./.env
# createdb завершится ошибкой, если такое имя уже занято; существующая БД не изменяется.
docker compose exec -T postgres createdb -U "$EXAM_DB_USER" "$EXAM_RESTORE_DB"
docker compose exec -T postgres pg_restore --exit-on-error --no-owner --no-acl -U "$EXAM_DB_USER" -d "$EXAM_RESTORE_DB" < "$EXAM_RESTORE_FILE"
echo "Копия восстановлена в $EXAM_RESTORE_DB; рабочая БД не изменена."
echo "Для просмотра запустите отдельный backend: EXAM_DB_URL=jdbc:postgresql://127.0.0.1:$EXAM_DB_PORT/$EXAM_RESTORE_DB EXAM_SERVER_PORT=8083 ./scripts/backend-local.sh"
echo "Затем в другом терминале: EXAM_API_URL=http://127.0.0.1:8083 ./run.sh"
