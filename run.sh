#!/usr/bin/env bash
# ==============================================================================
# AICTE Approval & Compliance Orchestration Platform (PS 26262)
# Master Service Runner: Database -> Backend -> Frontend
# ==============================================================================

set -eo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

# Ensure runtime paths and environment
export JAVA_HOME="${JAVA_HOME:-/home/Harshit/.local/share/junie/versions/3419.22/junie-app/lib/runtime}"
export PATH="$HOME/.local/bin:$JAVA_HOME/bin:$PATH"
export LD_LIBRARY_PATH="$HOME/.local/lib:${LD_LIBRARY_PATH}"
export PGDATA="${PGDATA:-$HOME/.local/share/aicte_postgres_data}"

# Ensure .env exists
if [ ! -f "$PROJECT_ROOT/.env" ]; then
  if [ -f "$PROJECT_ROOT/.env.example" ]; then
    echo -e "⚙️ Creating .env from .env.example..."
    sed 's/your_secure_db_password_here/aicte_123/' "$PROJECT_ROOT/.env.example" > "$PROJECT_ROOT/.env"
  fi
fi

# Load local environment variables if .env exists
if [ -f "$PROJECT_ROOT/.env" ]; then
  # shellcheck source=/dev/null
  set -a
  source "$PROJECT_ROOT/.env"
  set +a
fi

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-aicte_app_user}"
DB_NAME="${DB_NAME:-aicte_platform_db}"
SERVER_PORT="${SERVER_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"

# Colors
C_RESET='\033[0m'
C_BOLD='\033[1m'
C_GREEN='\033[32m'
C_BLUE='\033[34m'
C_YELLOW='\033[33m'
C_CYAN='\033[36m'
C_RED='\033[31m'

echo -e "${C_BOLD}${C_BLUE}==============================================================================${C_RESET}"
echo -e "${C_BOLD}${C_CYAN}    AICTE Approval & Compliance Orchestration Platform (SIH 26262)${C_RESET}"
echo -e "${C_BOLD}${C_BLUE}==============================================================================${C_RESET}"

# PID tracking for clean teardown
BACKEND_PID=""
FRONTEND_PID=""

