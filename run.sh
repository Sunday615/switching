#!/usr/bin/env bash
set -euo pipefail

# ──────────────────────────────────────────────
# run.sh — สคริปต์รัน Switching Application
# ──────────────────────────────────────────────
# Usage:
#   ./run.sh                → รัน local (ต้อง MySQL ขึ้นแล้ว)
#   ./run.sh docker         → รัน full stack (MySQL + App) ด้วย Docker Compose
#   ./run.sh docker:db      → รัน MySQL อย่างเดียว (App รัน local)
#   ./run.sh docker:build   → build Docker image เท่านั้น (ไม่รัน)
#   ./run.sh docker:rebuild → force rebuild ทุก layer แล้ว start
#   ./run.sh test           → รัน tests ทั้งหมด (Testcontainers — ไม่ต้องการ MySQL)
#   ./run.sh test:unit      → รัน unit tests อย่างเดียว (เร็วมาก ไม่ต้องการ DB)
#   ./run.sh test:single    → รัน test class เดียว (CLASS=ClassName ./run.sh test:single)
#   ./run.sh stop           → หยุด Docker containers ทั้งหมด
#   ./run.sh logs           → ดู log ของ app container
#   ./run.sh status         → แสดงสถานะ containers
# ──────────────────────────────────────────────

ENV_FILE=".env"

load_env() {
  if [[ -f "$ENV_FILE" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
  else
    echo "⚠️  ไม่พบ .env — copy จาก .env.example ก่อน:"
    echo "    cp .env.example .env"
    exit 1
  fi
}

is_port_busy() {
  local port="$1"
  lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
}

first_free_port() {
  local port="${1:-8080}"
  while is_port_busy "$port"; do
    port=$((port + 1))
  done
  echo "$port"
}

cmd="${1:-local}"

case "$cmd" in

  local)
    load_env
    SERVER_PORT="${SERVER_PORT:-$(first_free_port 8080)}"
    export SERVER_PORT
    echo "▶ Start local (Spring Boot)"
    echo "  DB_URL     : ${DB_URL:-jdbc:mysql://localhost:3306/switching_db?useSSL=false&serverTimezone=Asia/Bangkok&allowPublicKeyRetrieval=true}"
    echo "  DB_USERNAME: ${DB_USERNAME:-root}"
    echo "  SERVER_PORT: ${SERVER_PORT}"
    echo ""
    ./mvnw spring-boot:run \
      -Dspring-boot.run.jvmArguments="-Xms256m -Xmx512m"
    ;;

  docker)
    load_env
    echo "▶ Start full stack (Docker Compose)"
    docker compose up --build -d
    echo ""
    echo "✅ App running at http://localhost:${SERVER_PORT:-8080}"
    echo "   Health : http://localhost:${SERVER_PORT:-8080}/actuator/health"
    echo ""
    echo "   ดู log: ./run.sh logs"
    ;;

  docker:db)
    load_env
    echo "▶ Start MySQL only"
    docker compose up -d mysql
    echo "✅ MySQL พร้อมที่ localhost:3306"
    ;;

  docker:build)
    load_env
    echo "▶ Build Docker image (ไม่รัน)"
    docker compose build app
    echo "✅ Build เสร็จ — รัน: ./run.sh docker"
    ;;

  docker:rebuild)
    load_env
    echo "▶ Force rebuild ทุก layer แล้ว start"
    docker compose up --build --force-recreate -d
    echo ""
    echo "✅ Rebuild เสร็จ — App running at http://localhost:${SERVER_PORT:-8080}"
    echo "   ดู log: ./run.sh logs"
    ;;

  test)
    echo "▶ Run all tests (Testcontainers — ไม่ต้องการ MySQL)"
    ./mvnw clean test
    ;;

  test:unit)
    echo "▶ Run unit tests only (fast)"
    ./mvnw test \
      -Dtest="ParticipantManagementServiceTest,RoutingRuleManagementServiceTest,ConnectorConfigManagementServiceTest" \
      -Dsurefire.failIfNoSpecifiedTests=false
    ;;

  test:single)
    CLASS="${CLASS:-FullTransferFlowIntegrationTest}"
    echo "▶ Run test class: $CLASS"
    ./mvnw test -Dtest="$CLASS" -Dsurefire.failIfNoSpecifiedTests=false
    ;;

  stop)
    echo "▶ Stop all Docker containers"
    docker compose down
    echo "✅ Stopped"
    ;;

  logs)
    docker compose logs -f app
    ;;

  status)
    echo "▶ Container status"
    docker compose ps
    ;;

  *)
    echo "Usage: ./run.sh [local|docker|docker:db|docker:build|docker:rebuild|test|test:unit|test:single|stop|logs|status]"
    exit 1
    ;;

esac
