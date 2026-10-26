#!/usr/bin/env bash
# ==============================================================================
# SAGE — System for Approval Governance and Evaluation
# AICTE Approval and Compliance Platform | SIH Problem Statement 26262
# Master Service Runner: Database -> Spring Boot Backend -> Vite React Frontend
# ==============================================================================

set -eo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

# Colors
C_RESET='\033[0m'
C_BOLD='\033[1m'
C_GREEN='\033[32m'
C_BLUE='\033[34m'
C_YELLOW='\033[33m'
C_CYAN='\033[36m'
C_RED='\033[31m'

echo -e "${C_BOLD}${C_BLUE}==============================================================================${C_RESET}"
echo -e "${C_BOLD}${C_CYAN}    SAGE — System for Approval Governance and Evaluation${C_RESET}"
echo -e "${C_BOLD}${C_BLUE}    AICTE Approval & Compliance Platform | SIH Problem Statement 26262${C_RESET}"
echo -e "${C_BOLD}${C_BLUE}==============================================================================${C_RESET}"

# ------------------------------------------------------------------------------
# 0. Locate and Validate Java 21
# ------------------------------------------------------------------------------
find_java21() {
  # 1. If JAVA_HOME is already set, verify it
  if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    local v
    v=$("$JAVA_HOME/bin/java" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$v" = "21" ]; then
      echo "$JAVA_HOME"
      return 0
    fi
  fi

  # 2. If 'java' is on PATH, test if it is version 21
  if command -v java >/dev/null 2>&1; then
    local v
    v=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$v" = "21" ]; then
      local jpath
      jpath=$(command -v java)
      local realj
      realj=$(readlink -f "$jpath")
      dirname "$(dirname "$realj")"
      return 0
    fi
  fi

  # 3. Check well-known Linux directories (Arch Linux, Ubuntu, Fedora, user runtimes)
  for candidate in \
    "/usr/lib/jvm/java-21-openjdk" \
    "/usr/lib/jvm/default" \
    "/usr/lib/jvm/java-21" \
    "$HOME/.local/share/junie/versions/"*"/junie-app/lib/runtime"; do
    if [ -d "$candidate" ] && [ -x "$candidate/bin/java" ]; then
      local v
      v=$("$candidate/bin/java" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
      if [ "$v" = "21" ]; then
        echo "$candidate"
        return 0
      fi
    fi
  done

  return 1
}

DETECTED_JAVA_HOME=$(find_java21 || true)
if [ -z "$DETECTED_JAVA_HOME" ]; then
  echo -e "${C_RED}✗ Java 21 was not found on your system or in JAVA_HOME.${C_RESET}"
  echo -e "Please install OpenJDK 21 (e.g. 'sudo pacman -S jdk21-openjdk' on Arch or 'sudo apt install openjdk-21-jdk') and set JAVA_HOME."
  exit 1
fi

export JAVA_HOME="$DETECTED_JAVA_HOME"
export PATH="$JAVA_HOME/bin:$HOME/.local/bin:$PATH"

if [ -d "$HOME/.local/lib" ]; then
  export LD_LIBRARY_PATH="$HOME/.local/lib:${LD_LIBRARY_PATH:-}"
fi

if [ -d "$HOME/.local/share/tessdata" ]; then
  export TESSDATA_PREFIX="${TESSDATA_PREFIX:-$HOME/.local/share/tessdata}"
fi

export PGDATA="${PGDATA:-$HOME/.local/share/aicte_postgres_data}"

echo -e "${C_GREEN}✓ Using Java 21 from: ${JAVA_HOME}${C_RESET}"

# ------------------------------------------------------------------------------
# Environment Variables & Configuration
# ------------------------------------------------------------------------------
if [ ! -f "$PROJECT_ROOT/.env" ]; then
  if [ -f "$PROJECT_ROOT/.env.example" ]; then
    echo -e "⚙️ Creating .env from .env.example with standard local development defaults..."
    sed 's/your_secure_db_password_here/aicte_123/' "$PROJECT_ROOT/.env.example" > "$PROJECT_ROOT/.env"
  fi
fi

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
FRONTEND_HOST="${FRONTEND_HOST:-localhost}"

# PID tracking for clean teardown - track ONLY processes launched by this script
BACKEND_PID=""
FRONTEND_PID=""

cleanup() {
  echo ""
  echo -e "${C_YELLOW}>>> Initiating graceful shutdown of SAGE services...${C_RESET}"
  if [ -n "$FRONTEND_PID" ] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
    echo -e "Stopping Frontend (PID: $FRONTEND_PID)..."
    kill -TERM "$FRONTEND_PID" 2>/dev/null || true
  fi
  if [ -n "$BACKEND_PID" ] && kill -0 "$BACKEND_PID" 2>/dev/null; then
    echo -e "Stopping Backend (PID: $BACKEND_PID)..."
    kill -TERM "$BACKEND_PID" 2>/dev/null || true
  fi
  echo -e "${C_GREEN}>>> SAGE platform services stopped cleanly.${C_RESET}"
  exit 0
}

trap cleanup SIGINT SIGTERM EXIT

# Port conflict checking helper - NEVER kills arbitrary processes
check_port_available() {
  local port="$1"
  local svc_name="$2"
  local conflict_pid=""

  if command -v lsof >/dev/null 2>&1; then
    conflict_pid=$(lsof -ti:"${port}" 2>/dev/null || true)
  elif command -v ss >/dev/null 2>&1; then
    conflict_pid=$(ss -lptn "sport = :${port}" 2>/dev/null | grep -o 'pid=[0-9]*' | head -n1 | cut -d'=' -f2 || true)
  elif command -v fuser >/dev/null 2>&1; then
    conflict_pid=$(fuser "${port}/tcp" 2>/dev/null || true)
  fi

  if [ -n "$conflict_pid" ]; then
    echo -e "${C_RED}✗ Port ${port} required for ${svc_name} is already occupied by process (PID: ${conflict_pid}).${C_RESET}"
    echo -e "Please stop the conflicting process or change the port configuration in .env before running."
    return 1
  fi
  return 0
}

# ------------------------------------------------------------------------------
# 1. Start / Verify PostgreSQL Database
# ------------------------------------------------------------------------------
echo -e "\n${C_BOLD}[1/3] Checking PostgreSQL Database (${DB_HOST}:${DB_PORT})...${C_RESET}"

is_db_ready() {
  if command -v pg_isready >/dev/null 2>&1; then
    pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" >/dev/null 2>&1
  else
    timeout 1 bash -c "cat < /dev/null > /dev/tcp/${DB_HOST}/${DB_PORT}" 2>/dev/null
  fi
}

if is_db_ready; then
  echo -e "${C_GREEN}✓ PostgreSQL is active and accepting connections on port ${DB_PORT}.${C_RESET}"
else
  echo -e "${C_YELLOW}! PostgreSQL is not currently running. Attempting startup...${C_RESET}"

  if [ -d "$PGDATA" ] && command -v pg_ctl >/dev/null 2>&1; then
    echo -e "Starting local PostgreSQL server from $PGDATA..."
    pg_ctl -D "$PGDATA" -l "$PGDATA/server.log" start || true
  elif command -v docker >/dev/null 2>&1 && docker ps >/dev/null 2>&1; then
    echo -e "Starting PostgreSQL container via docker compose..."
    docker compose up -d postgres || true
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
    echo -e "Please verify your database service and credentials matching .env or compose.yaml."
    exit 1
  fi
fi

# ------------------------------------------------------------------------------
# 2. Check Port and Start Spring Boot Backend
# ------------------------------------------------------------------------------
echo -e "\n${C_BOLD}[2/3] Starting Spring Boot Backend API (Port: ${SERVER_PORT})...${C_RESET}"

if ! check_port_available "${SERVER_PORT}" "Spring Boot Backend"; then
  exit 1
fi

chmod +x ./mvnw
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
    echo -e "\n${C_RED}✗ Spring Boot process exited unexpectedly. Check target/ logs for details.${C_RESET}"
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
# 3. Check Port and Start Vite React Frontend
# ------------------------------------------------------------------------------
echo -e "\n${C_BOLD}[3/3] Starting Vite React Frontend (Port: ${FRONTEND_PORT})...${C_RESET}"

if ! check_port_available "${FRONTEND_PORT}" "React Frontend"; then
  exit 1
fi

cd "$PROJECT_ROOT/frontend"

if [ ! -d "node_modules" ]; then
  echo -e "Installing frontend npm dependencies..."
  npm install
fi

# Start frontend development server in background - binds to localhost by default
if [ "$FRONTEND_HOST" = "0.0.0.0" ]; then
  npm run dev -- --host 0.0.0.0 --port "${FRONTEND_PORT}" &
else
  npm run dev -- --port "${FRONTEND_PORT}" &
fi
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
echo -e "${C_BOLD}${C_GREEN}    🚀 SAGE — System for Approval Governance and Evaluation is OPERATIONAL${C_RESET}"
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
