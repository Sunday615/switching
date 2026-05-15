#!/bin/bash
# Creates least-privilege DB users for the switching app.
# Runs once on first MySQL container start (docker-entrypoint-initdb.d).
# Env vars injected from docker-compose mysql service environment block.
set -e

mysql -u root -p"${MYSQL_ROOT_PASSWORD}" <<-EOSQL
    -- App user: DML only — cannot ALTER or DROP schema
    CREATE USER IF NOT EXISTS 'switching_app'@'%'
        IDENTIFIED BY '${DB_APP_PASSWORD:-switching_app_password_change_me}';
    GRANT SELECT, INSERT, UPDATE, DELETE
        ON switching_db.*
        TO 'switching_app'@'%';

    -- Flyway user: needs DDL to run schema migrations
    CREATE USER IF NOT EXISTS 'switching_flyway'@'%'
        IDENTIFIED BY '${FLYWAY_PASSWORD:-switching_flyway_password_change_me}';
    GRANT ALL PRIVILEGES
        ON switching_db.*
        TO 'switching_flyway'@'%';

    FLUSH PRIVILEGES;
EOSQL

echo "[init-db-users] Created users: switching_app (DML only), switching_flyway (DDL for migrations)"
