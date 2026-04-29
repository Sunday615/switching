# Phase 2B + 2C Test Cases
## ISO 20022 Switching — Routing Integration + Connector Registry

---

## What Was Done

### Phase 2B (already implemented — gaps fixed)
- `CreateTransferService` validates source and destination participant (ACTIVE)
- `CreateTransferService` resolves route via `RoutingService` → gets `routeCode` + `connectorName`
- `routeCode` and `connectorName` are saved on the `TransferEntity` and in outbox payload
- `OutboxIsoMessageDispatchService` reads `connectorName` from payload, resolves connector via `ConnectorRegistry`
- **New:** `GlobalExceptionHandler` now handles `ParticipantNotFoundException` (PRT-001 404), `ParticipantUnavailableException` (PRT-002 422), `RoutingRuleNotFoundException` (RTE-001 422), `ConnectorConfigNotFoundException` (CON-001 503)

### Phase 2C (Connector Registry)
- Created `GenericMockConnector` — handles any MOCK-type connector config
- Created `GenericHttpConnector` — placeholder stub (throws UnsupportedOperationException)
- Created `GenericMqConnector` — placeholder stub (throws UnsupportedOperationException)
- Created `ConnectorRegistry` — resolves `BankConnector` by `connectorName → connectorType → implementation`
- `OutboxIsoMessageDispatchService` now uses `ConnectorRegistry` instead of a hardcoded single `BankConnector`
- `OutboxProcessorService` dead `BankConnector` injection removed
- `MockBankConnector` marked `@Deprecated`, no longer a Spring bean

---

## Seeded Data (from migrations V8, V9)

| Bank     | Status |
|----------|--------|
| BANK_A   | ACTIVE |
| BANK_B   | ACTIVE |
| BANK_C   | ACTIVE |

| Route Code                               | Source  | Destination | Connector               |
|------------------------------------------|---------|-------------|-------------------------|
| ROUTE_BANK_A_TO_BANK_B_PACS008_PRIMARY   | BANK_A  | BANK_B      | MOCK_BANK_B_CONNECTOR   |
| ROUTE_BANK_A_TO_BANK_C_PACS008_PRIMARY   | BANK_A  | BANK_C      | MOCK_BANK_C_CONNECTOR   |
| ROUTE_BANK_B_TO_BANK_A_PACS008_PRIMARY   | BANK_B  | BANK_A      | MOCK_BANK_A_CONNECTOR   |
| ROUTE_BANK_C_TO_BANK_A_PACS008_PRIMARY   | BANK_C  | BANK_A      | MOCK_BANK_A_CONNECTOR   |

| Connector Name         | Bank   | Type | Enabled | ForceReject |
|------------------------|--------|------|---------|-------------|
| MOCK_BANK_A_CONNECTOR  | BANK_A | MOCK | TRUE    | FALSE       |
| MOCK_BANK_B_CONNECTOR  | BANK_B | MOCK | TRUE    | FALSE       |
| MOCK_BANK_C_CONNECTOR  | BANK_C | MOCK | TRUE    | FALSE       |

---

## Test Case 1 — BANK_A → BANK_B Success (Happy Path)

**Step 1: Create inquiry**
```bash
curl -X POST http://localhost:8080/api/inquiries \
  -H "Content-Type: application/json" \
  -d '{
    "sourceBank": "BANK_A",
    "destinationBank": "BANK_B",
    "creditorAccount": "ACC-B-001",
    "amount": 1000.00,
    "currency": "THB",
    "reference": "TEST-TC1"
  }'
```

Expected: `201 Created`, response has `inquiryRef` (e.g. `INQ-...`), `status: ELIGIBLE`

**Step 2: Create transfer using the inquiry ref**
```bash
curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "inquiryRef": "<<INQUIRY_REF_FROM_STEP_1>>",
    "sourceBank": "BANK_A",
    "destinationBank": "BANK_B",
    "debtorAccount": "ACC-A-001",
    "creditorAccount": "ACC-B-001",
    "amount": 1000.00,
    "currency": "THB",
    "reference": "TEST-TC1"
  }'
```

Expected: `201 Created`, `status: RECEIVED`, `transferRef` returned.

**Step 3: Wait ~2 seconds for outbox worker, then check trace**
```bash
curl http://localhost:8080/api/transfers/<<TRANSFER_REF>>/trace
```