cleanup() {
  echo ""
  echo -e "${C_YELLOW}>>> Initiating graceful shutdown of all platform services...${C_RESET}"
  if [ -n "$FRONTEND_PID" ] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
    echo -e "Stopping Frontend (PID: $FRONTEND_PID)..."
    kill "$FRONTEND_PID" 2>/dev/null || true
  fi
  if [ -n "$BACKEND_PID" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
    echo -e "Stopping Backend (PID: $BACKEND_PID)..."
    kill "$BACKEND_PID" 2>/dev/null || true
  fi
  echo -e "${C_GREEN}>>> All services stopped cleanly.${C_RESET}"
  exit 0
}

trap cleanup SIGINT SIGTERM EXIT

# ------------------------------------------------------------------------------
# 1. Start / Verify PostgreSQL Database
# ------------------------------------------------------------------------------
echo -e "\n${C_BOLD}[1/3] Checking PostgreSQL Database (${DB_HOST}:${DB_PORT})...${C_RESET}"

is_db_ready() {
  if command -v pg_isready >/dev/null 2>&1; then
    pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" >/dev/null 2>&1
  else
    # Fallback socket test via bash/nc
    timeout 1 bash -c "cat < /dev/null > /dev/tcp/${DB_HOST}/${DB_PORT}" 2>/dev/null
  fi
}

if is_db_ready; then
  echo -e "${C_GREEN}✓ PostgreSQL is active and accepting connections on port ${DB_PORT}.${C_RESET}"
else
  echo -e "${C_YELLOW}! PostgreSQL is not currently running. Attempting startup...${C_RESET}"

  if command -v docker >/dev/null 2>&1 && docker ps >/dev/null 2>&1; then
    echo -e "Starting PostgreSQL container via docker compose..."
    docker compose up -d postgres || true
  elif [ -d "$PGDATA" ] && command -v pg_ctl >/dev/null 2>&1; then
    echo -e "Starting local PostgreSQL server from $PGDATA..."
    pg_ctl -D "$PGDATA" -l "$PGDATA/server.log" start || true
  elif command -v docker-compose >/dev/null 2>&1 && docker ps >/dev/null 2>&1; then
    echo -e "Starting PostgreSQL container via docker-compose..."
    docker-compose up -d postgres || true
  elif command -v systemctl >/dev/null 2>&1 && sudo -n true 2>/dev/null; then
    echo -e "Starting system PostgreSQL service..."
    sudo systemctl start postgresql 2>/dev/null || true
  fi

  # Wait for database readiness
  echo -n "Waiting for PostgreSQL to be ready..."
  READY=0
  for _ in {1..30}; do
    if is_db_ready; then
      READY=1
      break
    fi
    echo -n "."
    sleep 1
  done
  echo ""

  if [ "$READY" -eq 1 ]; then
    echo -e "${C_GREEN}✓ PostgreSQL database is now ready.${C_RESET}"
  else
    echo -e "${C_RED}✗ Unable to connect to PostgreSQL at ${DB_HOST}:${DB_PORT}.${C_RESET}"
    echo -e "Please ensure PostgreSQL is started with credentials matching .env or compose.yaml."
    exit 1
  fi
fi

# ------------------------------------------------------------------------------
# 2. Start Spring Boot Backend
# ------------------------------------------------------------------------------
echo -e "\n${C_BOLD}[2/3] Starting Spring Boot Backend API (Port: ${SERVER_PORT})...${C_RESET}"

# Free port 8080 if an old instance is lingering
if command -v fuser >/dev/null 2>&1; then
  fuser -k "${SERVER_PORT}/tcp" 2>/dev/null || true
elif command -v lsof >/dev/null 2>&1; then
  OLD_PID=$(lsof -ti:"${SERVER_PORT}" 2>/dev/null || true)
  if [ -n "$OLD_PID" ]; then
    kill -9 "$OLD_PID" 2>/dev/null || true
  fi
fi

# Launch backend in background
./mvnw spring-boot:run &
BACKEND_PID=$!
echo -e "Spring Boot starting in background (PID: ${BACKEND_PID})..."

# Wait for backend health check
echo -n "Waiting for Backend API to become healthy..."
BACKEND_READY=0
for _ in {1..60}; do
  HEALTH_STATUS=$(curl -s "http://localhost:${SERVER_PORT}/api/v1/health" 2>/dev/null | grep -o '"status":"UP"' || true)
  if [ -n "$HEALTH_STATUS" ]; then
    BACKEND_READY=1
    break
  fi
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
    echo -e "\n${C_RED}✗ Spring Boot process exited unexpectedly. Check logs for details.${C_RESET}"
    exit 1
  fi
  echo -n "."
  sleep 1
done
echo ""

if [ "$BACKEND_READY" -eq 1 ]; then
  echo -e "${C_GREEN}✓ Backend is healthy and running on http://localhost:${SERVER_PORT}${C_RESET}"
else
  echo -e "${C_RED}✗ Backend health check timed out on http://localhost:${SERVER_PORT}/api/v1/health${C_RESET}"
  exit 1
fi

# ------------------------------------------------------------------------------
# 3. Start Vite React Frontend
# ------------------------------------------------------------------------------
echo -e "\n${C_BOLD}[3/3] Starting Vite React Frontend (Port: ${FRONTEND_PORT})...${C_RESET}"

# Free frontend port if lingering
if command -v fuser >/dev/null 2>&1; then
  fuser -k "${FRONTEND_PORT}/tcp" 2>/dev/null || true
fi

cd "$PROJECT_ROOT/frontend"

# Ensure dependencies are installed
if [ ! -d "node_modules" ]; then
  echo -e "Installing frontend npm dependencies..."
  npm install
fi

# Start frontend development server in background
npm run dev -- --host 0.0.0.0 --port "${FRONTEND_PORT}" &
FRONTEND_PID=$!
cd "$PROJECT_ROOT"

echo -e "Frontend starting in background (PID: ${FRONTEND_PID})..."

# Wait for frontend port
echo -n "Waiting for Frontend to respond..."
FRONTEND_READY=0
for _ in {1..20}; do
  if curl -s -I "http://localhost:${FRONTEND_PORT}" 2>/dev/null | grep -q "HTTP/"; then
    FRONTEND_READY=1
    break
  fi
  if ! kill -0 "$FRONTEND_PID" 2>/dev/null; then
    echo -e "\n${C_RED}✗ Frontend process exited unexpectedly.${C_RESET}"
    exit 1
  fi
  echo -n "."
  sleep 1
done
echo ""

if [ "$FRONTEND_READY" -eq 1 ]; then
  echo -e "${C_GREEN}✓ Frontend is accessible on http://localhost:${FRONTEND_PORT}${C_RESET}"
fi

# ------------------------------------------------------------------------------
# Platform Ready Summary
# ------------------------------------------------------------------------------
echo -e "\n${C_BOLD}${C_GREEN}==============================================================================${C_RESET}"
echo -e "${C_BOLD}${C_GREEN}    🚀 AICTE Approval & Compliance Platform is FULLY OPERATIONAL${C_RESET}"
echo -e "${C_BOLD}${C_GREEN}==============================================================================${C_RESET}"
echo -e "${C_BOLD}• Frontend UI:${C_RESET}        ${C_CYAN}http://localhost:${FRONTEND_PORT}${C_RESET}"
echo -e "${C_BOLD}• Backend REST API:${C_RESET}   ${C_CYAN}http://localhost:${SERVER_PORT}${C_RESET}"
echo -e "${C_BOLD}• Swagger OpenAPI UI:${C_RESET} ${C_CYAN}http://localhost:${SERVER_PORT}/swagger-ui.html${C_RESET}"
echo -e "${C_BOLD}• Health Endpoint:${C_RESET}    ${C_CYAN}http://localhost:${SERVER_PORT}/api/v1/health${C_RESET}"
echo -e "${C_BOLD}• PostgreSQL DB:${C_RESET}      ${C_CYAN}localhost:${DB_PORT}/${DB_NAME}${C_RESET} (User: ${DB_USER})"
echo -e "------------------------------------------------------------------------------"
echo -e "${C_YELLOW}Press Ctrl+C at any time to shut down all services gracefully.${C_RESET}"
echo -e "------------------------------------------------------------------------------\n"

# Keep the script running and streaming service output until Ctrl+C
wait "$BACKEND_PID" "$FRONTEND_PID"
