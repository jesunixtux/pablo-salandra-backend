#!/bin/sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
HTTPD=/usr/sbin/httpd
APACHE_CONF=/private/tmp/pablo-salandra-phpmyadmin.conf

"$ROOT/start-mysql.sh"

if ! curl -fsS http://127.0.0.1:8081/ >/dev/null 2>&1; then
    nohup "$ROOT/start-phpmyadmin.sh" </dev/null >"$ROOT/logs/phpmyadmin.log" 2>&1 &
    echo "$!" >"$ROOT/data/phpmyadmin.pid"
    for attempt in $(seq 1 20); do
        if curl -fsS http://127.0.0.1:8081/ >/dev/null 2>&1; then break; fi
        sleep 1
    done
    if ! curl -fsS http://127.0.0.1:8081/ >/dev/null 2>&1; then
        echo "phpMyAdmin no inició; revisa $ROOT/logs/phpmyadmin.log" >&2
        exit 1
    fi
fi

if ! curl -fsS http://127.0.0.1/phpmyadmin/ >/dev/null 2>&1; then
    cat >"$APACHE_CONF" <<'EOF'
ServerName localhost
<Location />
    Require ip 127.0.0.1 ::1
</Location>
RedirectMatch 301 ^/phpmyadmin$ /phpmyadmin/
ProxyPass /phpmyadmin/ http://127.0.0.1:8081/
ProxyPassReverse /phpmyadmin/ http://127.0.0.1:8081/
EOF
    sudo "$HTTPD" -f /etc/apache2/httpd.conf \
        -C 'LoadModule proxy_module libexec/apache2/mod_proxy.so' \
        -C 'LoadModule proxy_http_module libexec/apache2/mod_proxy_http.so' \
        -c 'PidFile /private/tmp/pablo-salandra-phpmyadmin-httpd.pid' \
        -c "Include $APACHE_CONF" -k start
fi

echo "Listo: http://localhost/phpmyadmin/ (MySQL 127.0.0.1:3306)."
