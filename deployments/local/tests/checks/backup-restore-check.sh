#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
COMPOSE=(docker compose --env-file "$ROOT_DIR/deployments/local/docker.env" -f "$ROOT_DIR/deployments/local/docker-compose.yml")
BACKUP_FILE="${BACKUP_FILE:-}"
BACKUP_CREATED=false
RESTORE_DB="${RESTORE_DB:-PAYMENT_GATEWAY_RESTORE_CHECK}"
RESTORE_CREATED=false

if [[ ! "$RESTORE_DB" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
  echo "RESTORE_DB must be a simple MongoDB database name" >&2
  exit 2
fi
if [[ "${RESTORE_DB,,}" == "payment_gateway" ]]; then
  echo "RESTORE_DB must not be the primary PAYMENT_GATEWAY database" >&2
  exit 2
fi

if [[ -z "$BACKUP_FILE" ]]; then
  BACKUP_FILE="$(mktemp "$ROOT_DIR/deployments/local/payment-gateway-backup.XXXXXX.archive.gz")"
  BACKUP_CREATED=true
fi
TEMP_BACKUP="$(mktemp "${BACKUP_FILE}.tmp.XXXXXX")"

cleanup() {
  if [[ "$RESTORE_CREATED" == "true" ]]; then
    "${COMPOSE[@]}" exec -T mongo mongosh --quiet "$RESTORE_DB" \
      --eval 'db.dropDatabase()' >/dev/null 2>&1 || true
  fi
  rm -f "$TEMP_BACKUP"
  if [[ "$BACKUP_CREATED" == "true" ]]; then
    rm -f "$BACKUP_FILE"
  fi
}
trap cleanup EXIT

if [[ "${ALLOW_BACKUP_RESTORE_CHECK:-false}" != "true" ]]; then
  echo 'Set ALLOW_BACKUP_RESTORE_CHECK=true to run the non-destructive restore check.' >&2
  exit 2
fi

"${COMPOSE[@]}" exec -T mongo mongodump --db=PAYMENT_GATEWAY --archive --gzip > "$TEMP_BACKUP"
[[ -s "$TEMP_BACKUP" ]] || { echo "Backup is empty: $TEMP_BACKUP" >&2; exit 1; }
mv -f "$TEMP_BACKUP" "$BACKUP_FILE"
TEMP_BACKUP="$(mktemp "${BACKUP_FILE}.tmp.XXXXXX")"

"${COMPOSE[@]}" exec -T mongo mongosh --quiet "$RESTORE_DB" --eval 'db.dropDatabase()' >/dev/null
RESTORE_CREATED=true
"${COMPOSE[@]}" exec -T mongo mongorestore --archive --gzip --drop \
  --nsFrom='PAYMENT_GATEWAY.*' --nsTo="$RESTORE_DB.*" < "$BACKUP_FILE" >/dev/null

COLLECTION_COUNT="$("${COMPOSE[@]}" exec -T mongo mongosh --quiet "$RESTORE_DB" \
  --eval 'db.getCollectionNames().length')"
[[ "$COLLECTION_COUNT" =~ ^[0-9]+$ && "$COLLECTION_COUNT" -gt 0 ]] || {
  echo "Restore verification failed; restored collection count=$COLLECTION_COUNT" >&2
  exit 1
}

echo "Backup/restore verification passed without touching the primary database volume."
