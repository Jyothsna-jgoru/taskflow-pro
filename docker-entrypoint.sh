#!/bin/sh
set -eu

# Render provides Postgres as postgresql://user:password@host:port/database.
# Spring/Hikari expects a JDBC URL, while the credentials are supplied separately
# through DB_USERNAME and DB_PASSWORD.
case "${DB_URL:-}" in
  postgresql://*|postgres://*)
    connection_without_scheme="${DB_URL#*://}"
    connection_without_credentials="${connection_without_scheme#*@}"
    export DB_URL="jdbc:postgresql://${connection_without_credentials}"
    ;;
esac

exec java -XX:MaxRAMPercentage=75 -jar /app/app.jar
