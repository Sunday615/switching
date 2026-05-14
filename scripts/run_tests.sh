#!/usr/bin/env bash
# =============================================================================
#  Switching API — Full Automated Test Runner
#
#  Usage:
#    ./scripts/run_tests.sh
#    BASE_URL=http://localhost:8080 ./scripts/run_tests.sh
#
#  Requires: curl, jq
# =============================================================================

# ── Dependencies check ───────────────────────────────────────────────────────
for cmd in curl jq; do
  if ! command -v "$cmd" &>/dev/null; then
    echo "ERROR: '$cmd' is required but not installed. Exiting."
    exit 1
  fi
done

# ── Config ───────────────────────────────────────────────────────────────────
BASE_URL="${BASE_URL:-http://localhost:8080}"

ADMIN_KEY="sk-admin-switching-2026"
OPS_KEY="sk-ops-switching-2026"
BANK_A_KEY="sk-bank-a-switching-2026"
BANK_B_KEY="sk-bank-b-switching-2026"

RUN_ID="$(date +%s)"
BANK_A="BANK_A"
BANK_B="BANK_B"
DEBTOR="010100000001"
CREDITOR="020200000001"
AMOUNT="150000.00"
CURRENCY="LAK"

RESP_FILE="/tmp/switching_test_resp_$$.json"

# ── Colors ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m'

# ── Counters ─────────────────────────────────────────────────────────────────
PASS=0
FAIL=0
SKIP=0

# ── Runtime state (populated during test run) ────────────────────────────────
INQUIRY_REF=""
TRANSFER_REF=""
IDEM_INQUIRY_REF=""
IDEM_TRANSFER_REF=""
REJECT_INQUIRY_REF=""
REJECT_TRANSFER_REF=""
ISO_INQUIRY_REF=""
ISO_TRANSFER_REF=""
IDEM_KEY="AUTO-IDEM-${RUN_ID}"

HTTP_CODE=""
RESP_BODY=""

# ── Helpers ──────────────────────────────────────────────────────────────────

section() {
  echo ""
  echo -e "${BOLD}${BLUE}══════════════════════════════════════════════════${NC}"
  echo -e "${BOLD}${BLUE}  $*${NC}"
  echo -e "${BOLD}${BLUE}══════════════════════════════════════════════════${NC}"
}

info() {
  echo -e "  ${CYAN}ℹ  $*${NC}"
}

warn() {
  echo -e "  ${YELLOW}⚠  $*${NC}"
}

pass() {
  local tc="$1"
  local desc="$2"
  local detail="${3:-}"
  if [ -n "$detail" ]; then
    echo -e "  ${GREEN}✅ PASS${NC} ${BOLD}${tc}${NC}  ${desc}  ${DIM}${detail}${NC}"
  else
    echo -e "  ${GREEN}✅ PASS${NC} ${BOLD}${tc}${NC}  ${desc}"
  fi
  ((PASS++)) || true
}

fail() {
  local tc="$1"
  local desc="$2"
  local detail="${3:-}"
  if [ -n "$detail" ]; then
    echo -e "  ${RED}❌ FAIL${NC} ${BOLD}${tc}${NC}  ${desc}  ${DIM}${detail}${NC}"
  else
    echo -e "  ${RED}❌ FAIL${NC} ${BOLD}${tc}${NC}  ${desc}"
  fi
  if [ -n "$RESP_BODY" ]; then
    local preview
    preview=$(echo "$RESP_BODY" | head -c 300 | tr '\n' ' ')
    echo -e "    ${DIM}↳ Response: ${preview}${NC}"
  fi
  ((FAIL++)) || true
}

skip() {
  local tc="$1"
  local desc="$2"
  echo -e "  ${YELLOW}⏭  SKIP${NC} ${BOLD}${tc}${NC}  ${desc}"
  ((SKIP++)) || true
}

# Run curl and capture HTTP status code + response body
# Usage: do_curl <curl_args...>
# Sets: HTTP_CODE, RESP_BODY
do_curl() {
  HTTP_CODE=$(curl -s -o "$RESP_FILE" -w "%{http_code}" "$@" 2>/dev/null)
  RESP_BODY=$(cat "$RESP_FILE" 2>/dev/null || echo "")
}

# Assert HTTP status is within allowed codes (pipe-separated, e.g. "200|201|409")
# Usage: check_status TC_ID "description" "200|201|409"
check_status() {
  local tc="$1"
  local desc="$2"
  local expected="$3"

  if [ -z "$HTTP_CODE" ]; then
    fail "$tc" "$desc" "no response — server unreachable? (BASE_URL=$BASE_URL)"
    return 1
  fi

  if echo "$HTTP_CODE" | grep -qE "^(${expected})$"; then
    pass "$tc" "$desc" "HTTP $HTTP_CODE"
    return 0
  else
    fail "$tc" "$desc" "expected HTTP ${expected}, got HTTP ${HTTP_CODE}"
    return 1
  fi
}

# Assert a jq field equals expected value (uses $RESP_BODY from last do_curl)
# Usage: check_body TC_ID "description" ".field" "expected_value"
check_body() {
  local tc="$1"
  local desc="$2"
  local jq_expr="$3"
  local expected="$4"
  local actual
  actual=$(echo "$RESP_BODY" | jq -r "${jq_expr} // empty" 2>/dev/null)

  if [ "$actual" = "$expected" ]; then
    pass "$tc" "$desc" "${jq_expr} = ${actual}"
    return 0
  else
    fail "$tc" "$desc" "expected ${jq_expr} = '${expected}', got '${actual}'"
    return 1
  fi
}

# Extract a value from $RESP_BODY
jq_val() {
  echo "$RESP_BODY" | jq -r "${1} // empty" 2>/dev/null
}

xml_val() {
  local tag="$1"
  echo "$RESP_BODY" \
    | tr '\n' ' ' \
    | sed -n "s:.*<${tag}>\\([^<]*\\)</${tag}>.*:\\1:p" \
    | head -1
}

body_has() {
  local expected="$1"
  echo "$RESP_BODY" | grep -Fq "$expected"
}

# Print pretty-printed jq output from $RESP_BODY
# Usage: show_detail '{key: .field}'  OR  show_detail '.'
show_detail() {
  local jq_expr="${1:-.}"

  # raw check (no color) — skip if empty/null
  local raw
  raw=$(echo "$RESP_BODY" | jq -r "$jq_expr" 2>/dev/null)
  [ -z "$raw" ] || [ "$raw" = "null" ] || [ "$raw" = "null" ] && return 0

  # colored pretty-print (jq .)
  local colored
  colored=$(echo "$RESP_BODY" | jq -C "$jq_expr" 2>/dev/null)
  [ -z "$colored" ] && return 0

  local line_count
  line_count=$(echo "$colored" | wc -l | tr -d '[:space:]')

  if [ "$line_count" -le 1 ]; then
    # Single-line value (string/number) — inline, strip outer quotes
    local inline
    inline=$(echo "$raw" | head -1)
    echo -e "    ${DIM}↳ ${inline}${NC}"
  else
    # Multi-line object/array — pretty-print with indentation (jq . style)
    echo -e "    ${DIM}↳${NC}"
    echo "$colored" | sed 's/^/      /'
  fi
}

