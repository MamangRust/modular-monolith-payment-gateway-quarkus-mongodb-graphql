#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
COMPOSE=(docker compose --env-file "$ROOT_DIR/deployments/local/docker.env" -f "$ROOT_DIR/deployments/local/docker-compose.yml")
BASE_URL="${BASE_URL:-http://localhost:5000}"
E2E_EMAIL="${E2E_EMAIL:-clc-$(date -u +%Y%m%d%H%M%S)-$$@example.test}"

wait_for_gateway() { for _ in $(seq 1 "${E2E_WAIT_ATTEMPTS:-60}"); do curl --silent --fail --max-time 3 "$BASE_URL/q/health/ready" >/dev/null && return 0; sleep 2; done; exit 1; }
cleanup() { local id; id="$(docker exec mongo mongosh --quiet PAYMENT_GATEWAY --eval "db.users.findOne({email:'$E2E_EMAIL'},{_id:1})?._id?.toString() || ''" 2>/dev/null || true)"; [[ -z "$id" ]] || docker exec mongo mongosh --quiet PAYMENT_GATEWAY --eval 'db.users.deleteOne({_id:ObjectId("'"$id"'")})'  >/dev/null 2>&1 || true; }
trap cleanup EXIT
wait_for_gateway
BODY=/tmp/payment-gateway-clc-register.json
STATUS="$(curl --silent --show-error --max-time 15 -o "$BODY" -w '%{http_code}' -H 'Content-Type: application/json' -d "{\"firstname\":\"CLC\",\"lastname\":\"Runner\",\"email\":\"$E2E_EMAIL\",\"password\":\"E2E-password-123\",\"confirmPassword\":\"E2E-password-123\"}" "$BASE_URL/api/auth/register")"
[[ "$STATUS" == 201 ]] || { cat "$BODY" >&2; exit 1; }
USER_ID="$(docker exec mongo mongosh --quiet PAYMENT_GATEWAY --eval "db.users.findOne({email:'$E2E_EMAIL'},{_id:1})?._id?.toString() || ''")"
ROLE_ID="$(docker exec mongo mongosh --quiet PAYMENT_GATEWAY --eval "db.roles.findOne({roleName:'ROLE_ADMIN',deletedAt:null},{_id:1})?._id?.toString() || ''")"
docker exec mongo mongosh --quiet PAYMENT_GATEWAY --eval 'db.user_roles.updateOne({userId:ObjectId("'"$USER_ID"'"),roleId:ObjectId("'"$ROLE_ID"'")},{$setOnInsert:{userId:ObjectId("'"$USER_ID"'"),roleId:ObjectId("'"$ROLE_ID"'")}},{upsert:true})'  >/dev/null
hurl --test --variable base_url="$BASE_URL" --variable e2e_email="$E2E_EMAIL" --variable user_id="$USER_ID" "$ROOT_DIR/deployments/local/tests/credit-lifecycle.hurl"
echo "Credit lifecycle smoke passed for $E2E_EMAIL"
