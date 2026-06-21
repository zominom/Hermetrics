#!/usr/bin/env bash
set -euo pipefail

: "${LISTEN_PORT:=80}"
: "${API_UPSTREAM:=127.0.0.1:8080}"
: "${API_CONFIG:=/etc/hermetrics/api-config.json}"
export LISTEN_PORT API_UPSTREAM

envsubst '${LISTEN_PORT} ${API_UPSTREAM}' \
    < /etc/nginx/templates/default.conf.template \
    > /etc/nginx/conf.d/default.conf
rm -f /etc/nginx/sites-enabled/default

java -jar /app/app.jar "${API_CONFIG}" &
nginx -g 'daemon off;' &

wait -n
exit $?
