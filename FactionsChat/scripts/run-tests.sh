#!/usr/bin/env bash
# Run FactionsChat unit tests against Paper MockBukkit lines locally.
# Usage:
#   ./scripts/run-tests.sh           # both
#   ./scripts/run-tests.sh 26
#   ./scripts/run-tests.sh 1.21

set -euo pipefail
cd "$(dirname "$0")/.."

run_line() {
  local line="$1"
  echo ""
  echo "=== FactionsChat tests (paperTestLine=${line}) ==="
  mvn -q test "-DpaperTestLine=${line}"
}

case "${1:-all}" in
  26) run_line 26 ;;
  1.21) run_line 1.21 ;;
  all|"")
    run_line 26
    run_line 1.21
    ;;
  *)
    echo "Usage: $0 [all|26|1.21]" >&2
    exit 2
    ;;
esac

echo ""
echo "All requested FactionsChat test runs passed."
