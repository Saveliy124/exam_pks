#!/bin/bash
set -e
cd "$(dirname "$0")"
bash scripts/kr3-start.sh
open http://localhost:8080/
