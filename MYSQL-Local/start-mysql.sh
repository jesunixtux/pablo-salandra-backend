#!/bin/sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
MYSQL_HOME="$ROOT/runtime/mysql"
DATA="$ROOT/data/mysql"
LOG="$ROOT/logs/mysql-error.log"

mkdir -p "$DATA" "$ROOT/logs" "$ROOT/data/tmp"

if [ ! -d "$DATA/mysql" ]; then
    "$MYSQL_HOME/bin/mysqld" --initialize-insecure \
        --basedir="$MYSQL_HOME" \
        --datadir="$DATA" \
        --tmpdir="$ROOT/data/tmp"
fi

if [ -f "$DATA/mysql.pid" ] && ps -p "$(cat "$DATA/mysql.pid")" -o command= | grep -Fq "$MYSQL_HOME/bin/mysqld"; then
    echo "MySQL ya está iniciado (127.0.0.1:3306)."
    exit 0
fi

"$MYSQL_HOME/bin/mysqld" \
    --daemonize \
    --basedir="$MYSQL_HOME" \
    --datadir="$DATA" \
    --socket="/tmp/pablo-salandra-mysql.sock" \
    --pid-file="$DATA/mysql.pid" \
    --log-error="$LOG" \
    --tmpdir="$ROOT/data/tmp" \
    --bind-address=127.0.0.1 \
    --port=3306 \
    --mysqlx=0

for attempt in $(seq 1 30); do
    if "$MYSQL_HOME/bin/mysqladmin" --protocol=TCP --host=127.0.0.1 --port=3306 --user=root ping --silent >/dev/null 2>&1; then
        "$MYSQL_HOME/bin/mysql" --protocol=TCP --host=127.0.0.1 --port=3306 --user=root \
            -e "CREATE DATABASE IF NOT EXISTS bitacora CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
        echo "MySQL está listo en 127.0.0.1:3306. Base creada: bitacora."
        exit 0
    fi
    sleep 1
done

echo "MySQL no inició. Revisa $LOG" >&2
exit 1
