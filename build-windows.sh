#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 2 ]; then
    echo "Použití: $0 <adresář s Windows x64 JRE 25> <výstupní adresář>" >&2
    exit 2
fi

runtime="$(cd "$1" && pwd)"
if [ ! -f "$runtime/bin/javaw.exe" ]; then
    echo "Ve složce runtime chybí bin/javaw.exe." >&2
    exit 2
fi
mkdir -p "$2"
output="$(cd "$2" && pwd)"
if [ -e "$output/ZlaKocka" ]; then
    echo "Zvolte výstupní adresář, který ještě neobsahuje složku ZlaKocka." >&2
    exit 2
fi

cd "$(dirname "$0")"
scratch="$(mktemp -d)"
trap 'rm -rf "$scratch"' EXIT
mkdir -p "$scratch/classes" "$scratch/ZlaKocka/app"
javac --release 24 -encoding UTF-8 -d "$scratch/classes" src/*.java
jar --create --file "$scratch/ZlaKocka/app/ZlaKocka.jar" --main-class ZlaKocka -C "$scratch/classes" .
java -Djava.awt.headless=true -cp "$scratch/classes" CatIcon "$scratch/cat.ico"
cp windows/launcher.rc "$scratch/"
(
    cd "$scratch"
    zig rc /fo launcher.res launcher.rc
)
zig cc -target x86_64-windows-gnu -municode -Wl,--subsystem,windows -O2 -s -Wall -Wextra \
    windows/launcher.c "$scratch/launcher.res" -o "$scratch/ZlaKocka/ZlaKocka.exe"
cp -R "$runtime" "$scratch/ZlaKocka/runtime"
cp README.md "$scratch/ZlaKocka/"
mv "$scratch/ZlaKocka" "$output/"