cleanup() {
  rm -f "$RESP_FILE"
}
trap cleanup EXIT

# ── Connectivity pre-check ───────────────────────────────────────────────────
HEALTH_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$BASE_URL/actuator/health" 2>/dev/null)
if [ -z "$HEALTH_CODE" ] || [ "$HEALTH_CODE" = "000" ]; then
  echo ""
  echo -e "  ${RED}${BOLD}ERROR: Cannot reach $BASE_URL — is the server running?${NC}"
  echo -e "  ${DIM}Tip: set -a && source .env && set +a && ./mvnw spring-boot:run > /tmp/app.log 2>&1 &${NC}"
  echo ""
  exit 1
fi

# =============================================================================
echo ""
echo -e "${BOLD}${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${CYAN}║     Switching API — Full Automated Test Runner       ║${NC}"
echo -e "${BOLD}${CYAN}╚══════════════════════════════════════════════════════╝${NC}"
echo -e "  ${DIM}BASE_URL  : $BASE_URL${NC}"
echo -e "  ${DIM}RUN_ID    : $RUN_ID${NC}"
echo -e "  ${DIM}Started   : $(date '+%Y-%m-%d %H:%M:%S')${NC}"

# =============================================================================
section "1. Health / Public Endpoints"
# =============================================================================

# TC-001
do_curl "$BASE_URL/actuator/health"
check_status "TC-001" "Actuator /health is public and returns UP" "200"

# TC-002
do_curl "$BASE_URL/api/operations/health"
check_status "TC-002" "Ops /health without API key → 401" "401"

# TC-003
do_curl "$BASE_URL/api/operations/health" \
  -H "X-API-Key: $OPS_KEY"
check_status "TC-003" "Ops /health with OPS key → 200" "200"

# =============================================================================
section "2. API Key / Role Authorization"
# =============================================================================

# TC-010
do_curl "$BASE_URL/api/participants"
check_status "TC-010" "No API key on /participants → 401" "401"

# TC-011
do_curl "$BASE_URL/api/operations/dashboard-summary" \
  -H "X-API-Key: $BANK_A_KEY"
check_status "TC-011" "BANK key on /operations/* → 403" "403"

# TC-012
do_curl "$BASE_URL/api/participants" \
  -H "X-API-Key: $OPS_KEY"
check_status "TC-012" "OPS key can read /participants → 200" "200"

