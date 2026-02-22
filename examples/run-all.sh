#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
EXAMPLES_DIR="$SCRIPT_DIR/src/main/java/io/seqera/zvec/examples"

for file in "$EXAMPLES_DIR"/*.java; do
  example=$(basename "$file" .java)
  echo "--- Running $example ---"
  ./gradlew :examples:run -PmainClass=io.seqera.zvec.examples.$example "$@"
  echo ""
done

echo "All examples completed successfully."
