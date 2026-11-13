#!/bin/sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
HTTPD=/usr/sbin/httpd
APACHE_CONF=/private/tmp/pablo-salandra-phpmyadmin.conf

if [ -f /private/tmp/pablo-salandra-phpmyadmin-httpd.pid ]; then
    if [ ! -f "$APACHE_CONF" ]; then
        cat >"$APACHE_CONF" <<'EOF'
ServerName localhost
<Location />
    Require ip 127.0.0.1 ::1
</Location>
RedirectMatch 301 ^/phpmyadmin$ /phpmyadmin/
ProxyPass /phpmyadmin/ http://127.0.0.1:8081/
ProxyPassReverse /phpmyadmin/ http://127.0.0.1:8081/
EOF
    fi
    if sudo "$HTTPD" -f /etc/apache2/httpd.conf \
        -C 'LoadModule proxy_module libexec/apache2/mod_proxy.so' \
        -C 'LoadModule proxy_http_module libexec/apache2/mod_proxy_http.so' \
        -c 'PidFile /private/tmp/pablo-salandra-phpmyadmin-httpd.pid' \
        -c "Include $APACHE_CONF" -k stop; then
        :
        rm -f "$APACHE_CONF"
    else
        echo "No se pudo detener el Apache local; continúo deteniendo MySQL." >&2
    fi
fi

if [ -f "$ROOT/data/phpmyadmin.pid" ]; then
    PID="$(cat "$ROOT/data/phpmyadmin.pid")"
    if ps -p "$PID" -o command= | grep -F "$ROOT/runtime/php/bin/php" | grep -Fq '127.0.0.1:8081'; then
        kill "$PID"
    fi
    rm -f "$ROOT/data/phpmyadmin.pid"
fi

if "$ROOT/runtime/mysql/bin/mysqladmin" --protocol=TCP --host=127.0.0.1 --port=3306 --user=root ping --silent >/dev/null 2>&1; then
    "$ROOT/stop-mysql.sh"
else
    echo "MySQL ya está detenido."
fi
