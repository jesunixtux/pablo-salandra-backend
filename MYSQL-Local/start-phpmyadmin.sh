#!/bin/sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
mkdir -p "$ROOT/data/php-sessions"
exec "$ROOT/runtime/php/bin/php" -n \
    -d date.timezone=America/Santiago \
    -d session.save_path="$ROOT/data/php-sessions" \
    -S 127.0.0.1:8081 \
    -t "$ROOT/runtime/phpmyadmin"
