# Phase 3 Test Cases — Management APIs

---

## APIs Added

| Method | Path | Description |
|--------|------|-------------|
| POST   | /api/participants | Create new participant |
| PATCH  | /api/participants/{bankCode} | Update participant (name/status/country/currency) |
| POST   | /api/routing-rules | Create new routing rule |
| PATCH  | /api/routing-rules/{routeCode} | Update routing rule (connector/priority/enabled) |
| POST   | /api/connector-configs | Create new connector config |
| PATCH  | /api/connector-configs/{connectorName} | Update connector config |

---

## PARTICIPANT MANAGEMENT

### TC-P1 — Create new participant (success)
```bash
curl -X POST http://localhost:8080/api/participants \
  -H "Content-Type: application/json" \
  -d '{
    "bankCode": "BANK_D",
    "bankName": "Demo Bank D",
    "status": "ACTIVE",
    "participantType": "BANK",
    "country": "TH",
    "currency": "THB"
  }'
```
Expected: `201 Created`
```json
{
  "bankCode": "BANK_D",
  "bankName": "Demo Bank D",
  "status": "ACTIVE",
  "participantType": "BANK",
  "country": "TH",
  "currency": "THB"
}
```

### TC-P2 — Create duplicate participant (conflict)
```bash
curl -X POST http://localhost:8080/api/participants \
  -H "Content-Type: application/json" \
  -d '{
    "bankCode": "BANK_A",
    "bankName": "Duplicate",
    "country": "TH",
    "currency": "THB"
  }'
```
Expected: `409 CONFLICT`
```json
{ "errorCode": "PRT-003", "message": "Participant already exists: BANK_A" }
```

### TC-P3 — Update participant status to INACTIVE
```bash
curl -X PATCH http://localhost:8080/api/participants/BANK_D \
  -H "Content-Type: application/json" \
  -d '{ "status": "INACTIVE" }'
```
Expected: `200 OK`, `status: INACTIVE`

### TC-P4 — Update back to ACTIVE
```bash
curl -X PATCH http://localhost:8080/api/participants/BANK_D \
  -H "Content-Type: application/json" \
  -d '{ "status": "ACTIVE" }'
```
Expected: `200 OK`, `status: ACTIVE`

### TC-P5 — Update bankName only
```bash
curl -X PATCH http://localhost:8080/api/participants/BANK_D \
  -H "Content-Type: application/json" \
  -d '{ "bankName": "Demo Bank D (Updated)" }'
```
Expected: `200 OK`, `bankName: "Demo Bank D (Updated)"`

### TC-P6 — Create participant with missing required field
```bash
curl -X POST http://localhost:8080/api/participants \
  -H "Content-Type: application/json" \
  -d '{ "bankCode": "BANK_E" }'
```
Expected: `400 BAD_REQUEST`, `"bankName is required"`

### TC-P7 — Create participant, bank not found on update
```bash
curl -X PATCH http://localhost:8080/api/participants/BANK_Z \
  -H "Content-Type: application/json" \
  -d '{ "status": "INACTIVE" }'
```
Expected: `404 NOT_FOUND`, `errorCode: PRT-001`

---

## ROUTING RULE MANAGEMENT

### TC-R1 — Create new routing rule for BANK_A → BANK_D
```bash
curl -X POST http://localhost:8080/api/routing-rules \
  -H "Content-Type: application/json" \
  -d '{
    "routeCode": "ROUTE_BANK_A_TO_BANK_D_PACS008_PRIMARY",
    "sourceBank": "BANK_A",
    "destinationBank": "BANK_D",
    "messageType": "PACS_008",
    "connectorName": "MOCK_BANK_D_CONNECTOR",
    "priority": 1,
    "enabled": true
  }'
```
Expected: `201 Created`
```json
{
  "routeCode": "ROUTE_BANK_A_TO_BANK_D_PACS008_PRIMARY",
  "sourceBank": "BANK_A",
  "destinationBank": "BANK_D",
  "messageType": "PACS_008",
  "connectorName": "MOCK_BANK_D_CONNECTOR",
  "priority": 1,
  "enabled": true
}
```

### TC-R2 — Create duplicate routeCode (conflict)
```bash
curl -X POST http://localhost:8080/api/routing-rules \
  -H "Content-Type: application/json" \
  -d '{
    "routeCode": "ROUTE_BANK_A_TO_BANK_B_PACS008_PRIMARY",
    "sourceBank": "BANK_A",
    "destinationBank": "BANK_B",
    "messageType": "PACS_008",
    "connectorName": "MOCK_BANK_B_CONNECTOR"
  }'
```
Expected: `409 CONFLICT`
```json
{ "errorCode": "RTE-002", "message": "Routing rule already exists: ROUTE_BANK_A_TO_BANK_B_PACS008_PRIMARY" }
```