Expected in trace:
- `transfer.status = SUCCESS`
- `outboxEvents[0].status = SUCCESS`
- `isoMessages` contains PACS_008 OUTBOUND and PACS_002 INBOUND
- `timeline` includes `PACS002_PARSED`, `TRANSFER_STATUS SUCCESS`
- Route info: `routeCode = ROUTE_BANK_A_TO_BANK_B_PACS008_PRIMARY`, `connectorName = MOCK_BANK_B_CONNECTOR`

---

## Test Case 2 — BANK_A → BANK_C Success

**Step 1: Create inquiry**
```bash
curl -X POST http://localhost:8080/api/inquiries \
  -H "Content-Type: application/json" \
  -d '{
    "sourceBank": "BANK_A",
    "destinationBank": "BANK_C",
    "creditorAccount": "ACC-C-001",
    "amount": 2500.00,
    "currency": "THB",
    "reference": "TEST-TC2"
  }'
```

**Step 2: Create transfer**
```bash
curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "inquiryRef": "<<INQUIRY_REF>>",
    "sourceBank": "BANK_A",
    "destinationBank": "BANK_C",
    "debtorAccount": "ACC-A-001",
    "creditorAccount": "ACC-C-001",
    "amount": 2500.00,
    "currency": "THB",
    "reference": "TEST-TC2"
  }'
```

Expected: `status: RECEIVED`, then after worker: `SUCCESS` via `MOCK_BANK_C_CONNECTOR`

---

## Test Case 3 — Route Not Found (BANK_B → BANK_C)

No routing rule exists for BANK_B → BANK_C.

