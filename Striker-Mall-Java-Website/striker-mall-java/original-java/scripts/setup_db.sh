#!/usr/bin/env bash
# Creates the emall database and loads sample data. Usage: scripts/setup_db.sh [mysql-user]
cd "$(dirname "$0")/.."
U=${1:-root}
mysql -u "$U" -p < database/schema.sql && mysql -u "$U" -p < database/seed.sql && echo "Database ready."