### TC-R3 — Disable a routing rule
```bash
curl -X PATCH "http://localhost:8080/api/routing-rules/ROUTE_BANK_A_TO_BANK_D_PACS008_PRIMARY" \
  -H "Content-Type: application/json" \
  -d '{ "enabled": false }'
```
Expected: `200 OK`, `enabled: false`

### TC-R4 — Re-enable routing rule
```bash
curl -X PATCH "http://localhost:8080/api/routing-rules/ROUTE_BANK_A_TO_BANK_D_PACS008_PRIMARY" \
  -H "Content-Type: application/json" \
  -d '{ "enabled": true }'
```
Expected: `200 OK`, `enabled: true`

### TC-R5 — Resolve route after new rule created
```bash
curl "http://localhost:8080/api/routing-rules/resolve?sourceBank=BANK_A&destinationBank=BANK_D&messageType=PACS_008"
```
Expected: `routeCode: ROUTE_BANK_A_TO_BANK_D_PACS008_PRIMARY`, `connectorName: MOCK_BANK_D_CONNECTOR`

### TC-R6 — Create rule with invalid messageType
```bash
curl -X POST http://localhost:8080/api/routing-rules \
  -H "Content-Type: application/json" \
  -d '{
    "routeCode": "ROUTE_TEST",
    "sourceBank": "BANK_A",
    "destinationBank": "BANK_B",
    "messageType": "INVALID_TYPE",
    "connectorName": "MOCK_BANK_B_CONNECTOR"
  }'
```
Expected: `400 BAD_REQUEST`, `"Invalid messageType: INVALID_TYPE"`

### TC-R7 — Create rule where sourceBank not in participants
```bash
curl -X POST http://localhost:8080/api/routing-rules \
  -H "Content-Type: application/json" \
  -d '{
    "routeCode": "ROUTE_GHOST_TO_BANK_B",
    "sourceBank": "GHOST_BANK",
    "destinationBank": "BANK_B",
    "messageType": "PACS_008",
    "connectorName": "MOCK_BANK_B_CONNECTOR"
  }'
```
Expected: `404 NOT_FOUND`, `errorCode: PRT-001`, `"Participant not found: GHOST_BANK"`

---

## CONNECTOR CONFIG MANAGEMENT

### TC-C1 — Create new connector config for BANK_D
```bash
curl -X POST http://localhost:8080/api/connector-configs \
  -H "Content-Type: application/json" \
  -d '{
    "connectorName": "MOCK_BANK_D_CONNECTOR",
    "bankCode": "BANK_D",
    "connectorType": "MOCK",
    "timeoutMs": 5000,
    "enabled": true,
    "forceReject": false,
    "rejectReasonCode": "AC01",
    "rejectReasonMessage": "Mock Bank D rejected transfer"
  }'
```
Expected: `201 Created`

### TC-C2 — Create duplicate connector (conflict)
```bash
curl -X POST http://localhost:8080/api/connector-configs \
  -H "Content-Type: application/json" \
  -d '{
    "connectorName": "MOCK_BANK_A_CONNECTOR",
    "bankCode": "BANK_A",
    "connectorType": "MOCK"
  }'
```
Expected: `409 CONFLICT`
```json
{ "errorCode": "CON-002", "message": "Connector config already exists: MOCK_BANK_A_CONNECTOR" }
```

### TC-C3 — Disable a connector
```bash
curl -X PATCH http://localhost:8080/api/connector-configs/MOCK_BANK_D_CONNECTOR \
  -H "Content-Type: application/json" \
  -d '{ "enabled": false }'
```
Expected: `200 OK`, `enabled: false`

### TC-C4 — Enable forceReject for testing
```bash
curl -X PATCH http://localhost:8080/api/connector-configs/MOCK_BANK_D_CONNECTOR \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true,
    "forceReject": true,
    "rejectReasonCode": "AC04",
    "rejectReasonMessage": "Closed account - force reject"
  }'
```
Expected: `200 OK`, `forceReject: true`

### TC-C5 — Disable forceReject (restore normal)
```bash
curl -X PATCH http://localhost:8080/api/connector-configs/MOCK_BANK_D_CONNECTOR \
  -H "Content-Type: application/json" \
  -d '{ "forceReject": false }'
```
Expected: `200 OK`, `forceReject: false`

