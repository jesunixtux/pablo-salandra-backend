#!/bin/sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
"$ROOT/runtime/mysql/bin/mysqladmin" --protocol=TCP --host=127.0.0.1 --port=3306 --user=root shutdown
echo "MySQL se detuvo."