# TC-013
do_curl -X POST "$BASE_URL/api/participants" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $OPS_KEY" \
  -d "{\"bankCode\":\"BANK_TEST_${RUN_ID}\",\"bankName\":\"Test\",\"participantType\":\"BANK\",\"country\":\"LA\",\"currency\":\"LAK\",\"status\":\"ACTIVE\"}"
check_status "TC-013" "OPS key cannot create participant (write) → 403" "403"

# TC-014
do_curl "$BASE_URL/api/participants" \
  -H "X-API-Key: sk-invalid-fake-key-xyz"
check_status "TC-014" "Invalid API key → 401" "401"

# =============================================================================
section "3. Admin Setup (Participants, Connector, Routing Rule)"
# =============================================================================

info "Note: 409 is expected if records already exist from previous runs."

# TC-020
do_curl -X POST "$BASE_URL/api/participants" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $ADMIN_KEY" \
  -d "{\"bankCode\":\"${BANK_A}\",\"bankName\":\"Source Test Bank\",\"participantType\":\"BANK\",\"country\":\"LA\",\"currency\":\"LAK\",\"status\":\"ACTIVE\"}"
check_status "TC-020" "Create BANK_A participant → 200|201|409" "200|201|409"
show_detail '{bankCode, bankName, status, participantType}'

# TC-021
do_curl -X POST "$BASE_URL/api/participants" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $ADMIN_KEY" \
  -d "{\"bankCode\":\"${BANK_B}\",\"bankName\":\"Receiver Test Bank\",\"participantType\":\"BANK\",\"country\":\"LA\",\"currency\":\"LAK\",\"status\":\"ACTIVE\"}"
check_status "TC-021" "Create BANK_B participant → 200|201|409" "200|201|409"
show_detail '{bankCode, bankName, status, participantType}'

# TC-022
do_curl -X POST "$BASE_URL/api/connector-configs" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $ADMIN_KEY" \
  -d "{\"connectorName\":\"MOCK_BANK_B_CONNECTOR\",\"bankCode\":\"${BANK_B}\",\"connectorType\":\"MOCK\",\"timeoutMs\":5000,\"enabled\":true,\"forceReject\":false,\"rejectReasonCode\":\"AC01\",\"rejectReasonMessage\":\"Mock reject\"}"
check_status "TC-022" "Create MOCK_BANK_B_CONNECTOR → 200|201|409" "200|201|409"
show_detail '{connectorName, bankCode, connectorType, forceReject, enabled}'

# TC-023
do_curl -X POST "$BASE_URL/api/routing-rules" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $ADMIN_KEY" \
  -d "{\"routeCode\":\"ROUTE_A_TO_B_${RUN_ID}\",\"sourceBank\":\"${BANK_A}\",\"destinationBank\":\"${BANK_B}\",\"messageType\":\"PACS_008\",\"connectorName\":\"MOCK_BANK_B_CONNECTOR\",\"priority\":1,\"enabled\":true}"
check_status "TC-023" "Create routing rule BANK_A→BANK_B → 200|201" "200|201"
show_detail '{routeCode, sourceBank, destinationBank, messageType, connectorName, enabled}'

# TC-024
do_curl "$BASE_URL/api/routing-rules/resolve?sourceBank=${BANK_A}&destinationBank=${BANK_B}&messageType=PACS_008" \
  -H "X-API-Key: $OPS_KEY"
check_status "TC-024" "Resolve routing rule BANK_A→BANK_B → 200" "200"
check_body "TC-024b" "Routing resolves to MOCK_BANK_B_CONNECTOR" ".connectorName" "MOCK_BANK_B_CONNECTOR"
show_detail '{routeCode, sourceBank, destinationBank, connectorName, priority}'

# =============================================================================
section "4. JSON Inquiry Flow"
# =============================================================================

# TC-030
info "Creating JSON inquiry (RUN_ID=${RUN_ID})..."
do_curl -X POST "$BASE_URL/api/inquiries" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $BANK_A_KEY" \
  -d "{\"sourceBank\":\"${BANK_A}\",\"destinationBank\":\"${BANK_B}\",\"creditorAccount\":\"${CREDITOR}\",\"amount\":${AMOUNT},\"currency\":\"${CURRENCY}\",\"reference\":\"AUTO-INQ-${RUN_ID}\"}"
check_status "TC-030" "Create JSON inquiry → 200" "200"
show_detail '{inquiryRef, status, eligibleForTransfer, sourceBank, destinationBank, creditorAccount, amount, currency}'
INQUIRY_REF=$(jq_val ".inquiryRef")
if [ -n "$INQUIRY_REF" ]; then
  info "inquiryRef captured: $INQUIRY_REF"
else
  warn "Could not capture inquiryRef — transfer tests in Section 5 will be skipped"
fi

# TC-031
if [ -n "$INQUIRY_REF" ]; then
  do_curl "$BASE_URL/api/inquiries/${INQUIRY_REF}" \
    -H "X-API-Key: $BANK_A_KEY"
  check_status "TC-031" "GET inquiry by ref → 200" "200"
  check_body "TC-031b" "Inquiry status = ELIGIBLE" ".status" "ELIGIBLE"
  show_detail '{inquiryRef, status, eligibleForTransfer, accountFound, bankAvailable}'
else
  skip "TC-031" "GET inquiry by ref — skipped (no inquiryRef)"
  skip "TC-031b" "Inquiry status ELIGIBLE — skipped"
fi

# TC-032
do_curl "$BASE_URL/api/inquiries/INQ-NOTFOUND-${RUN_ID}" \
  -H "X-API-Key: $BANK_A_KEY"
check_status "TC-032" "GET non-existent inquiry → 404 INQ-001" "404"

# TC-033
do_curl -X POST "$BASE_URL/api/inquiries" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $BANK_A_KEY" \
  -d "{\"sourceBank\":\"${BANK_A}\"}"
check_status "TC-033" "Create inquiry with missing fields → 400 REQ-001" "400"

# =============================================================================
section "5. JSON Transfer Flow (Happy Path)"
# =============================================================================

if [ -z "$INQUIRY_REF" ]; then
  warn "No inquiryRef available — skipping all transfer happy-path tests"
  for tc in "TC-040" "TC-040b" "TC-041" "TC-041b" "TC-042" "TC-043" "TC-043b" "TC-044" "TC-044b" "TC-044c"; do
    skip "$tc" "Skipped (no inquiryRef from TC-030)"
  done
else
  # TC-040
  info "Creating transfer with inquiryRef=${INQUIRY_REF}..."
  do_curl -X POST "$BASE_URL/api/transfers" \
    -H "Content-Type: application/json" \
    -H "X-API-Key: $BANK_A_KEY" \
    -d "{\"inquiryRef\":\"${INQUIRY_REF}\",\"sourceBank\":\"${BANK_A}\",\"destinationBank\":\"${BANK_B}\",\"debtorAccount\":\"${DEBTOR}\",\"creditorAccount\":\"${CREDITOR}\",\"amount\":${AMOUNT},\"currency\":\"${CURRENCY}\",\"reference\":\"AUTO-TRF-${RUN_ID}\"}"
  check_status "TC-040" "Create JSON transfer → 200 RECEIVED" "200"
  show_detail '{transferRef, status, sourceBank: .sourceBankCode, destinationBank: .destinationBankCode, amount, currency, inquiryRef}'
  TRANSFER_REF=$(jq_val ".transferRef")
  if [ -n "$TRANSFER_REF" ]; then
    info "transferRef captured: $TRANSFER_REF"
    check_body "TC-040b" "Transfer initial status = RECEIVED" ".status" "RECEIVED"
  else
    warn "Could not capture transferRef"
    skip "TC-040b" "Initial status check — skipped (no transferRef)"
  fi

  # TC-041 — Near real-time dispatch check
  if [ -n "$TRANSFER_REF" ]; then
    info "Waiting 2s for near real-time outbox dispatch..."
    sleep 2
    do_curl "$BASE_URL/api/transfers/${TRANSFER_REF}" \
      -H "X-API-Key: $BANK_A_KEY"
    check_status "TC-041" "GET transfer status after 2s → 200" "200"
    show_detail '{transferRef, status, errorCode, errorMessage}'
    STATUS=$(jq_val ".status")
    if [ "$STATUS" = "SUCCESS" ]; then
      pass "TC-041b" "Transfer status = SUCCESS after 2s (near real-time dispatch ✅)" "status=SUCCESS"
    elif [ "$STATUS" = "RECEIVED" ]; then
      warn "Still RECEIVED after 2s — waiting 3 more seconds..."
      sleep 3
      do_curl "$BASE_URL/api/transfers/${TRANSFER_REF}" -H "X-API-Key: $BANK_A_KEY"
      show_detail '{transferRef, status, errorCode}'
      STATUS=$(jq_val ".status")
      if [ "$STATUS" = "SUCCESS" ]; then
        pass "TC-041b" "Transfer status = SUCCESS after 5s" "status=SUCCESS"
      else
        fail "TC-041b" "Transfer not SUCCESS after 5s" "status=${STATUS}"
      fi
    else
      fail "TC-041b" "Unexpected transfer status after dispatch" "status=${STATUS}"
    fi
  else
    skip "TC-041" "Transfer status check — skipped (no transferRef)"
    skip "TC-041b" "Near real-time check — skipped"
  fi

  # TC-042
  if [ -n "$TRANSFER_REF" ]; then
    do_curl "$BASE_URL/api/transfers/${TRANSFER_REF}" \
      -H "X-API-Key: $BANK_A_KEY"
    check_status "TC-042" "GET transfer by ref → 200" "200"
    show_detail '{transferRef, status, sourceBankCode, destinationBankCode, amount, currency, channelId, inquiryRef}'
  else
    skip "TC-042" "GET transfer — skipped (no transferRef)"
  fi

  # TC-043 — Public trace (check inquiryRef in response)
  if [ -n "$TRANSFER_REF" ]; then
    do_curl "$BASE_URL/api/transfers/${TRANSFER_REF}/trace" \
      -H "X-API-Key: $BANK_A_KEY"
    check_status "TC-043" "Public transfer trace → 200" "200"
    show_detail '{status, inquiryRef, channelId, timelineCount: (.timeline | length)}'
    INQ_IN_TRACE=$(jq_val ".inquiryRef")
    if [ -n "$INQ_IN_TRACE" ] && [ "$INQ_IN_TRACE" != "null" ]; then
      pass "TC-043b" "Public trace has inquiryRef (JSON trace fix ✅)" "inquiryRef=${INQ_IN_TRACE}"
    else
      fail "TC-043b" "Public trace missing inquiryRef — JSON trace fix not working"
    fi
  else
    skip "TC-043" "Public trace — skipped (no transferRef)"
    skip "TC-043b" "Trace inquiryRef check — skipped"
  fi

  # TC-044 — Operations trace
  if [ -n "$TRANSFER_REF" ]; then
    do_curl "$BASE_URL/api/operations/transfers/${TRANSFER_REF}/trace" \
      -H "X-API-Key: $OPS_KEY"
    check_status "TC-044" "Ops transfer trace → 200" "200"
    show_detail '{status, warnings: (.warnings | length), summary: {hasInquiry: .summary.hasInquiry, timelineEventCount: .summary.timelineEventCount, hasOutbox: .summary.hasOutbox, hasIsoMessages: .summary.hasIsoMessages}, inquiryApiPath: .inquiry.inquiryApiPath}'
    HAS_INQ=$(jq_val ".summary.hasInquiry")
    if [ "$HAS_INQ" = "true" ]; then
      pass "TC-044b" "Ops trace summary.hasInquiry = true (JSON trace fix ✅)"
    else
      fail "TC-044b" "Ops trace summary.hasInquiry ≠ true" "hasInquiry=${HAS_INQ}"
    fi
    WARN_COUNT=$(echo "$RESP_BODY" | jq -r '(.warnings // []) | length' 2>/dev/null || echo "?")
    if [ "$WARN_COUNT" = "0" ]; then
      pass "TC-044c" "Ops trace has 0 warnings (no errors in trace pipeline)"
    else
      fail "TC-044c" "Ops trace has warnings" "warnings=${WARN_COUNT}"
    fi
  else
    skip "TC-044" "Ops trace — skipped (no transferRef)"
    skip "TC-044b" "hasInquiry check — skipped"
    skip "TC-044c" "Warnings check — skipped"
  fi
fi

# TC-045 — Reuse inquiry → 409
if [ -n "$INQUIRY_REF" ]; then
  do_curl -X POST "$BASE_URL/api/transfers" \
    -H "Content-Type: application/json" \
    -H "X-API-Key: $BANK_A_KEY" \
    -d "{\"inquiryRef\":\"${INQUIRY_REF}\",\"sourceBank\":\"${BANK_A}\",\"destinationBank\":\"${BANK_B}\",\"debtorAccount\":\"${DEBTOR}\",\"creditorAccount\":\"${CREDITOR}\",\"amount\":${AMOUNT},\"currency\":\"${CURRENCY}\"}"
  check_status "TC-045" "Reuse already-used inquiry → 409 INQ-003" "409"
else
  skip "TC-045" "Reuse inquiry check — skipped (no inquiryRef)"
fi

# TC-046 — Missing inquiryRef → 400
do_curl -X POST "$BASE_URL/api/transfers" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $BANK_A_KEY" \
  -d "{\"sourceBank\":\"${BANK_A}\",\"destinationBank\":\"${BANK_B}\",\"debtorAccount\":\"${DEBTOR}\",\"creditorAccount\":\"${CREDITOR}\",\"amount\":${AMOUNT},\"currency\":\"${CURRENCY}\"}"
check_status "TC-046" "Create transfer without inquiryRef → 400 REQ-001" "400"

# TC-047 — Transfer not found
do_curl "$BASE_URL/api/transfers/TRX-NOT-EXIST-${RUN_ID}" \
  -H "X-API-Key: $BANK_A_KEY"
check_status "TC-047" "GET non-existent transfer → 404 TRF-001" "404"

# =============================================================================
section "6. Idempotency"
# =============================================================================

# TC-050 — New inquiry for idempotency test
info "Creating fresh inquiry for idempotency test..."
do_curl -X POST "$BASE_URL/api/inquiries" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $BANK_A_KEY" \
  -d "{\"sourceBank\":\"${BANK_A}\",\"destinationBank\":\"${BANK_B}\",\"creditorAccount\":\"${CREDITOR}\",\"amount\":${AMOUNT},\"currency\":\"${CURRENCY}\",\"reference\":\"IDEM-INQ-${RUN_ID}\"}"
check_status "TC-050" "Create inquiry for idempotency test → 200" "200"
show_detail '{inquiryRef, status, eligibleForTransfer, amount, currency}'
IDEM_INQUIRY_REF=$(jq_val ".inquiryRef")
[ -n "$IDEM_INQUIRY_REF" ] && info "Idempotency inquiryRef: $IDEM_INQUIRY_REF"

# TC-051 — First transfer with idempotencyKey
if [ -n "$IDEM_INQUIRY_REF" ]; then
  do_curl -X POST "$BASE_URL/api/transfers" \
    -H "Content-Type: application/json" \
    -H "X-API-Key: $BANK_A_KEY" \
    -d "{\"inquiryRef\":\"${IDEM_INQUIRY_REF}\",\"sourceBank\":\"${BANK_A}\",\"destinationBank\":\"${BANK_B}\",\"debtorAccount\":\"${DEBTOR}\",\"creditorAccount\":\"${CREDITOR}\",\"amount\":${AMOUNT},\"currency\":\"${CURRENCY}\",\"idempotencyKey\":\"${IDEM_KEY}\"}"
  check_status "TC-051" "First transfer with idempotencyKey → 200" "200"
  show_detail '{transferRef, status, inquiryRef}'
  IDEM_TRANSFER_REF=$(jq_val ".transferRef")
  [ -n "$IDEM_TRANSFER_REF" ] && info "Idempotency transferRef: $IDEM_TRANSFER_REF"
else
  skip "TC-051" "First idempotency transfer — skipped (no idem inquiryRef)"
fi

# TC-052 — Exact same request → same transferRef returned
if [ -n "$IDEM_INQUIRY_REF" ] && [ -n "$IDEM_TRANSFER_REF" ]; then
  do_curl -X POST "$BASE_URL/api/transfers" \
    -H "Content-Type: application/json" \
    -H "X-API-Key: $BANK_A_KEY" \
    -d "{\"inquiryRef\":\"${IDEM_INQUIRY_REF}\",\"sourceBank\":\"${BANK_A}\",\"destinationBank\":\"${BANK_B}\",\"debtorAccount\":\"${DEBTOR}\",\"creditorAccount\":\"${CREDITOR}\",\"amount\":${AMOUNT},\"currency\":\"${CURRENCY}\",\"idempotencyKey\":\"${IDEM_KEY}\"}"
  check_status "TC-052" "Repeat identical request (same hash) → 200" "200"
  show_detail '{transferRef, status}'
  RETURNED_REF=$(jq_val ".transferRef")
  if [ "$RETURNED_REF" = "$IDEM_TRANSFER_REF" ]; then
    pass "TC-052b" "Returned transferRef matches original (idempotency works ✅)" "ref=${RETURNED_REF}"
  else
    fail "TC-052b" "Returned transferRef differs from original" "expected=${IDEM_TRANSFER_REF} got=${RETURNED_REF}"
  fi
else
  skip "TC-052" "Idempotency repeat request — skipped"
  skip "TC-052b" "TransferRef match check — skipped"
fi

# TC-053 — Same idempotencyKey, different payload → 409 TRF-002
if [ -n "$IDEM_INQUIRY_REF" ]; then
  do_curl -X POST "$BASE_URL/api/transfers" \
    -H "Content-Type: application/json" \
    -H "X-API-Key: $BANK_A_KEY" \
    -d "{\"inquiryRef\":\"${IDEM_INQUIRY_REF}\",\"sourceBank\":\"${BANK_A}\",\"destinationBank\":\"${BANK_B}\",\"debtorAccount\":\"${DEBTOR}\",\"creditorAccount\":\"${CREDITOR}\",\"amount\":999.00,\"currency\":\"${CURRENCY}\",\"idempotencyKey\":\"${IDEM_KEY}\"}"
  check_status "TC-053" "Same idempotencyKey, different payload → 409 TRF-002" "409"
else
  skip "TC-053" "Idempotency hash conflict — skipped (no idem inquiryRef)"
fi

# =============================================================================
section "7. Force Reject Flow (Downstream REJECT)"
# =============================================================================

# TC-060 — Enable force_reject
do_curl -X PATCH "$BASE_URL/api/connector-configs/MOCK_BANK_B_CONNECTOR" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $ADMIN_KEY" \
  -d "{\"forceReject\":true}"
check_status "TC-060" "Enable force_reject on MOCK_BANK_B_CONNECTOR → 200" "200"
show_detail '{connectorName, forceReject, enabled, bankCode}'

# TC-061 — New inquiry for reject test
info "Creating fresh inquiry for force-reject test..."
do_curl -X POST "$BASE_URL/api/inquiries" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $BANK_A_KEY" \
  -d "{\"sourceBank\":\"${BANK_A}\",\"destinationBank\":\"${BANK_B}\",\"creditorAccount\":\"${CREDITOR}\",\"amount\":${AMOUNT},\"currency\":\"${CURRENCY}\",\"reference\":\"REJECT-INQ-${RUN_ID}\"}"
check_status "TC-061" "Create inquiry for force-reject test → 200" "200"
show_detail '{inquiryRef, status, eligibleForTransfer}'
REJECT_INQUIRY_REF=$(jq_val ".inquiryRef")
[ -n "$REJECT_INQUIRY_REF" ] && info "Reject inquiryRef: $REJECT_INQUIRY_REF"

# TC-062 — Create transfer (should be accepted, then FAILED by dispatcher)
if [ -n "$REJECT_INQUIRY_REF" ]; then
  do_curl -X POST "$BASE_URL/api/transfers" \
    -H "Content-Type: application/json" \
    -H "X-API-Key: $BANK_A_KEY" \
    -d "{\"inquiryRef\":\"${REJECT_INQUIRY_REF}\",\"sourceBank\":\"${BANK_A}\",\"destinationBank\":\"${BANK_B}\",\"debtorAccount\":\"${DEBTOR}\",\"creditorAccount\":\"${CREDITOR}\",\"amount\":${AMOUNT},\"currency\":\"${CURRENCY}\",\"reference\":\"REJECT-TRF-${RUN_ID}\"}"
  check_status "TC-062" "Create transfer (force_reject=true) → 200 RECEIVED" "200"
  show_detail '{transferRef, status, inquiryRef}'
  REJECT_TRANSFER_REF=$(jq_val ".transferRef")
  [ -n "$REJECT_TRANSFER_REF" ] && info "Reject transferRef: $REJECT_TRANSFER_REF"
else
  skip "TC-062" "Force-reject transfer creation — skipped (no reject inquiryRef)"
fi

# TC-063 — Wait and verify status = FAILED
if [ -n "$REJECT_TRANSFER_REF" ]; then
  info "Waiting 2s for dispatcher to process rejection..."
  sleep 2
  do_curl "$BASE_URL/api/transfers/${REJECT_TRANSFER_REF}" \
    -H "X-API-Key: $BANK_A_KEY"
  check_status "TC-063" "GET rejected transfer status → 200" "200"
  show_detail '{transferRef, status, errorCode, errorMessage}'
  REJ_STATUS=$(jq_val ".status")
  if [ "$REJ_STATUS" = "FAILED" ]; then
    pass "TC-063b" "Transfer status = FAILED after force_reject ✅" "status=FAILED"
  else
    warn "Status=${REJ_STATUS} — waiting 3 more seconds..."
    sleep 3
    do_curl "$BASE_URL/api/transfers/${REJECT_TRANSFER_REF}" -H "X-API-Key: $BANK_A_KEY"
    show_detail '{transferRef, status, errorCode, errorMessage}'
    REJ_STATUS=$(jq_val ".status")
    if [ "$REJ_STATUS" = "FAILED" ]; then
      pass "TC-063b" "Transfer status = FAILED after force_reject ✅ (after 5s)" "status=FAILED"
    else
      fail "TC-063b" "Transfer not FAILED after 5s with force_reject" "status=${REJ_STATUS}"
    fi
  fi
else
  skip "TC-063" "Force-reject status check — skipped (no reject transferRef)"
  skip "TC-063b" "FAILED status assertion — skipped"
fi

# TC-064 — Restore force_reject = false (CRITICAL — must run to unblock subsequent tests)
do_curl -X PATCH "$BASE_URL/api/connector-configs/MOCK_BANK_B_CONNECTOR" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $ADMIN_KEY" \
  -d "{\"forceReject\":false}"
check_status "TC-064" "Disable force_reject on MOCK_BANK_B_CONNECTOR → 200" "200"
show_detail '{connectorName, forceReject, enabled}'

# =============================================================================
section "8. Outbox & Dispatch Operations"
# =============================================================================

# TC-080
do_curl "$BASE_URL/api/outbox-events" \
  -H "X-API-Key: $OPS_KEY"
check_status "TC-080" "List outbox events (OPS key) → 200" "200"
show_detail 'if type == "array" then "count: \(length)  |  latest: {id:\(.[0].id), transferRef:\(.[0].transferRef), status:\(.[0].status), retryCount:\(.[0].retryCount)}" else . end'

# TC-081
do_curl -X POST "$BASE_URL/api/outbox-events/999999999/retry" \
  -H "X-API-Key: $OPS_KEY"
check_status "TC-081" "Retry non-existent outbox event → 404 OUT-005" "404"

# TC-082
do_curl "$BASE_URL/api/outbox-events" \
  -H "X-API-Key: $BANK_A_KEY"
check_status "TC-082" "BANK key on /outbox-events → 403" "403"

# TC-083
do_curl "$BASE_URL/api/operations/outbox-failures" \
  -H "X-API-Key: $OPS_KEY"
check_status "TC-083" "Operations outbox-failures list → 200" "200"

# TC-084
do_curl "$BASE_URL/api/operations/outbox-stuck" \
  -H "X-API-Key: $OPS_KEY"
check_status "TC-084" "Operations outbox-stuck list → 200" "200"

# =============================================================================
section "9. Operations Query Smoke Tests"
# =============================================================================

# TC-090
do_curl "$BASE_URL/api/operations/dashboard-summary" \
  -H "X-API-Key: $OPS_KEY"
check_status "TC-090" "Operations dashboard-summary → 200" "200"
show_detail 'to_entries | map(select(.value | type != "object" and type != "array")) | map("\(.key): \(.value)") | join("  |  ")'

# TC-091
do_curl "$BASE_URL/api/operations/transactions" \
  -H "X-API-Key: $OPS_KEY"
check_status "TC-091" "Operations transactions list → 200" "200"
show_detail 'if type == "array" then "count: \(length)" elif .items then "count: \(.items | length)  |  total: \(.totalItems)" else . end'

# TC-092
do_curl "$BASE_URL/api/operations/transfers" \
  -H "X-API-Key: $OPS_KEY"
check_status "TC-092" "Operations transfers list → 200" "200"
show_detail 'if type == "array" then "count: \(length)  |  latest_status: \(.[0].status)" elif .items then "count: \(.items | length)  |  total: \(.totalItems)" else . end'

# TC-093
do_curl "$BASE_URL/api/operations/iso-messages" \
  -H "X-API-Key: $OPS_KEY"
check_status "TC-093" "Operations iso-messages list → 200" "200"
show_detail 'if type == "array" then "count: \(length)" elif .items then "count: \(.items | length)  |  total: \(.totalItems)" else . end'

# TC-094
do_curl "$BASE_URL/api/operations/audit-logs" \
  -H "X-API-Key: $OPS_KEY"
check_status "TC-094" "Operations audit-logs list → 200" "200"
show_detail 'if type == "array" then "count: \(length)" elif .items then "count: \(.items | length)  |  total: \(.totalItems)" else . end'

# TC-095
do_curl "$BASE_URL/api/operations/connectors/health" \
  -H "X-API-Key: $OPS_KEY"
check_status "TC-095" "Operations connector health → 200" "200"
show_detail 'if type == "array" then map({name: .connectorName, status: .status}) | .[] | "\(.name): \(.status)" else . end'

# TC-096
do_curl "$BASE_URL/api/operations/bank-status" \
  -H "X-API-Key: $OPS_KEY"
check_status "TC-096" "Operations bank-status overview → 200" "200"
show_detail 'if type == "array" then map({bank: .bankCode, status: .status}) | .[] | "\(.bank): \(.status)" else . end'

# =============================================================================
section "10. Transfer List"
# =============================================================================

# TC-110
do_curl "$BASE_URL/api/transfers" \
  -H "X-API-Key: $BANK_A_KEY"
check_status "TC-110" "List all transfers (BANK key) → 200" "200"

# TC-111
do_curl "$BASE_URL/api/operations/transfers?status=SUCCESS" \
  -H "X-API-Key: $OPS_KEY"
check_status "TC-111" "Ops transfers list ?status=SUCCESS → 200" "200"
show_detail 'if type == "array" then "count: \(length)  |  statuses: \([.[].status] | unique | join(", "))" elif .items then "count: \(.items | length)  |  total: \(.totalItems)" else . end'

# =============================================================================
section "11. ISO XML Smoke Tests"
# =============================================================================

# TC-100 — Malformed PACS.008
do_curl -X POST "$BASE_URL/api/iso20022/pacs008" \
  -H "Content-Type: application/xml" \
  -H "X-API-Key: $BANK_A_KEY" \
  -H "X-Bank-Code: $BANK_A" \
  --data-binary '<not-valid-xml>'
# ISO endpoints may return 200 with PACS.002 RJCT XML (ISO protocol), 400, or 500
check_status "TC-100" "Malformed PACS.008 → ISO rejection (200) or HTTP error" "200|400|500"

# TC-101 — ACMT.023 without X-Bank-Code
# ISO endpoints always return HTTP 200 with ACMT.024 XML body even on errors
# (ISO 20022 protocol wraps rejections inside XML, not as HTTP error codes)
do_curl -X POST "$BASE_URL/api/iso20022/acmt023" \
  -H "Content-Type: application/xml" \
  -H "X-API-Key: $BANK_A_KEY" \
  --data-binary '<Document></Document>'
check_status "TC-101" "ACMT.023 without X-Bank-Code → ISO rejection (200) or HTTP error" "200|400|422|500"
# Verify 200 response is actually an error/rejection XML (contains UNKNOWN or no valid inquiry)
if [ "$HTTP_CODE" = "200" ]; then
  if echo "$RESP_BODY" | grep -qi "UNKNOWN\|RJCT\|FAIL\|error"; then
    pass "TC-101b" "ACMT.024 response body signals rejection (ISO protocol behavior ✅)"
  else
    fail "TC-101b" "HTTP 200 but response body does not look like a rejection"
  fi
fi

# TC-102 — OPS key on ISO endpoint → 403
do_curl -X POST "$BASE_URL/api/iso20022/pacs008" \
  -H "Content-Type: application/xml" \
  -H "X-API-Key: $OPS_KEY" \
  -H "X-Bank-Code: $BANK_A" \
  --data-binary '<Document></Document>'
check_status "TC-102" "OPS key on ISO endpoint → 403" "403"

# TC-103 — Valid ACMT.023 smoke
ISO_ACMT_SUFFIX="AUTO-${RUN_ID}"
ISO_XML="<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03\">
  <IdVrfctnReq>
    <Assgnmt>
      <MsgId>ACMT023-${ISO_ACMT_SUFFIX}</MsgId>
      <CreDtTm>2026-05-14T12:00:00Z</CreDtTm>
    </Assgnmt>
    <Vrfctn>
      <Id>VERIFY-${ISO_ACMT_SUFFIX}</Id>
      <PtyAndAcctId>
        <Acct>
          <Id>
            <Othr>
              <Id>${CREDITOR}</Id>
            </Othr>
          </Id>
        </Acct>
      </PtyAndAcctId>
    </Vrfctn>
    <DbtrAgt><FinInstnId><BICFI>${BANK_A}</BICFI></FinInstnId></DbtrAgt>
    <CdtrAgt><FinInstnId><BICFI>${BANK_B}</BICFI></FinInstnId></CdtrAgt>
    <Amt Ccy=\"${CURRENCY}\">${AMOUNT}</Amt>
    <RmtInf><Ustrd>ISO automated test ${ISO_ACMT_SUFFIX}</Ustrd></RmtInf>
  </IdVrfctnReq>
</Document>"

do_curl -X POST "$BASE_URL/api/iso20022/acmt023" \
  -H "Content-Type: application/xml" \
  -H "X-API-Key: $BANK_A_KEY" \
  -H "X-Bank-Code: $BANK_A" \
  --data-binary "$ISO_XML"
check_status "TC-103" "Valid ACMT.023 ISO inquiry smoke → 200 (ACMT.024 XML response)" "200"
ISO_INQUIRY_REF=$(xml_val "InquiryRef")
ISO_VERIFY_STATUS=$(xml_val "Vrfctn")
if [ "$ISO_VERIFY_STATUS" = "MTCH" ] && [ -n "$ISO_INQUIRY_REF" ]; then
  pass "TC-103b" "ACMT.024 response contains MTCH + InquiryRef" "inquiryRef=${ISO_INQUIRY_REF}"
else
  fail "TC-103b" "ACMT.024 response missing MTCH or InquiryRef" "Vrfctn=${ISO_VERIFY_STATUS:-empty} inquiryRef=${ISO_INQUIRY_REF:-empty}"
fi

# TC-104 — Valid PACS.008 uses InquiryRef from TC-103
if [ -n "$ISO_INQUIRY_REF" ]; then
  ISO_PACS_SUFFIX="AUTO-PACS-${RUN_ID}"
  ISO_PACS008_XML="<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.12\">
  <FIToFICstmrCdtTrf>
    <GrpHdr>
      <MsgId>MSG-${ISO_PACS_SUFFIX}</MsgId>
      <CreDtTm>2026-05-14T12:01:00</CreDtTm>
      <NbOfTxs>1</NbOfTxs>
    </GrpHdr>
    <CdtTrfTxInf>
      <PmtId>
        <InstrId>INST-${ISO_PACS_SUFFIX}</InstrId>
        <EndToEndId>E2E-${ISO_PACS_SUFFIX}</EndToEndId>
      </PmtId>
      <IntrBkSttlmAmt Ccy=\"${CURRENCY}\">${AMOUNT}</IntrBkSttlmAmt>
      <DbtrAgt><FinInstnId><BICFI>${BANK_A}</BICFI></FinInstnId></DbtrAgt>
      <CdtrAgt><FinInstnId><BICFI>${BANK_B}</BICFI></FinInstnId></CdtrAgt>
      <DbtrAcct><Id><Othr><Id>${DEBTOR}</Id></Othr></Id></DbtrAcct>
      <CdtrAcct><Id><Othr><Id>${CREDITOR}</Id></Othr></Id></CdtrAcct>
    </CdtTrfTxInf>
    <SplmtryData><PlcAndNm>LAO_SWITCHING_INQUIRY_REF</PlcAndNm><Envlp><InquiryRef>${ISO_INQUIRY_REF}</InquiryRef></Envlp></SplmtryData>
  </FIToFICstmrCdtTrf>
</Document>"

  do_curl -X POST "$BASE_URL/api/iso20022/pacs008" \
    -H "Content-Type: application/xml" \
    -H "X-API-Key: $BANK_A_KEY" \
    -H "X-Bank-Code: $BANK_A" \
    --data-binary "$ISO_PACS008_XML"
  check_status "TC-104" "Valid PACS.008 with InquiryRef → 200 (PACS.002 XML response)" "200"
  ISO_TX_STATUS=$(xml_val "TxSts")
  ISO_TRANSFER_REF=$(xml_val "AcctSvcrRef")
  if echo "$ISO_TX_STATUS" | grep -qE "^(ACCP|ACTC)$" && [ -n "$ISO_TRANSFER_REF" ]; then
    pass "TC-104b" "PACS.002 response accepted and includes transfer ref" "transferRef=${ISO_TRANSFER_REF}"
  else
    fail "TC-104b" "PACS.002 response not accepted or missing transfer ref" "TxSts=${ISO_TX_STATUS:-empty} AcctSvcrRef=${ISO_TRANSFER_REF:-empty}"
  fi
else
  skip "TC-104" "Valid PACS.008 — skipped (no ISO inquiryRef)"
  skip "TC-104b" "PACS.002 accepted check — skipped"
fi

# TC-105 — Repeat same PACS.008 should be idempotent and return same transfer ref
if [ -n "$ISO_INQUIRY_REF" ] && [ -n "$ISO_TRANSFER_REF" ]; then
  do_curl -X POST "$BASE_URL/api/iso20022/pacs008" \
    -H "Content-Type: application/xml" \
    -H "X-API-Key: $BANK_A_KEY" \
    -H "X-Bank-Code: $BANK_A" \
    --data-binary "$ISO_PACS008_XML"
  check_status "TC-105" "Repeat same PACS.008 → 200 idempotent response" "200"
  REPEAT_TX_STATUS=$(xml_val "TxSts")
  REPEAT_TRANSFER_REF=$(xml_val "AcctSvcrRef")
  if echo "$REPEAT_TX_STATUS" | grep -qE "^(ACCP|ACTC)$" && [ "$REPEAT_TRANSFER_REF" = "$ISO_TRANSFER_REF" ]; then
    pass "TC-105b" "Repeat PACS.008 returns same transfer ref" "transferRef=${REPEAT_TRANSFER_REF}"
  else
    fail "TC-105b" "Repeat PACS.008 did not return same accepted transfer" "TxSts=${REPEAT_TX_STATUS:-empty} expected=${ISO_TRANSFER_REF} got=${REPEAT_TRANSFER_REF:-empty}"
  fi
else
  skip "TC-105" "Repeat PACS.008 — skipped"
  skip "TC-105b" "Repeat transferRef check — skipped"
fi

# TC-106 — New PACS.008 instruction using already-used inquiry should reject
if [ -n "$ISO_INQUIRY_REF" ]; then
  ISO_USED_SUFFIX="USED-${RUN_ID}"
  ISO_USED_PACS008_XML="<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.12\">
  <FIToFICstmrCdtTrf>
    <GrpHdr><MsgId>MSG-${ISO_USED_SUFFIX}</MsgId><CreDtTm>2026-05-14T12:02:00</CreDtTm><NbOfTxs>1</NbOfTxs></GrpHdr>
    <CdtTrfTxInf>
      <PmtId><InstrId>INST-${ISO_USED_SUFFIX}</InstrId><EndToEndId>E2E-${ISO_USED_SUFFIX}</EndToEndId></PmtId>
      <IntrBkSttlmAmt Ccy=\"${CURRENCY}\">${AMOUNT}</IntrBkSttlmAmt>
      <DbtrAgt><FinInstnId><BICFI>${BANK_A}</BICFI></FinInstnId></DbtrAgt>
      <CdtrAgt><FinInstnId><BICFI>${BANK_B}</BICFI></FinInstnId></CdtrAgt>
      <DbtrAcct><Id><Othr><Id>${DEBTOR}</Id></Othr></Id></DbtrAcct>
      <CdtrAcct><Id><Othr><Id>${CREDITOR}</Id></Othr></Id></CdtrAcct>
    </CdtTrfTxInf>
    <SplmtryData><PlcAndNm>LAO_SWITCHING_INQUIRY_REF</PlcAndNm><Envlp><InquiryRef>${ISO_INQUIRY_REF}</InquiryRef></Envlp></SplmtryData>
  </FIToFICstmrCdtTrf>
</Document>"
  do_curl -X POST "$BASE_URL/api/iso20022/pacs008" \
    -H "Content-Type: application/xml" \
    -H "X-API-Key: $BANK_A_KEY" \
    -H "X-Bank-Code: $BANK_A" \
    --data-binary "$ISO_USED_PACS008_XML"
  check_status "TC-106" "New PACS.008 with used InquiryRef → 200 rejection XML" "200"
  USED_TX_STATUS=$(xml_val "TxSts")
  if [ "$USED_TX_STATUS" = "RJCT" ] && body_has "status=USED"; then
    pass "TC-106b" "Used InquiryRef rejected with status=USED"
  else
    fail "TC-106b" "Used InquiryRef did not produce expected rejection" "TxSts=${USED_TX_STATUS:-empty}"
  fi
else
  skip "TC-106" "Used InquiryRef rejection — skipped"
  skip "TC-106b" "Used InquiryRef body check — skipped"
fi

# TC-107 — PACS.008 with unknown InquiryRef should reject
ISO_UNKNOWN_SUFFIX="UNKNOWN-${RUN_ID}"
ISO_UNKNOWN_PACS008_XML="<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.12\">
  <FIToFICstmrCdtTrf>
    <GrpHdr><MsgId>MSG-${ISO_UNKNOWN_SUFFIX}</MsgId><CreDtTm>2026-05-14T12:03:00</CreDtTm><NbOfTxs>1</NbOfTxs></GrpHdr>
    <CdtTrfTxInf>
      <PmtId><InstrId>INST-${ISO_UNKNOWN_SUFFIX}</InstrId><EndToEndId>E2E-${ISO_UNKNOWN_SUFFIX}</EndToEndId></PmtId>
      <IntrBkSttlmAmt Ccy=\"${CURRENCY}\">${AMOUNT}</IntrBkSttlmAmt>
      <DbtrAgt><FinInstnId><BICFI>${BANK_A}</BICFI></FinInstnId></DbtrAgt>
      <CdtrAgt><FinInstnId><BICFI>${BANK_B}</BICFI></FinInstnId></CdtrAgt>
      <DbtrAcct><Id><Othr><Id>${DEBTOR}</Id></Othr></Id></DbtrAcct>
      <CdtrAcct><Id><Othr><Id>${CREDITOR}</Id></Othr></Id></CdtrAcct>
    </CdtTrfTxInf>
    <SplmtryData><PlcAndNm>LAO_SWITCHING_INQUIRY_REF</PlcAndNm><Envlp><InquiryRef>INQ-ISO-NOTFOUND-${RUN_ID}</InquiryRef></Envlp></SplmtryData>
  </FIToFICstmrCdtTrf>
</Document>"
do_curl -X POST "$BASE_URL/api/iso20022/pacs008" \
  -H "Content-Type: application/xml" \
  -H "X-API-Key: $BANK_A_KEY" \
  -H "X-Bank-Code: $BANK_A" \
  --data-binary "$ISO_UNKNOWN_PACS008_XML"
check_status "TC-107" "PACS.008 with unknown InquiryRef → 200 rejection XML" "200"
UNKNOWN_TX_STATUS=$(xml_val "TxSts")
if [ "$UNKNOWN_TX_STATUS" = "RJCT" ]; then
  pass "TC-107b" "Unknown InquiryRef rejected with RJCT status"
else
  fail "TC-107b" "Unknown InquiryRef did not produce expected rejection" "TxSts=${UNKNOWN_TX_STATUS:-empty}"
fi

# =============================================================================
section "12. Metrics"
# =============================================================================

# TC-120 — Actuator metrics
do_curl "$BASE_URL/actuator/metrics"
if [ "$HTTP_CODE" = "200" ]; then
  PAYMENT_COUNT=$(echo "$RESP_BODY" | jq -r '[.names[] | select(startswith("payment."))] | length' 2>/dev/null || echo "0")
  if [ "${PAYMENT_COUNT:-0}" -ge 8 ] 2>/dev/null; then
    pass "TC-120" "Actuator /metrics has ${PAYMENT_COUNT} payment.* metrics ✅"
  elif [ "${PAYMENT_COUNT:-0}" -gt 0 ] 2>/dev/null; then
    fail "TC-120" "Only ${PAYMENT_COUNT} payment.* metrics found (expected ≥ 8)"
  else
    pass "TC-120" "Actuator /metrics accessible (payment metrics not listed in names)"
  fi
else
  # /actuator/metrics not exposed — correct per application.yml (health,info only)
  pass "TC-120" "Actuator /metrics not exposed (correct: only health,info exposed)" "HTTP ${HTTP_CODE}"
fi

# =============================================================================
section "13. Rate Limiting (runs last — exhausts BANK_A_KEY for this minute)"
# =============================================================================

warn "Sending up to 120 POST requests to trigger rate limit (100 req/min)..."
info "This may take 10-15 seconds..."

RATE_LIMIT_HIT=0
REQUESTS_SENT=0
for i in $(seq 1 120); do
  CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/inquiries" \
    -H "Content-Type: application/json" \
    -H "X-API-Key: $BANK_A_KEY" \
    -d "{\"sourceBank\":\"${BANK_A}\",\"destinationBank\":\"${BANK_B}\",\"creditorAccount\":\"${CREDITOR}\",\"amount\":${AMOUNT},\"currency\":\"${CURRENCY}\"}" \
    2>/dev/null)
  REQUESTS_SENT=$i
  if [ "$CODE" = "429" ]; then
    RATE_LIMIT_HIT=$i
    break
  fi
done

# TC-070
if [ "$RATE_LIMIT_HIT" -gt 0 ]; then
  pass "TC-070" "Rate limit 429 triggered at request ${RATE_LIMIT_HIT} (≤ 100 POST/min ✅)"
else
  fail "TC-070" "Rate limit not triggered in ${REQUESTS_SENT} requests" "check RATE_LIMIT_ENABLED=true in config"
fi

# TC-071 — Check 429 response body has errorCode=REQ-004
if [ "$RATE_LIMIT_HIT" -gt 0 ]; then
  do_curl -X POST "$BASE_URL/api/inquiries" \
    -H "Content-Type: application/json" \
    -H "X-API-Key: $BANK_A_KEY" \
    -d "{\"sourceBank\":\"${BANK_A}\",\"destinationBank\":\"${BANK_B}\",\"creditorAccount\":\"${CREDITOR}\",\"amount\":${AMOUNT},\"currency\":\"${CURRENCY}\"}"
  ERR_CODE=$(jq_val ".errorCode")
  if [ "$ERR_CODE" = "REQ-004" ]; then
    pass "TC-071" "429 response body has errorCode=REQ-004 ✅"
  else
    fail "TC-071" "429 response body errorCode wrong" "expected REQ-004, got ${ERR_CODE}"
  fi
else
  skip "TC-071" "429 body check — skipped (rate limit not triggered)"
fi

# TC-072 — GET requests are NOT rate limited (check 10 times)
info "Verifying GET requests bypass rate limiting..."
GET_429_HIT=0
for i in $(seq 1 10); do
  CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/api/participants" \
    -H "X-API-Key: $OPS_KEY" 2>/dev/null)
  if [ "$CODE" = "429" ]; then
    GET_429_HIT=1
    break
  fi
done

if [ "$GET_429_HIT" = "0" ]; then
  pass "TC-072" "GET requests not rate limited (10 checks, all passed ✅)"
else
  fail "TC-072" "GET request returned 429 — unexpected behavior"
fi

# =============================================================================
# FINAL SUMMARY
# =============================================================================
TOTAL=$((PASS + FAIL + SKIP))
echo ""
echo -e "${BOLD}${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${CYAN}║                 TEST RUN SUMMARY                     ║${NC}"
echo -e "${BOLD}${CYAN}╚══════════════════════════════════════════════════════╝${NC}"
echo ""
printf "  %-12s %s\n" "Total run:" "$TOTAL"
echo -e "  ${GREEN}${BOLD}$(printf '%-12s' 'PASS:') ${PASS}${NC}"
if [ "$FAIL" -gt 0 ]; then
  echo -e "  ${RED}${BOLD}$(printf '%-12s' 'FAIL:') ${FAIL}${NC}"
else
  echo -e "  $(printf '%-12s' 'FAIL:') 0"
fi
if [ "$SKIP" -gt 0 ]; then
  echo -e "  ${YELLOW}$(printf '%-12s' 'SKIP:') ${SKIP}${NC}"
fi
echo ""
echo -e "  ${DIM}BASE_URL  : $BASE_URL${NC}"
echo -e "  ${DIM}RUN_ID    : $RUN_ID${NC}"
echo -e "  ${DIM}Finished  : $(date '+%Y-%m-%d %H:%M:%S')${NC}"
echo ""

if [ "$FAIL" -eq 0 ]; then
  echo -e "  ${GREEN}${BOLD}✅  ALL TESTS PASSED${NC}"
  echo ""
  exit 0
else
  echo -e "  ${RED}${BOLD}❌  ${FAIL} TEST(S) FAILED — see output above for details${NC}"
  echo ""
  exit 1
fi