### TC-C6 — Update timeout
```bash
curl -X PATCH http://localhost:8080/api/connector-configs/MOCK_BANK_B_CONNECTOR \
  -H "Content-Type: application/json" \
  -d '{ "timeoutMs": 10000 }'
```
Expected: `200 OK`, `timeoutMs: 10000`

### TC-C7 — Create connector with bankCode not in participants
```bash
curl -X POST http://localhost:8080/api/connector-configs \
  -H "Content-Type: application/json" \
  -d '{
    "connectorName": "MOCK_GHOST_CONNECTOR",
    "bankCode": "GHOST_BANK",
    "connectorType": "MOCK"
  }'
```
Expected: `404 NOT_FOUND`, `errorCode: PRT-001`, `"Participant not found: GHOST_BANK"`

### TC-C8 — Connector not found on update
```bash
curl -X PATCH http://localhost:8080/api/connector-configs/NO_EXIST_CONNECTOR \
  -H "Content-Type: application/json" \
  -d '{ "enabled": false }'
```
Expected: `503 SERVICE_UNAVAILABLE`, `errorCode: CON-001`

---

## END-TO-END FLOW — Add BANK_D and do a full transfer

สร้าง BANK_D ใหม่ ทำ routing + connector แล้ว transfer จาก BANK_A → BANK_D

```bash
# 1. สร้าง participant
curl -X POST http://localhost:8080/api/participants \
  -H "Content-Type: application/json" \
  -d '{"bankCode":"BANK_D","bankName":"Demo Bank D","country":"TH","currency":"THB"}'

# 2. สร้าง connector config
curl -X POST http://localhost:8080/api/connector-configs \
  -H "Content-Type: application/json" \
  -d '{"connectorName":"MOCK_BANK_D_CONNECTOR","bankCode":"BANK_D","connectorType":"MOCK","enabled":true,"forceReject":false}'

# 3. สร้าง routing rule
curl -X POST http://localhost:8080/api/routing-rules \
  -H "Content-Type: application/json" \
  -d '{"routeCode":"ROUTE_BANK_A_TO_BANK_D_PACS008_PRIMARY","sourceBank":"BANK_A","destinationBank":"BANK_D","messageType":"PACS_008","connectorName":"MOCK_BANK_D_CONNECTOR","priority":1,"enabled":true}'

# 4. สร้าง inquiry
curl -X POST http://localhost:8080/api/inquiries \
  -H "Content-Type: application/json" \
  -d '{"sourceBank":"BANK_A","destinationBank":"BANK_D","creditorAccount":"ACC-D-001","amount":500.00,"currency":"THB"}'

# 5. สร้าง transfer (ใส่ inquiryRef จากข้อ 4)
curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -d '{"inquiryRef":"<<INQ_REF>>","sourceBank":"BANK_A","destinationBank":"BANK_D","debtorAccount":"ACC-A-001","creditorAccount":"ACC-D-001","amount":500.00,"currency":"THB"}'

# 6. ตรวจสอบ trace (รอ worker ~2 วิ)
curl http://localhost:8080/api/transfers/<<TRANSFER_REF>>/trace
```

Expected ทุก step ผ่าน → transfer status = SUCCESS ผ่าน MOCK_BANK_D_CONNECTOR

---

## Files Changed (Phase 3)

| File | Type |
|------|------|
| `ErrorCatalog.java` | แก้ไข — เพิ่ม PRT-003, RTE-002, CON-002 |
| `GlobalExceptionHandler.java` | แก้ไข — เพิ่ม 3 handlers ใหม่ |
| `ParticipantController.java` | แก้ไข — เพิ่ม POST + PATCH |
| `CreateParticipantRequest.java` | ใหม่ |
| `UpdateParticipantRequest.java` | ใหม่ |
| `ParticipantAlreadyExistsException.java` | ใหม่ |
| `ParticipantManagementService.java` | ใหม่ |
| `RoutingRuleController.java` | แก้ไข — เพิ่ม POST + PATCH |
| `CreateRoutingRuleRequest.java` | ใหม่ |
| `UpdateRoutingRuleRequest.java` | ใหม่ |
| `RoutingRuleAlreadyExistsException.java` | ใหม่ |
| `RoutingRuleManagementService.java` | ใหม่ |
| `ConnectorConfigController.java` | แก้ไข — เพิ่ม POST + PATCH |
| `CreateConnectorConfigRequest.java` | ใหม่ |
| `UpdateConnectorConfigRequest.java` | ใหม่ |
| `ConnectorConfigAlreadyExistsException.java` | ใหม่ |
| `ConnectorConfigManagementService.java` | ใหม่ |
