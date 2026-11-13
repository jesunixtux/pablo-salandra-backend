#!/bin/sh
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
"$ROOT/stop-local.sh"
STATUS=$?
echo
read -r -p "Presiona Enter para cerrar esta ventana... " _
exit "$STATUS"
