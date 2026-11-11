#!/usr/bin/env bash
set -euo pipefail

output="${1:-ZlaKocka.jar}"
mkdir -p "$(dirname "$output")"
output="$(cd "$(dirname "$output")" && pwd)/$(basename "$output")"
cd "$(dirname "$0")"

scratch="$(mktemp -d)"
trap 'rm -rf "$scratch"' EXIT
javac --release 17 -encoding UTF-8 -d "$scratch" src/*.java
jar --create --file "$output" --main-class ZlaKocka -C "$scratch" .
