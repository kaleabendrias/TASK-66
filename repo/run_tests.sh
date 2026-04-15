#!/usr/bin/env bash
set -euo pipefail

BOLD="\033[1m"
RED="\033[31m"
GREEN="\033[32m"
YELLOW="\033[33m"
RESET="\033[0m"

FAIL=0

log()  { echo -e "${BOLD}${YELLOW}>>> $1${RESET}"; }
pass() { echo -e "${BOLD}${GREEN}  PASS: $1${RESET}"; }
fail() { echo -e "${BOLD}${RED}  FAIL: $1${RESET}"; FAIL=1; }

# Generate secrets at runtime if not already set (no .env, no hardcoded values)
export APP_JWT_SECRET="${APP_JWT_SECRET:-$(openssl rand -hex 32)}"
export APP_ENCRYPTION_SECRET="${APP_ENCRYPTION_SECRET:-$(openssl rand -hex 32)}"
log "Runtime secrets generated and exported (no .env file used)"

# ─────────────────────────────────────────────
# 1. Backend unit tests (Maven + JaCoCo, >= 70% line coverage,
#    enforced by the jacoco-maven-plugin `check` rule in backend/pom.xml)
# ─────────────────────────────────────────────
log "Running backend unit tests with coverage..."
if docker run --rm \
  -v "$(pwd)/backend:/app/backend" \
  -v "$(pwd)/unit_tests:/app/unit_tests" \
  -w /app/backend \
  maven:3.9-eclipse-temurin-21 \
  mvn test -Dspring.profiles.active=test -B 2>&1 | tee /tmp/backend-test.log | tail -30; then
  pass "Backend unit tests"
else
  fail "Backend unit tests"
fi

# Extract JaCoCo coverage from log
BACKEND_COV=$(grep -oP 'Total.*?(\d+)%' /tmp/backend-test.log 2>/dev/null | tail -1 || echo "see report")
echo -e "  Backend coverage: ${BACKEND_COV}"

# ─────────────────────────────────────────────
# 2. Frontend unit tests (Vitest + v8, >= 74% lines/statements,
#    55% branches, 40% functions — see frontend/vitest.config.ts)
# ─────────────────────────────────────────────
log "Running frontend unit tests with coverage..."
if docker run --rm \
  -v "$(pwd)/frontend:/app/frontend" \
  -v "$(pwd)/unit_tests:/app/unit_tests" \
  -w /app/frontend \
  node:20-alpine \
  sh -c "npx vitest run --coverage 2>&1" | tee /tmp/frontend-test.log | tail -30; then
  pass "Frontend unit tests"
else
  fail "Frontend unit tests"
fi

# ─────────────────────────────────────────────
# 3. API integration tests (JUnit 5 + RestAssured against live stack,
#    >= 90% endpoint coverage across 86 API endpoints)
# ─────────────────────────────────────────────
log "Starting services for API integration tests..."
docker compose down -v 2>/dev/null || true
docker compose up -d --build --wait 2>&1 | tail -5

# Wait for backend readiness — the backend only listens on HTTPS 8443
# inside the container; the readiness probe hits the proxy over TLS.
log "Waiting for backend to be ready..."
for i in $(seq 1 30); do
  if curl -k -s -o /dev/null -w '%{http_code}' https://localhost:8443/api/categories | grep -qE '^(200|401|403)$'; then
    break
  fi
  sleep 2
done

log "Running API integration tests (JUnit 5 + RestAssured)..."
if docker run --rm \
  --network host \
  -e API_BASE_URL=https://localhost:8443/api \
  -v "$(pwd)/API_tests:/tests" \
  -v "$HOME/.m2:/root/.m2" \
  -w /tests \
  maven:3.9-eclipse-temurin-21 \
  mvn test -B 2>&1 | tee /tmp/api-test.log | tail -40; then
  pass "API integration tests"
else
  fail "API integration tests"
fi

# ─────────────────────────────────────────────
# 4. Playwright E2E tests (browser-level journeys against live stack,
#    covers login, RBAC, incident creation, cross-page navigation)
# ─────────────────────────────────────────────
log "Running Playwright E2E tests..."
if docker run --rm \
  --network host \
  -e BASE_URL=https://localhost:8443 \
  -v "$(pwd)/e2e:/e2e" \
  -w /e2e \
  mcr.microsoft.com/playwright:v1.44.0-jammy \
  bash -c "npx playwright test --reporter=list 2>&1" | tee /tmp/e2e-test.log | tail -40; then
  pass "Playwright E2E tests"
else
  fail "Playwright E2E tests"
fi

# ─────────────────────────────────────────────
# Summary
# ─────────────────────────────────────────────
echo ""
log "Test Summary"
echo "─────────────────────────────────────────"
grep -E "Tests run:|PASS|FAIL|passed|failed|coverage" /tmp/backend-test.log 2>/dev/null | tail -5 || true
echo "─────────────────────────────────────────"
grep -E "Tests|pass|fail|Coverage|Statements|Branches|Functions|Lines" /tmp/frontend-test.log 2>/dev/null | tail -10 || true
echo "─────────────────────────────────────────"
grep -E "Tests run:|BUILD|FAILURE|SUCCESS" /tmp/api-test.log 2>/dev/null | tail -5 || true
echo "─────────────────────────────────────────"
grep -E "passed|failed|error" /tmp/e2e-test.log 2>/dev/null | tail -5 || true
echo "─────────────────────────────────────────"

if [ "$FAIL" -ne 0 ]; then
  echo -e "\n${BOLD}${RED}ONE OR MORE TEST SUITES FAILED${RESET}"
  exit 1
else
  echo -e "\n${BOLD}${GREEN}ALL TEST SUITES PASSED${RESET}"
  exit 0
fi
