#!/usr/bin/env bash
set +e
BASE=http://localhost:5000
EMAIL="diag-$(date -u +%Y%m%d%H%M%S)-$$@example.test"

echo "=== register ==="
curl -sS -m 15 -w '\nHTTP=%{http_code}\n' -H 'Content-Type: application/json' \
  -d "{\"firstname\":\"Diag\",\"lastname\":\"Runner\",\"email\":\"$EMAIL\",\"password\":\"E2E-password-123\",\"confirmPassword\":\"E2E-password-123\"}" \
  "$BASE/api/auth/register"

USER_ID=$(docker exec mongo mongosh --quiet PAYMENT_GATEWAY --eval "db.users.findOne({email:'$EMAIL'},{_id:1})?._id?.toString() || ''")
ROLE_ID=$(docker exec mongo mongosh --quiet PAYMENT_GATEWAY --eval "db.roles.findOne({roleName:'ROLE_ADMIN',deletedAt:null},{_id:1})?._id?.toString() || ''")
docker exec mongo mongosh --quiet PAYMENT_GATEWAY --eval 'db.user_roles.updateOne({userId:ObjectId("'"$USER_ID"'"),roleId:ObjectId("'"$ROLE_ID"'")},{$setOnInsert:{userId:ObjectId("'"$USER_ID"'"),roleId:ObjectId("'"$ROLE_ID"'")}},{upsert:true})'  >/dev/null
echo "USER_ID=$USER_ID"

echo "=== login ==="
TOKEN_JSON=$(curl -sS -m 15 -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"E2E-password-123\"}" \
  "$BASE/api/auth/login")
echo "$TOKEN_JSON" | head -c 300; echo
TOKEN=$(echo "$TOKEN_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])" 2>/dev/null)
echo "TOKEN_LEN=${#TOKEN}"

echo "=== create card ==="
CARD_JSON=$(curl -sS -m 15 -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"userId\":\"$USER_ID\",\"cardType\":\"VISA\",\"expireDate\":\"2035-12-31\",\"cvv\":\"123\",\"cardProvider\":\"BCA\"}" \
  "$BASE/api/cards")
echo "$CARD_JSON" | head -c 300; echo
CARD_NUM=$(echo "$CARD_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['cardNumber'])" 2>/dev/null)
echo "CARD_NUM=$CARD_NUM"

echo "=== create saldo ==="
curl -sS -m 15 -w '\nHTTP=%{http_code}\n' -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"cardNumber\":\"$CARD_NUM\",\"totalBalance\":1000000}" "$BASE/api/saldos"

echo "=== FAILING 1: saldos/stats/balance/monthly ==="
curl -sS -m 15 -w '\nHTTP=%{http_code}\n' -H "Authorization: Bearer $TOKEN" "$BASE/api/saldos/stats/balance/monthly?year=2026"

echo "=== FAILING 2: cards/stats/topup/monthly/by-card ==="
curl -sS -m 15 -w '\nHTTP=%{http_code}\n' -H "Authorization: Bearer $TOKEN" "$BASE/api/cards/stats/topup/monthly/by-card?year=2035&cardNumber=$CARD_NUM"

echo "=== FAILING 3: cards/stats/balance/monthly (stats.hurl line ~104) ==="
curl -sS -m 15 -w '\nHTTP=%{http_code}\n' -H "Authorization: Bearer $TOKEN" "$BASE/api/cards/stats/balance/monthly?year=2026"

echo "=== logs saldo ==="
docker logs --since 3m saldo 2>&1 | grep -Ei 'error|exception' | tail -6
echo "=== logs card ==="
docker logs --since 3m card 2>&1 | grep -Ei 'error|exception' | tail -6
echo "=== logs gateway ==="
docker logs --since 3m gateway 2>&1 | grep -Ei 'error|exception|400' | tail -8

docker exec mongo mongosh --quiet PAYMENT_GATEWAY --eval 'db.users.deleteOne({_id:ObjectId("'"$USER_ID"'")})'  >/dev/null 2>&1 || true