**Step 1: Create inquiry (will succeed — inquiry doesn't validate routing)**
```bash
curl -X POST http://localhost:8080/api/inquiries \
  -H "Content-Type: application/json" \
  -d '{
    "sourceBank": "BANK_B",
    "destinationBank": "BANK_C",
    "creditorAccount": "ACC-C-001",
    "amount": 500.00,
    "currency": "THB",
    "reference": "TEST-TC3"
  }'
```

**Step 2: Attempt transfer**
```bash
curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "inquiryRef": "<<INQUIRY_REF>>",
    "sourceBank": "BANK_B",
    "destinationBank": "BANK_C",
    "debtorAccount": "ACC-B-001",
    "creditorAccount": "ACC-C-001",
    "amount": 500.00,
    "currency": "THB",
    "reference": "TEST-TC3"
  }'
```

Expected: `422 UNPROCESSABLE_ENTITY`
```json
{
  "errorCode": "RTE-001",
  "error": "UNPROCESSABLE_ENTITY",
  "message": "Routing rule not found. sourceBank=BANK_B, destinationBank=BANK_C, messageType=PACS_008"
}
```

---

## Test Case 4 — Source Bank Not Found

```bash
curl -X POST http://localhost:8080/api/inquiries \
  -H "Content-Type: application/json" \
  -d '{
    "sourceBank": "BANK_X",
    "destinationBank": "BANK_B",
    "creditorAccount": "ACC-B-001",
    "amount": 100.00,
    "currency": "THB"
  }'
```
Then attempt a transfer with `sourceBank: BANK_X`.

Expected: `404 NOT_FOUND`
```json
{
  "errorCode": "PRT-001",
  "error": "NOT_FOUND",
  "message": "Participant not found: BANK_X"
}
```

---

## Test Case 5 — Source Bank Inactive

**Setup (run once in MySQL):**
```sql
UPDATE participants SET status = 'INACTIVE' WHERE bank_code = 'BANK_B';
```

**Step 1: Create inquiry**
```bash
curl -X POST http://localhost:8080/api/inquiries \
  -H "Content-Type: application/json" \
  -d '{
    "sourceBank": "BANK_B",
    "destinationBank": "BANK_A",
    "creditorAccount": "ACC-A-001",
    "amount": 800.00,
    "currency": "THB"
  }'
```

**Step 2: Attempt transfer**
```bash
curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "inquiryRef": "<<INQUIRY_REF>>",
    "sourceBank": "BANK_B",
    "destinationBank": "BANK_A",
    "debtorAccount": "ACC-B-001",
    "creditorAccount": "ACC-A-001",
    "amount": 800.00,
    "currency": "THB"
  }'
```

Expected: `422 UNPROCESSABLE_ENTITY`
```json
{
  "errorCode": "PRT-002",
  "error": "UNPROCESSABLE_ENTITY",
  "message": "Participant is not ACTIVE. bankCode=BANK_B, status=INACTIVE"
}
```

**Teardown:**
```sql
UPDATE participants SET status = 'ACTIVE' WHERE bank_code = 'BANK_B';
```

---

## Test Case 6 — Disabled Connector

**Setup:**
```sql
UPDATE connector_configs SET enabled = FALSE WHERE connector_name = 'MOCK_BANK_B_CONNECTOR';
```

**Run a normal BANK_A → BANK_B transfer (create inquiry + transfer as in TC1)**

Expected: Transfer created (status = RECEIVED), but after worker runs:
- `transfer.status = FAILED`
- `outbox.status = FAILED`
- `errorCode = EXT-001`
- Audit log: `OUTBOX_DISPATCH_FAILED`
- PACS.002 NOT saved (connector rejected before building response)

Check trace:
```bash
curl http://localhost:8080/api/transfers/<<TRANSFER_REF>>/trace
```

**Teardown:**
```sql
UPDATE connector_configs SET enabled = TRUE WHERE connector_name = 'MOCK_BANK_B_CONNECTOR';
```

---

## Test Case 7 — ForceReject Connector Config (PACS.002 RJCT)

**Setup:**
```sql
UPDATE connector_configs
SET force_reject = TRUE,
    reject_reason_code = 'AC04',
    reject_reason_message = 'Closed account - force reject test'
WHERE connector_name = 'MOCK_BANK_C_CONNECTOR';
```

**Run BANK_A → BANK_C transfer (create inquiry + transfer as in TC2)**

Expected: Transfer created, then after worker:
- `transfer.status = FAILED`
- `outbox.status = FAILED`
- `errorCode = EXT-001`
- A PACS.002 INBOUND message IS saved with `TxSts = RJCT`
- Audit: `PACS002_PARSED`, `TRANSFER_STATUS FAILED`

```bash
curl http://localhost:8080/api/transfers/<<TRANSFER_REF>>/trace
```

**Teardown:**
```sql
UPDATE connector_configs
SET force_reject = FALSE
WHERE connector_name = 'MOCK_BANK_C_CONNECTOR';
```

---

## Quick Verification Commands

**List participants:**
```bash
curl http://localhost:8080/api/participants
```

**List routing rules:**
```bash
curl http://localhost:8080/api/routing-rules
```

**Resolve a route:**
```bash
curl "http://localhost:8080/api/routing-rules/resolve?sourceBank=BANK_A&destinationBank=BANK_B&messageType=PACS_008"
```

**List connector configs:**
```bash
curl http://localhost:8080/api/connector-configs
```

**List outbox events:**
```bash
curl http://localhost:8080/api/outbox-events
```

**List ISO messages:**
```bash
curl http://localhost:8080/api/iso-messages
```

**Clear routing cache (after SQL updates):**
```bash
curl -X POST http://localhost:8080/api/routing-rules/cache/clear
```

---

## Files Changed in This Session

| File | Type | What Changed |
|------|------|--------------|
| `common/error/ErrorCatalog.java` | Modified | Added PRT_001, PRT_002, RTE_001, CON_001 |
| `common/exception/GlobalExceptionHandler.java` | Modified | Added 4 exception handlers |
| `connector/GenericMockConnector.java` | **New** | Generic MOCK connector (replaces per-bank mocks) |
| `connector/GenericHttpConnector.java` | **New** | HTTP connector placeholder stub |
| `connector/GenericMqConnector.java` | **New** | MQ connector placeholder stub |
| `connector/registry/ConnectorRegistry.java` | **New** | Resolves BankConnector by connectorType |
| `connector/MockBankConnector.java` | Modified | Marked @Deprecated, removed @Component |
| `outbox/service/OutboxIsoMessageDispatchService.java` | Modified | Uses ConnectorRegistry instead of BankConnector |
| `outbox/service/OutboxProcessorService.java` | Modified | Removed dead BankConnector injection |

---

## Next Phase: Phase 3 — Participant & Routing Management APIs

After these tests pass, the recommended next step is adding management APIs:

```
POST   /api/participants
PATCH  /api/participants/{bankCode}/status
POST   /api/routing-rules
PATCH  /api/routing-rules/{routeCode}
POST   /api/connector-configs
PATCH  /api/connector-configs/{connectorName}
```

This enables operations team to manage config without direct SQL access, which is a prerequisite for the Web Portal admin features.
