#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose --env-file .env -f web-backend/docker-compose.yml down
