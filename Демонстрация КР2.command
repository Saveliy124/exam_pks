#!/bin/sh
set -eu
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"
EXAM_PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$EXAM_PROJECT_DIR"
./scripts/setup.sh
exec ./scripts/demo.sh
