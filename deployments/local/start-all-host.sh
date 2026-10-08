#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
LOG_DIR="/tmp/e2e-host-logs"
mkdir -p "$LOG_DIR"

# Kill any existing
pkill -f "quarkus-app" 2>/dev/null || true
pkill -f "quarkus:dev" 2>/dev/null || true
sleep 2
rm -f "$LOG_DIR"/*.log "$LOG_DIR"/*.pid 2>/dev/null

export MONGO_HOST=localhost
export MONGO_PORT=27017
export MONGO_DB=PAYMENT_GATEWAY
export KAFKA_BROKERS=localhost:9092
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export REDIS_HOSTS="redis://:dragon_knight@localhost:6379,redis://:dragon_knight@localhost:6380,redis://:dragon_knight@localhost:6381,redis://:dragon_knight@localhost:6382,redis://:dragon_knight@localhost:6383,redis://:dragon_knight@localhost:6384"
export REDIS_PASSWORD=dragon_knight
export REDIS_CLUSTER_ENABLED=true
export OTEL_ENDPOINT=localhost:4317
export APP_ENV=local

start_svc() {
    local name="$1" jar="$2"; shift 2
    local java_opts=""
    local env_args=()
    for arg in "$@"; do
        if [[ "$arg" == JAVA_OPTS=* ]]; then
            java_opts="${arg#JAVA_OPTS=}"
        else
            env_args+=("$arg")
        fi
    done
    echo "Starting $name..."
    ( export "${env_args[@]}"; exec java $java_opts -jar "$ROOT/$jar" ) >"$LOG_DIR/$name.log" 2>&1 &
    echo $! >"$LOG_DIR/$name.pid"
}

# ── Start per-service with correct DB port/name ──────────────────
start_svc role role/target/quarkus-app/quarkus-run.jar \
    MONGO_HOST=localhost MONGO_PORT=27017 MONGO_DB=PAYMENT_GATEWAY

start_svc user user/target/quarkus-app/quarkus-run.jar \
    MONGO_HOST=localhost MONGO_PORT=27017 MONGO_DB=PAYMENT_GATEWAY ROLE_GRPC_HOST=localhost

start_svc auth auth/target/quarkus-app/quarkus-run.jar \
    MONGO_HOST=localhost MONGO_PORT=27017 MONGO_DB=PAYMENT_GATEWAY \
    USER_GRPC_HOST=localhost ROLE_GRPC_HOST=localhost GRPC_PORT=9012

start_svc card card/target/quarkus-app/quarkus-run.jar \
    MONGO_HOST=localhost MONGO_PORT=27017 MONGO_DB=PAYMENT_GATEWAY

start_svc merchant merchant/target/quarkus-app/quarkus-run.jar \
    MONGO_HOST=localhost MONGO_PORT=27017 MONGO_DB=PAYMENT_GATEWAY

start_svc saldo saldo/target/quarkus-app/quarkus-run.jar \
    MONGO_HOST=localhost MONGO_PORT=27017 MONGO_DB=PAYMENT_GATEWAY

start_svc topup topup/target/quarkus-app/quarkus-run.jar \
    MONGO_HOST=localhost MONGO_PORT=27017 MONGO_DB=PAYMENT_GATEWAY

start_svc transaction transaction/target/quarkus-app/quarkus-run.jar \
    MONGO_HOST=localhost MONGO_PORT=27017 MONGO_DB=PAYMENT_GATEWAY

start_svc transfer transfer/target/quarkus-app/quarkus-run.jar \
    MONGO_HOST=localhost MONGO_PORT=27017 MONGO_DB=PAYMENT_GATEWAY

start_svc withdraw withdraw/target/quarkus-app/quarkus-run.jar \
    MONGO_HOST=localhost MONGO_PORT=27017 MONGO_DB=PAYMENT_GATEWAY

# ── Start gateway last (needs all gRPC backends) ────────────────
start_svc gateway gateway/target/quarkus-app/quarkus-run.jar \
    HTTP_PORT=8080 \
    MONGO_HOST=localhost MONGO_PORT=27017 MONGO_DB=PAYMENT_GATEWAY \
    AUTH_HOST=localhost USER_HOST=localhost CARD_HOST=localhost \
    MERCHANT_HOST=localhost ROLE_HOST=localhost SALDO_HOST=localhost \
    TOPUP_HOST=localhost TRANSACTION_HOST=localhost TRANSFER_HOST=localhost \
    WITHDRAW_HOST=localhost \
    JAVA_OPTS=-Dapp.jwt.enabled=false

echo ""
echo "All 11 services started. Waiting for gateway..."
for i in $(seq 1 90); do
    if curl -sf http://localhost:8080/q/health > /dev/null 2>&1; then
        echo "✅ Gateway ready after ${i}s on http://localhost:8080"
        break
    fi
    if [ $i -eq 90 ]; then
        echo "❌ Gateway not ready after 180s. Check $LOG_DIR/gateway.log"
        tail -5 "$LOG_DIR/gateway.log" 2>/dev/null
    fi
    sleep 2
done

echo ""
echo "=== Service Status ==="
for pidfile in "$LOG_DIR"/*.pid; do
    name=$(basename "$pidfile" .pid)
    pid=$(cat "$pidfile")
    alive="❌"
    kill -0 "$pid" 2>/dev/null && alive="✅"
    printf "%s %-15s PID %s\n" "$alive" "$name" "$pid"
done
