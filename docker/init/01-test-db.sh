#!/bin/sh
set -eu
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -v testdb="${POSTGRES_DB}_test" <<'SQL'
CREATE DATABASE :"testdb";
SQL
