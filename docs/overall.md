# Switching API — Overall Project Reference

> **Purpose of this document:** Comprehensive reference for AI agents working on this codebase.
> Covers architecture, domain model, APIs, DB schema, services, workers, config, tests, known issues, and next steps.
> Last updated: 2026-05-14 — Phase 0 baseline freeze started: risk register + production checklist created

---

## 1. Project Overview

**Switching API** is a **Payment Switching System** — an intermediary that routes cross-bank money transfers following the **ISO 20022** financial messaging standard.

```
Source Bank (BANK_A) ──► Switching API ──► Destination Bank (BANK_B)
                         (this system)
```

The system:
- Receives ISO 20022 XML messages (PACS.008 credit transfer, ACMT.023 account inquiry)
- Validates participants, resolves routing rules, creates a transfer record
- Persists an outbox event, dispatches asynchronously to the destination bank's connector
- Receives PACS.002 response, updates transfer status
- Provides full tracing/audit of every transfer step

---

## 2. Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.3 (Spring Framework 6) |
| Web | Spring MVC (spring-boot-starter-webmvc) |
| Persistence | Spring Data JPA + Hibernate 7 |
| Database | MySQL 8.x / 9.x |
| Migrations | Flyway (12 migrations, V1–V13, no V12) |
| Validation | Jakarta Bean Validation (spring-boot-starter-validation) |
| Security | Spring Security 6 + API Key (X-API-Key header) + AES/GCM/NoPadding |
| JSON | Jackson + jackson-datatype-jsr310 |
| Monitoring | Spring Actuator (health, info only) + Micrometer metrics (counters, timers, gauges) |
| Build | Maven (mvnw wrapper) |
| Container | Docker + Docker Compose |
| Tests | JUnit 5 + MockMvc + Spring Boot Test |
| Code gen | Lombok |

---

## 3. Project Structure

```
src/main/java/com/example/switching/
├── SwitchingApplication.java              # Entry point
├── config/SchedulingConfig.java           # @EnableScheduling
├── audit/                                 # Audit log module
│   ├── controller/AuditLogController.java
│   ├── entity/AuditLogEntity.java
│   ├── repository/AuditLogRepository.java
│   └── service/AuditLogService.java       # Core: log(eventType, refType, refId, actor, payload)
│                AuditLogQueryService.java
├── common/
│   ├── dto/ApiErrorResponse.java          # Standard error response body
│   ├── error/ErrorCatalog.java            # All error codes (REQ, INQ, TRF, OUT, NET, EXT, INF, SYS, etc.)
│   │        ErrorCategory.java           # REQUEST, BUSINESS, CORE, NETWORK, DOWNSTREAM, INFRASTRUCTURE, UNKNOWN
│   │        ErrorClassifier.java         # Maps exceptions to ErrorCatalog entries
│   │        ErrorLayer.java              # API, INQUIRY, TRANSFER, OUTBOX, CONNECTOR, ISO, DATABASE, SYSTEM, ROUTING, WORKER
│   │        ErrorPhase.java              # RECEIVE_REQUEST, VALIDATE_REQUEST, LOOKUP_INQUIRY, CREATE_TRANSFER, ...
│   ├── exception/GlobalExceptionHandler.java   # @RestControllerAdvice — maps all known exceptions to ApiErrorResponse
│   ├── filter/RequestIdFilter.java        # Injects X-Request-Id header (UUID if not provided); sets MDC.requestId for log correlation
│   └── util/RequestHashUtil.java         # SHA-256 hash of request for idempotency
│              TransferRefGenerator.java  # Generates "TRX-{timestamp}-{random}" refs
├── connector/                             # Bank connector abstraction
│   ├── BankConnector.java                 # Interface: dispatch(DispatchIsoMessageCommand) → BankIsoDispatchResponse
│   ├── MockBankConnector.java             # MOCK implementation, reads connector_configs.force_reject
│   ├── registry/ConnectorRegistry.java   # Map<connectorName, BankConnector>
│   ├── entity/ConnectorConfigEntity.java
│   ├── enums/ConnectorType.java           # MOCK (only type currently)
│   ├── controller/ConnectorConfigController.java
│   └── service/ConnectorConfigService.java
│              ConnectorConfigManagementService.java
├── dashboard/                             # Dashboard summary
├── demo/                                  # DemoFlowService (unused in prod)
├── idempotency/                           # Idempotency layer
│   ├── entity/IdempotencyRecordEntity.java  # Table: idempotency_records
│   ├── repository/IdempotencyRecordRepository.java
│   └── service/IdempotencyService.java    # findExistingTransfer(), saveNew(), updateStatus()
├── inquiry/                               # JSON-based inquiry (non-ISO path)
│   ├── controller/InquiryController.java  # POST /api/inquiries, GET /api/inquiries/{inquiryRef}
│   ├── entity/InquiryEntity.java          # Table: inquiries
│   ├── enums/InquiryStatus.java           # RECEIVED, ELIGIBLE, NOT_ELIGIBLE, FAILED
│   └── service/CreateInquiryService.java
│              InquiryLookupService.java
├── security/                              # API Key authentication
│   ├── config/SecurityConfig.java         # Spring Security config, role-based access rules
│   ├── entity/ApiKeyEntity.java           # Table: api_keys
│   ├── enums/ApiKeyRole.java              # ADMIN, OPS, BANK
│   ├── filter/ApiKeyAuthFilter.java       # OncePerRequestFilter — reads X-API-Key, sets SecurityContext
│   └── repository/ApiKeyRepository.java  # findByKeyValueAndEnabledTrue()
├── iso/                                   # ISO 20022 module
│   ├── controller/IsoMessageController.java    # GET /api/iso-messages, /api/iso-messages/{key}
│   ├── entity/IsoMessageEntity.java            # Table: iso_messages
│   ├── enums/IsoMessageType.java               # PACS_008, PACS_002, PACS_028, PACS_004
│   │        IsoSecurityStatus.java             # ENCRYPTED, DECRYPTED, BYPASS, FAILED
│   │        IsoValidationStatus.java           # VALID, INVALID, BYPASS
│   ├── inbound/IsoPacs008InboundController.java  # POST /api/iso20022/pacs008 (XML)
│   │           IsoPacs008InboundService.java     # Full ISO inbound flow
│   │           InboundPacs008PersistenceService.java
│   ├── inquiry/IsoInquiryController.java        # POST /api/iso20022/acmt023 (XML)
│   │           IsoInquiryInboundService.java    # Parse ACMT.023, validate, create iso_inquiry
│   │           IsoInquiryQueryController.java   # GET /api/iso-inquiries/{ref}
│   │           Acmt023XmlParser.java            # XML → Acmt023InquiryRequest
│   │           Acmt024XmlResponseBuilder.java   # Build ACMT.024 XML response
│   ├── parser/Pacs002Parser.java                # Parse PACS.002 XML response
│   │           IsoXmlValidator.java
│   └── security/IsoMessageCryptoService.java    # AES/GCM encrypt/decrypt
├── operations/                            # Ops/admin APIs
│   ├── controller/ (14 controllers)       # All under /api/operations/*
│   └── service/ (14 services)
├── outbox/                                # Transactional Outbox
│   ├── entity/OutboxEventEntity.java      # Table: outbox_events
│   ├── enums/OutboxStatus.java            # PENDING, PROCESSING, SUCCESS, FAILED, REVIEWED
│   ├── event/OutboxCreatedEvent.java      # record OutboxCreatedEvent(Long outboxEventId, String transferRef)
│   ├── worker/OutboxDispatchWorker.java   # @TransactionalEventListener(AFTER_COMMIT) near real-time + @Scheduled safety-net (30s)
│   │           OutboxRecoveryWorker.java  # @Scheduled configurable (60s default) — recovers stuck PROCESSING
│   └── service/OutboxProcessorService.java     # Core dispatch logic, maxRetry configurable via env
│              OutboxIsoMessageDispatchService.java  # decrypt → dispatch via connector → handle PACS.002
│              OutboxTransactionService.java     # Creates outbox event in same transaction as transfer
│              OutboxRecoveryService.java
│              OutboxManualRetryService.java
├── participant/                           # Bank participant management
│   ├── entity/ParticipantEntity.java      # Table: participants
│   ├── enums/ParticipantStatus.java       # ACTIVE, INACTIVE, SUSPENDED
│   ├── controller/ParticipantController.java  # GET/POST/PATCH /api/participants
│   └── service/ParticipantService.java    # Validates participant is ACTIVE before routing
├── routing/                               # Routing rule management
│   ├── entity/RoutingRuleEntity.java      # Table: routing_rules
│   ├── controller/RoutingRuleController.java  # GET/POST/PATCH /api/routing-rules
│   └── service/RoutingService.java        # resolve(sourceBank, destBank, msgType) with in-memory ConcurrentHashMap cache
│              RoutingRuleManagementService.java
└── transfer/                              # Transfer domain (core)
    ├── controller/TransferController.java      # POST /api/transfers
    │             TransferListController.java   # GET /api/transfers
    │             TransferTraceController.java  # GET /api/transfers/{ref}/trace
    │             TransferInquiryController.java
    ├── entity/TransferEntity.java              # Table: transfers
    │           TransferStatusHistoryEntity.java # Table: transfer_status_history
    ├── enums/TransferStatus.java               # RECEIVED, SUCCESS, FAILED
    └── service/CreateTransferService.java      # Core transfer creation + outbox
                TransferListService.java
                TransferTraceService.java
                TransferInquiryService.java
```

---

## 4. Payment Flow (Step by Step)

### 4.1 JSON Path (non-ISO, simpler)

```
POST /api/inquiries
  → CreateInquiryService
  → Validate participants (sourceBank must be ACTIVE)
  → Resolve routing rule (sourceBank + destBank + PACS_008)
  → Save inquiry (table: inquiries, status=ELIGIBLE)
  → Return { inquiryRef, status, ... }

POST /api/transfers  { inquiryRef: "INQ-xxx" }
  → CreateTransferService
  → [Idempotency check] if idempotencyKey already exists → return existing transfer
  → Load inquiry, validate (status=ELIGIBLE, amount match, currency match, sourceBank match)
  → Mark inquiry as USED (status=ELIGIBLE→USED via inquiry_status_history)
  → Create TransferEntity (status=RECEIVED)
  → Create IsoMessageEntity (PACS_008, direction=OUTBOUND)
  → [OutboxTransactionService] Create OutboxEventEntity (status=PENDING) in SAME transaction
  → Save IdempotencyRecord
  → Return { transferRef, status=RECEIVED }
```

### 4.2 ISO XML Path (PACS.008 inbound)

```
POST /api/iso20022/pacs008  (Content-Type: application/xml)
  Header: X-Bank-Code: BANK_A
  Body: <PACS.008 XML>
  → IsoPacs008InboundService
  → IsoMessageCryptoService.encrypt(xmlBody) → store encrypted payload
  → InboundPacs008PersistenceService → save iso_messages record
  → Create Transfer + Outbox event
  → Return PACS.002 XML response
```

### 4.3 ISO XML Path (ACMT.023 inquiry inbound)

```
POST /api/iso20022/acmt023  (Content-Type: application/xml)
  Header: X-Bank-Code: BANK_A
  Body: <ACMT.023 XML>
  → IsoInquiryInboundService
  → Acmt023XmlParser.parse(xmlBody)
  → Validate X-Bank-Code matches source bank
  → Check participant is ACTIVE
  → Save to iso_inquiries (status=USED initially, eligible_for_transfer=true, TTL=15min)
  → Return ACMT.024 XML response
```

### 4.4 Outbox Dispatch Flow

```
[Near real-time path — ~50-200ms after transfer commit]
OutboxTransactionService.enqueueTransferDispatch()
  → Saves OutboxEventEntity (status=PENDING) in SAME transaction as transfer
  → Publishes OutboxCreatedEvent(outboxEventId, transferRef) via ApplicationEventPublisher

OutboxDispatchWorker.onOutboxCreated(@TransactionalEventListener AFTER_COMMIT)
  → Fires immediately after outer transaction commits (not before — prevents race condition)
  → OutboxProcessorService.processSingleEvent(id)
    → Claim event (PENDING → PROCESSING) in separate REQUIRES_NEW transaction
    → OutboxIsoMessageDispatchService.dispatchEncryptedIsoMessage(payload)
      → Decrypt PACS.008 payload (IsoMessageCryptoService.decrypt)
      → ConnectorRegistry.getConnector(connectorName)
      → MockBankConnector.dispatch(command)
        → If connector_configs.force_reject=true → return REJECT
        → Else → return ACCEPT (simulated PACS.002 response)
      → Parse PACS.002 response
    → On success: OutboxEvent → SUCCESS, Transfer → SUCCESS
    → On retryable failure (retryCount < maxRetry): event → PENDING, retryCount++
    → On terminal failure (>= maxRetry or non-retryable): event → FAILED, Transfer → FAILED

[Safety-net poll — catches events missed by listener (app restart, listener crash)]
OutboxDispatchWorker.processPendingEvents(@Scheduled fixedDelayString=poll-interval-ms:30000)
  → Finds top batchSize PENDING events → processes each via processSingleEvent()

[Recovery — unsticks long-running PROCESSING events]
OutboxRecoveryWorker(@Scheduled fixedDelayString=recovery-interval-ms:60000)
  → Finds PROCESSING events stuck > stuck-timeout-minutes (default 2)
  → If retryCount < maxRetry → reset to PENDING
  → If retryCount >= maxRetry → mark FAILED, set Transfer → FAILED
```

---

## 5. Database Schema

### Tables (13 migrations, current version = 13)

#### `transfers`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| transfer_ref | VARCHAR(255) UNI | `TRX-{ts}-{rand}` |
| client_transfer_id | VARCHAR(255) | |
| idempotency_key | VARCHAR(100) | |
| source_bank_code | VARCHAR(20) | |
| source_account_no | VARCHAR(40) | |
| destination_bank_code | VARCHAR(20) | |
| destination_account_no | VARCHAR(40) | |
| destination_account_name | VARCHAR(120) | nullable |
| amount | DECIMAL(38,2) | nullable |
| currency | VARCHAR(255) | nullable |
| channel_id | VARCHAR(40) | e.g. `API`, `ISO20022_XML` |
| route_code | VARCHAR(40) | nullable |
| connector_name | VARCHAR(80) | nullable |
| external_reference | VARCHAR(80) | nullable |
| status | VARCHAR(30) | `RECEIVED`, `SUCCESS`, `FAILED` |
| error_code | VARCHAR(40) | nullable |
| error_message | TEXT | nullable |
| reference | VARCHAR(255) | nullable |
| inquiry_ref | VARCHAR(64) UNI | nullable — added in V4 |
| created_at | DATETIME(3) | auto |
| updated_at | DATETIME(3) | auto on update |

#### `transfer_status_history`
| Column | Type |
|--------|------|
| id | BIGINT PK |
| transfer_ref | VARCHAR(255) |
| status | VARCHAR(30) |
| reason_code | VARCHAR(40) |
| created_at | DATETIME(3) |

#### `inquiries` (V3 — JSON inquiry path)
| Column | Type |
|--------|------|
| id | BIGINT PK |
| inquiry_ref | VARCHAR(255) UNI |
| client_inquiry_id | VARCHAR(255) |
| source_bank | VARCHAR(20) |
| destination_bank | VARCHAR(20) |
| creditor_account | VARCHAR(40) |
| destination_account_name | VARCHAR(120) |
| amount | DECIMAL(38,2) |
| currency | VARCHAR(20) |
| channel_id | VARCHAR(40) |
| route_code | VARCHAR(40) |
| connector_name | VARCHAR(80) |
| account_found | BOOLEAN |
| bank_available | BOOLEAN |
| eligible_for_transfer | BOOLEAN |
| status | VARCHAR(30) |
| error_code / error_message | VARCHAR |
| reference | VARCHAR(255) |
| created_at / updated_at | DATETIME(3) |

#### `iso_inquiries` (V13 — ISO XML inquiry path)
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| inquiry_ref | VARCHAR(80) UNI | |
| channel_id | VARCHAR(50) | `ISO20022_XML` |
| message_id | VARCHAR(120) | UNI with channel_id |
| instruction_id | VARCHAR(120) | nullable |
| end_to_end_id | VARCHAR(120) | nullable |
| source_bank_code | VARCHAR(30) | |
| destination_bank_code | VARCHAR(30) | |
| debtor_account_no | VARCHAR(60) | nullable |
| creditor_account_no | VARCHAR(60) | |
| amount | DECIMAL(19,2) | nullable |
| currency | VARCHAR(10) | nullable |
| reference | VARCHAR(255) | nullable |
| status | VARCHAR(30) | `USED`, `ELIGIBLE`, etc. |
| account_found | BOOLEAN | default false |
| bank_available | BOOLEAN | default false |
| eligible_for_transfer | BOOLEAN | default false |
| failure_code | VARCHAR(30) | nullable |
| failure_message | TEXT | nullable |
| expires_at | DATETIME | nullable, TTL=15min from creation |
| used_by_transfer_ref | VARCHAR(80) | nullable, INDEX |
| created_at / updated_at | DATETIME | |

#### `outbox_events`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| transfer_ref | VARCHAR(255) | |
| message_type | VARCHAR(100) | e.g. `PACS_008` |
| payload | LONGTEXT | JSON: `{transferRef, isoMessageId, sourceBank, destBank, routeCode, connectorName}` |
| status | VARCHAR(20) | `PENDING`→`PROCESSING`→`SUCCESS`/`FAILED`/`REVIEWED` |
| retry_count | INT | max 3 |
| last_error | TEXT | nullable |
| processed_at | DATETIME(3) | nullable |
| next_retry_at | DATETIME(3) | nullable |
| created_at / updated_at | DATETIME(3) | |

#### `iso_messages`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| correlation_ref | VARCHAR(100) | |
| inquiry_ref | VARCHAR(100) | nullable |
| transfer_ref | VARCHAR(100) | nullable |
| end_to_end_id | VARCHAR(100) | |
| message_id | VARCHAR(100) | |
| message_type | VARCHAR(50) | `PACS_008`, `PACS_002`, etc. |
| direction | VARCHAR(20) | `INBOUND` / `OUTBOUND` |
| plain_payload | LONGTEXT | decrypted XML |
| encrypted_payload | LONGTEXT | AES/GCM encrypted + base64 |
| security_status | VARCHAR(30) | `ENCRYPTED`, `DECRYPTED`, `BYPASS`, `FAILED` |
| validation_status | VARCHAR(30) | `VALID`, `INVALID`, `BYPASS` |
| error_code / error_message | VARCHAR | |
| created_at | DATETIME | |

#### `audit_logs`
| Column | Type |
|--------|------|
| id | BIGINT PK |
| event_type | VARCHAR(60) |
| reference_type | VARCHAR(40) |
| reference_id | VARCHAR(60) |
| actor | VARCHAR(60) |
| payload | LONGTEXT |
| created_at | DATETIME(3) |

#### `idempotency_records`
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| idempotency_key | VARCHAR(100) UNI | |
| channel_id | VARCHAR(40) | |
| request_hash | VARCHAR(128) | SHA-256 of request body |
| transfer_ref | VARCHAR(255) | |
| status | VARCHAR(30) | |
| created_at | DATETIME(3) | |
| expired_at | DATETIME(3) | nullable |

**Note:** V7 added `updated_at` to `idempotency_records`. V7 also adds `channel_id` to the unique lookup (findByChannelIdAndIdempotencyKey).

#### `participants`
| Column | Type |
|--------|------|
| id | BIGINT PK |
| bank_code | VARCHAR(32) UNI |
| bank_name | VARCHAR(255) |
| status | VARCHAR(32) | `ACTIVE`, `INACTIVE`, `SUSPENDED` |
| participant_type | VARCHAR(32) |
| country | VARCHAR(8) |
| currency | VARCHAR(8) |
| created_at / updated_at | DATETIME |

#### `routing_rules` (V8 — replaces V1 version)
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| route_code | VARCHAR(128) UNI | |
| source_bank | VARCHAR(32) | |
| destination_bank | VARCHAR(32) | |
| message_type | VARCHAR(32) | `PACS_008` |
| connector_name | VARCHAR(128) | |
| priority | INT | default 1 |
| enabled | BOOLEAN | default true |
| created_at / updated_at | DATETIME | |

**Lookup index:** `(source_bank, destination_bank, message_type, enabled, priority)`

#### `api_keys` (V14)
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| key_value | VARCHAR(128) UNI | raw API key value |
| name | VARCHAR(128) | human-readable label |
| role | VARCHAR(32) | `ADMIN`, `OPS`, `BANK` |
| bank_code | VARCHAR(32) | nullable — for BANK role |
| enabled | BOOLEAN | default true |
| created_at | DATETIME | |
| last_used_at | DATETIME | nullable, updated per request |

**Demo keys (seeded in V14):**
| Key | Role |
|-----|------|
| `sk-admin-switching-2026` | ADMIN |
| `sk-ops-switching-2026` | OPS |
| `sk-bank-a-switching-2026` | BANK (BANK_A) |
| `sk-bank-b-switching-2026` | BANK (BANK_B) |

#### `connector_configs` (V9)
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | |
| connector_name | VARCHAR(128) UNI | e.g. `MOCK_BANK_B_CONNECTOR` |
| bank_code | VARCHAR(32) | destination bank |
| connector_type | VARCHAR(32) | `MOCK` |
| endpoint_url | VARCHAR(512) | nullable |
| timeout_ms | INT | default 5000 |
| enabled | BOOLEAN | default true |
| force_reject | BOOLEAN | default false — **used in tests** |
| reject_reason_code | VARCHAR(32) | `AC01` |
| reject_reason_message | VARCHAR(512) | |
| created_at / updated_at | DATETIME | |

#### `inquiry_status_history`
| Column | Type |
|--------|------|
| id | BIGINT PK |
| inquiry_ref | VARCHAR(255) |
| status | VARCHAR(30) |
| reason_code | VARCHAR(40) |
| created_at | DATETIME(3) |

#### `participant_banks` (V1 — legacy, superseded by `participants` in V8)
Legacy table still exists in schema. Not actively used after V8.

---

## 6. API Endpoints

### Public / Client-Facing APIs

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/inquiries` | Create inquiry (JSON path). Body: `{sourceBank, destinationBank, creditorAccount, amount, currency}` |
| GET | `/api/inquiries/{inquiryRef}` | Get inquiry by ref |
| POST | `/api/transfers` | Create transfer. Body: `{inquiryRef, sourceBank, destinationBank, debtorAccount, creditorAccount, amount, currency, idempotencyKey?}` |
| GET | `/api/transfers` | List all transfers |
| GET | `/api/transfers/{transferRef}` | Get transfer by ref |
| GET | `/api/transfers/{transferRef}/trace` | Full trace of a transfer (public) |
| POST | `/api/iso20022/pacs008` | Receive PACS.008 ISO XML. Header: `X-Bank-Code`. Content-Type: `application/xml` |
| POST | `/api/iso20022/acmt023` | Receive ACMT.023 ISO XML inquiry. Header: `X-Bank-Code`. Content-Type: `application/xml` |
| GET | `/api/iso-messages` | List ISO messages |
| GET | `/api/iso-messages/{messageKey}` | Get ISO message |
| GET | `/api/outbox-events` | List outbox events |
| POST | `/api/outbox-events/{outboxEventId}/retry` | Manually retry an outbox event |
| GET | `/api/participants` | List participants. Query: `?status=ACTIVE` |
| GET | `/api/participants/{bankCode}` | Get participant |
| POST | `/api/participants` | Create participant |
| PATCH | `/api/participants/{bankCode}` | Update participant |
| GET | `/api/routing-rules` | List routing rules |
| GET | `/api/routing-rules/resolve` | Resolve route for `?sourceBank=&destinationBank=&messageType=` |
| POST | `/api/routing-rules/cache/clear` | Clear routing cache |
| POST | `/api/routing-rules` | Create routing rule |
| PATCH | `/api/routing-rules/{routeCode}` | Update routing rule |
| GET | `/api/connector-configs` | List connector configs |
| GET | `/api/connector-configs/{connectorName}` | Get connector config |
| POST | `/api/connector-configs` | Create connector config |
| PATCH | `/api/connector-configs/{connectorName}` | Update connector config |
| GET | `/api/iso-inquiries/{inquiryRef}` | Get ISO inquiry by ref |

### Operations / Admin APIs (`/api/operations/*`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/operations/health` | Operations health check |
| GET | `/api/operations/dashboard-summary` | Dashboard: counts, recent activity |
| GET | `/api/operations/transactions` | List all transactions (cross-domain view) |
| GET | `/api/operations/transfers` | List transfers (ops view) |
| GET | `/api/operations/transfers/{transferRef}` | Get transfer detail (ops) |
| GET | `/api/operations/transfers/{transferRef}/trace` | **Full trace**: transfer + inquiry + outbox + ISO messages + audit timeline |
| GET | `/api/operations/iso-messages` | List ISO messages (ops) |
| GET | `/api/operations/iso-inquiries` | List ISO inquiries (ops) |
| GET | `/api/operations/iso-inquiries/{inquiryRef}` | Get ISO inquiry (ops) |
| GET | `/api/operations/audit-logs` | List audit logs |
| GET | `/api/operations/outbox-failures` | List failed outbox events |
| GET | `/api/operations/outbox-stuck` | List stuck PROCESSING events |
| POST | `/api/operations/outbox-failures/retry-all` | Retry all failed events |
| POST | `/api/operations/outbox-events/{id}/mark-reviewed` | Mark event as REVIEWED |
| POST | `/api/operations/outbox-stuck/recover` | Manually recover stuck events |
| GET | `/api/operations/bank-status` | Bank status overview |
| POST | `/api/operations/bank-onboarding` | Onboard new bank (creates participant + routing + connector) |
| GET | `/api/operations/connectors/health` | Connector health |
| POST | `/api/operations/connectors/{connectorName}/test` | Test a connector |

### Actuator
| Method | Path |
|--------|------|
| GET | `/actuator/health` |
| GET | `/actuator/info` |

---

## 7. Key Services

### CreateTransferService
**Path:** `transfer/service/CreateTransferService.java`
**Flow:**
1. Compute `requestHash` (SHA-256 of request)
2. Check idempotency: if key exists + hash matches → return existing transfer
3. Load inquiry, validate (status=ELIGIBLE, amounts/currency/bank must match)
4. Mark inquiry USED via `InquiryStatus`
5. Generate `transferRef` via `TransferRefGenerator`
6. Create `TransferEntity` (status=RECEIVED)
7. Create `IsoMessageEntity` (PACS_008, OUTBOUND, encrypt payload)
8. `OutboxTransactionService.createOutboxEvent()` — all in one `@Transactional`
9. Save `IdempotencyRecord`
10. Log audit event `TRANSFER_CREATED`

**Exceptions thrown:**
- `InquiryValidationException` (400) — mismatched fields
- `InquiryAlreadyUsedException` (409) — inquiry already used
- `ParticipantUnavailableException` (422) — bank not ACTIVE
- `RoutingRuleNotFoundException` (422) — no route found
- `IdempotencyConflictException` (409) — key exists with different hash

### OutboxProcessorService
**Path:** `outbox/service/OutboxProcessorService.java`
**Key config:** `maxRetry` injected via `@Value("${switching.outbox.worker.max-retry:3}")` (was a hardcoded static final), `SOURCE_SYSTEM = "WORKER"`
**Flow per event:**
1. Claim event (PENDING → PROCESSING) in `PROPAGATION_REQUIRES_NEW` TX to prevent concurrent processing
2. Dispatch via `OutboxIsoMessageDispatchService`
3. Success: event → SUCCESS, transfer → SUCCESS
4. Retryable failure (retryCount < maxRetry): event → PENDING, retry_count++
5. Terminal failure (>= maxRetry or non-retryable): event → FAILED, transfer → FAILED

### IsoMessageCryptoService
**Path:** `iso/security/IsoMessageCryptoService.java`
**Algorithm:** AES/GCM/NoPadding, 12-byte IV, 128-bit GCM tag
**Config:** `switching.security.message-crypto-key-base64` (env: `MESSAGE_CRYPTO_KEY_BASE64`)
**✅ Fixed (Phase 1):** If `MESSAGE_CRYPTO_KEY_BASE64` is empty and the active Spring profile is not `test`, `resolveKey()` throws `IllegalStateException` at startup. In test profile it falls back to a hard-coded dev key so tests don't need the env var.

### RoutingService
**Path:** `routing/service/RoutingService.java`
**Cache:** In-memory `ConcurrentHashMap` — cleared via `POST /api/routing-rules/cache/clear`
**Lookup:** `routing_rules` by `(source_bank, destination_bank, message_type, enabled=true)` ordered by priority

### OperationsTransferTraceService
**Path:** `operations/service/OperationsTransferTraceService.java`
**Returns:** Combined timeline of transfer + inquiry + outbox events + ISO messages + audit logs

**✅ Fixed (Phase 1 SQL bug):** SQL text blocks used `WHERE """` which stripped trailing space → `WHEREcondition` → MySQL syntax error. Fixed with `WHERE\s"""`.

**✅ Fixed (Phase 2 silent catch):** All 4 catch blocks now call `log.error(msg, transferRef, ex)` before adding to `warnings`. Failures are now visible in application logs instead of silently producing `TRACE_FOUND_WITH_WARNINGS`.

**✅ Fixed (Phase 3 JSON-path trace):** `findInquiry()` now uses a two-step fallback:
1. Try `iso_inquiries` first (`findIsoPathInquiry`) — covers ISO XML transfers
2. If null, fall back to `inquiries` table (`findJsonPathInquiry`) — covers JSON-path transfers

JSON-path inquiries are mapped to `OperationsTransferTraceInquiryResponse` with `messageId/instructionId/endToEndId/debtorAccount/expiresAt = null` (ISO-only fields). `usedByTransferRef` is inferred from context because the `inquiries` table has no back-reference column. Timeline events use type `JSON_INQUIRY` and protocol `JSON_API` instead of `ISO_INQUIRY` / `ACMT_023`.

**Result:** JSON-path transfers now show `hasInquiry: true` and two extra timeline events (`JSON_INQUIRY_CREATED`, `JSON_INQUIRY_USED_BY_TRANSFER`).

---

## 8. Background Workers

| Worker | Schedule | Action |
|--------|----------|--------|
| `OutboxDispatchWorker` | `@TransactionalEventListener(AFTER_COMMIT)` | Near real-time: fires immediately after transfer saves — ~50-200ms latency |
| `OutboxDispatchWorker` | `fixedDelayString=${outbox.worker.poll-interval-ms:30000}` | Safety-net poll every 30s for missed events |
| `OutboxRecoveryWorker` | `fixedDelayString=${outbox.worker.recovery-interval-ms:60000}` | Find PROCESSING events stuck > `stuck-timeout-minutes` → reset to PENDING |

Workers are enabled via `@EnableScheduling` on `SchedulingConfig`.

**All outbox values are now configurable via env vars:**

| Env Var | Default | Description |
|---------|---------|-------------|
| `OUTBOX_POLL_INTERVAL_MS` | `30000` | Safety-net poll interval (ms) |
| `OUTBOX_RECOVERY_INTERVAL_MS` | `60000` | Recovery worker interval (ms) |
| `OUTBOX_BATCH_SIZE` | `20` | Max events per poll cycle |
| `OUTBOX_MAX_RETRY` | `3` | Max dispatch retries before FAILED |
| `OUTBOX_STUCK_TIMEOUT_MINUTES` | `2` | Minutes before PROCESSING is considered stuck |

---

## 9. Error Catalog

All errors are defined in `ErrorCatalog.java`. Structure:

```json
{
  "timestamp": "...",
  "status": 400,
  "error": "BAD_REQUEST",
  "errorCode": "REQ-001",
  "category": "REQUEST",
  "layer": "API",
  "phase": "VALIDATE_REQUEST",
  "retryable": false,
  "message": "Request validation failed",
  "path": "/api/transfers",
  "requestId": "uuid",
  "details": { "field": "message" }
}
```

| Code | HTTP | Description | Retryable |
|------|------|-------------|-----------|
| REQ-001 | 400 | Request validation failed (Bean Validation) | No |
| REQ-002 | 400 | Malformed JSON | No |
| REQ-003 | 405 | HTTP method not allowed | No |
| INQ-001 | 404 | Inquiry not found | No |
| INQ-002 | 400 | Inquiry validation failed (field mismatch) | No |
| INQ-003 | 409 | Inquiry already used by transfer | No |
| TRF-001 | 404 | Transfer not found | No |
| TRF-002 | 409 | Idempotency conflict | No |
| OUT-001 | 500 | Outbox payload parse failed | No |
| OUT-002 | 500 | Outbox worker processing failed | **Yes** |
| OUT-003 | 500 | Outbox stuck processing recovered | **Yes** |
| OUT-004 | 409 | Outbox event cannot be manually retried | No |
| OUT-005 | 404 | Outbox event not found | No |
| NET-001 | 503 | Downstream connection failed | **Yes** |
| NET-002 | 503 | Downstream timeout | **Yes** |
| NET-003 | 503 | DNS resolution failed | **Yes** |
| NET-004 | 503 | TLS/SSL handshake failed | **Yes** |
| EXT-001 | 502 | Downstream bank rejected transfer | No |
| EXT-002 | 502 | Downstream bank invalid response | No |
| INF-DB-001 | 500 | Database write failed | **Yes** |
| INF-DB-002 | 409 | DB unique constraint violation | No |
| INF-SER-001 | 500 | Serialization/deserialization failed | No |
| ISO-001 | 404 | ISO message not found | No |
| ISO-002 | 409 | ISO message invalid state | No |
| ISO-003 | 500 | ISO message crypto failed | No |
| PRT-001 | 404 | Participant not found | No |
| PRT-002 | 422 | Participant not ACTIVE | No |
| PRT-003 | 409 | Participant already exists | No |
| RTE-001 | 422 | Routing rule not found | No |
| RTE-002 | 409 | Routing rule already exists | No |
| CON-001 | 503 | Connector not found/unavailable | No |
| CON-002 | 409 | Connector already exists | No |
| SYS-001 | 500 | Internal server error (catch-all) | No |

---

## 10. Status Enums

### TransferStatus
- `RECEIVED` — created, waiting for outbox dispatch
- `SUCCESS` — PACS.002 accepted by destination bank
- `FAILED` — rejected or exceeded max retries

### InquiryStatus (JSON path)
- `RECEIVED` → `ELIGIBLE` / `NOT_ELIGIBLE` / `FAILED`

### OutboxStatus
- `PENDING` → `PROCESSING` → `SUCCESS` / `FAILED` / `REVIEWED`

### IsoMessageType
- `PACS_008` — FI Credit Transfer
- `PACS_002` — FI Payment Status
- `PACS_028` — FI Payment Status Request
- `PACS_004` — Payment Return

---

## 11. Security

### IsoMessageCryptoService (AES/GCM)
- Algorithm: `AES/GCM/NoPadding`
- IV: 12 bytes (SecureRandom), prepended to ciphertext
- Tag: 128-bit GCM authentication tag
- Key: 16, 24, or 32 bytes decoded from Base64
- Encrypted format: `base64(iv[12] + ciphertext)`
- **✅ Fixed (Phase 1):** If `MESSAGE_CRYPTO_KEY_BASE64` is blank outside the test profile, startup throws `IllegalStateException`. Test profile uses a safe fixed dev key so tests run without the env var.

### RequestIdFilter
- Reads `X-Request-Id` header → if absent, generates UUID
- Sets attribute and response header `X-Request-Id`
- Puts `requestId` into **SLF4J MDC** at filter entry; clears MDC in `finally` so it appears in every log line for the request duration

### Rate Limiting (Phase 3 ✅)
- **Filter:** `RateLimitFilter` (`@Order(10)`) — runs before Security filters
- **Scope:** POST, PUT, PATCH, DELETE requests only
- **Client identity:** `X-API-Key` header value → fallback to `X-Forwarded-For` → fallback to remote IP
- **Algorithm:** Bucket4j token bucket — `requestsPerMinute` tokens, refills every 1 minute
- **Response on limit:** `429 Too Many Requests` with `errorCode: REQ-004`
- **Config:**
  - `switching.security.rate-limit.enabled` (env: `RATE_LIMIT_ENABLED`, default `true`)
  - `switching.security.rate-limit.requests-per-minute` (env: `RATE_LIMIT_RPM`, default `100`)
- **Disabled in tests:** `application-test.yml` sets `rate-limit.enabled: false`

### Near Real-time Dispatch (Phase 3 ✅)
- `OutboxTransactionService.enqueueTransferDispatch()` publishes `OutboxCreatedEvent` after saving the outbox record
- `OutboxDispatchWorker.onOutboxCreated()` listens with `@TransactionalEventListener(AFTER_COMMIT)` — fires synchronously after the outer transaction commits (~50-200ms vs 0-5s before)
- Safety-net polling worker still runs every 30s to catch any events missed due to app restart or listener failure
- In test profile: poll interval kept at 5000ms so integration tests aren't slowed down

### Structured Logging with MDC (Phase 2 ✅)
- **`requestId`** — set in `RequestIdFilter` for every HTTP request; cleared in `finally`
- **`inquiryRef`** — set in `CreateTransferService.create()` once the inquiry ref is resolved
- **`transferRef`** — set in `CreateTransferService.create()` once the transfer ref is generated; also set in `OutboxProcessorService.doProcessSingleEvent()` once the outbox payload is parsed
- **`outboxEventId`** — set in `OutboxProcessorService.processSingleEvent()` for the duration of processing one event
- All MDC keys are removed/cleared in `finally` blocks to prevent leaking across requests or worker iterations

### Micrometer Metrics (Phase 2 ✅)
Pre-registered at startup; always appear in `/actuator/metrics` even before first event.

| Metric name | Type | Description |
|-------------|------|-------------|
| `payment.transfer.created` | Counter | Transfers accepted and queued for dispatch |
| `payment.transfer.failed` | Counter | Transfers that threw during `CreateTransferService.create()` |
| `payment.outbox.dispatch.success` | Counter | Outbox events finalized as SUCCESS |
| `payment.outbox.dispatch.failed{type=business}` | Counter | Outbox events rejected by downstream bank |
| `payment.outbox.dispatch.failed{type=technical}` | Counter | Outbox events that hit terminal technical failure |
| `payment.outbox.dispatch.duration` | Timer | End-to-end time for `OutboxProcessorService.processSingleEvent()` |
| `payment.outbox.pending.count` | Gauge | Live count of `outbox_events` with status=PENDING |
| `payment.outbox.processing.count` | Gauge | Live count of `outbox_events` with status=PROCESSING |
| `payment.outbox.failed.count` | Gauge | Live count of `outbox_events` with status=FAILED |

Gauges query `OutboxEventRepository.countByStatus()` directly (already existed in repository).

### API Key Authentication (Phase 1 ✅)
- **Header:** `X-API-Key: <key>`
- **Filter:** `ApiKeyAuthFilter` (OncePerRequestFilter) — queries `api_keys` table, sets `SecurityContextHolder`
- **Roles:** `ROLE_ADMIN`, `ROLE_OPS`, `ROLE_BANK`
- **Config flag:** `switching.security.api-key.enabled` (default `true`; set `false` in test profile)
- **Disabled in tests:** `application-test.yml` sets `api-key.enabled: false` → all requests allowed

**Role access matrix:**
| Endpoint group | BANK | OPS | ADMIN |
|----------------|------|-----|-------|
| POST /api/inquiries, /api/transfers | ✅ | ❌ | ✅ |
| GET /api/inquiries/**, /api/transfers/** | ✅ | ✅ | ✅ |
| /api/iso20022/** | ✅ | ❌ | ✅ |
| /api/operations/** | ❌ | ✅ | ✅ |
| /api/outbox-events/** | ❌ | ✅ | ✅ |
| POST/PATCH /api/participants, routing-rules, connector-configs | ❌ | ❌ | ✅ |
| GET /api/participants, routing-rules, connector-configs | ❌ | ✅ | ✅ |
| /actuator/health, /actuator/info | ✅ | ✅ | ✅ |

---

## 12. Configuration

### Environment Variables (`.env` file required)

```bash
# Required
DB_PASSWORD=your_mysql_password
TEST_DB_PASSWORD=your_test_mysql_password

# Required for production (leave empty = dev fallback key used — INSECURE)
MESSAGE_CRYPTO_KEY_BASE64=<base64-encoded 16/24/32 byte AES key>

# Docker only
MYSQL_ROOT_PASSWORD=your_mysql_password
```

### `application.yml` (production)

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/switching_db?useSSL=false&serverTimezone=Asia/Bangkok&allowPublicKeyRetrieval=true}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD}        # NO default — must be set
  jpa:
    hibernate.ddl-auto: validate
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: ${SERVER_PORT:8080}

management:
  endpoints.web.exposure.include: health,info

switching:
  payment:
    json-initiation.enabled: ${JSON_INITIATION_ENABLED:false}   # false = ISO-only; set true in .env for local dev
  mock-bank.pacs002.force-reject: ${MOCK_BANK_FORCE_REJECT:false}
  security:
    message-crypto-key-base64: ${MESSAGE_CRYPTO_KEY_BASE64:}    # empty = DEV fallback (blocked in prod)
    api-key.enabled: ${API_KEY_AUTH_ENABLED:true}
    rate-limit:
      enabled: ${RATE_LIMIT_ENABLED:true}
      requests-per-minute: ${RATE_LIMIT_RPM:100}
  outbox.worker:
    poll-interval-ms: ${OUTBOX_POLL_INTERVAL_MS:30000}
    recovery-interval-ms: ${OUTBOX_RECOVERY_INTERVAL_MS:60000}
    batch-size: ${OUTBOX_BATCH_SIZE:20}
    max-retry: ${OUTBOX_MAX_RETRY:3}
    stuck-timeout-minutes: ${OUTBOX_STUCK_TIMEOUT_MINUTES:2}
```

### `application-test.yml` (test profile)

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
  datasource:
    url: ${TEST_DB_URL:jdbc:mysql://localhost:3306/switching_clean?...}
    password: ${TEST_DB_PASSWORD}   # NO default — must be set

switching:
  payment.json-initiation.enabled: true
  mock-bank.pacs002.force-reject: false
  security.message-crypto-key-base64: ${TEST_MESSAGE_CRYPTO_KEY_BASE64:NImwCmFwkSIeDgy8UJtzGq86A389puEEe6gi2Wdo9MM=}
```

Test DB: `switching_clean` (separate from prod `switching_db`)

### 2026-05-14 Local Run Fix

- Added `spring.config.import: optional:file:.env[.properties]` to both `application.yml` and `application-test.yml`.
- Reason: running with `./mvnw spring-boot:run` directly does not source `.env`, so Spring could start with missing DB env values and fail during Flyway/DataSource initialization.
- `run.sh` still sources `.env`; this change also supports direct Maven/IDE local runs.
- Updated `run.sh local` to auto-select the first free port starting at `8080` when `SERVER_PORT` is not explicitly set.
- Reason: local startup failed with `Port 8080 was already in use` when another app instance was already listening.

---

## 13. Docker Setup

### `docker-compose.yml`
- **mysql** service: `mysql:8.4`, database `switching_db`, health check with `mysqladmin ping`
- **app** service: built from `Dockerfile`, waits for MySQL health, connects via `mysql:3306`

### `Dockerfile`
Multi-stage:
1. Build: `maven:3.9.9-eclipse-temurin-21` → `./mvnw clean package -DskipTests`
2. Run: `eclipse-temurin:21-jre` → `java -jar app.jar`

### Run Commands (`run.sh`)
```bash
./run.sh              # Start locally (requires MySQL already running)
./run.sh docker       # Build + start full stack (Docker Compose)
./run.sh docker:db    # Start MySQL only
./run.sh test         # Run all Maven tests
./run.sh test:single  # CLASS=ClassName ./run.sh test:single
./run.sh stop         # docker compose down
./run.sh logs         # docker compose logs -f app
```

---

## 14. Test Coverage

**Latest local scan:** 2026-05-13

**Current result:** `./mvnw test` does **not** pass in the scanned environment.

- Maven test summary: `Tests run: 60, Failures: 0, Errors: 46, Skipped: 0`
- First/root failure: Spring test context cannot connect to MySQL test DB.
- Error: `Access denied for user 'root'@'localhost' (using password: YES)`
- A skip-test package build does pass: `./mvnw -DskipTests package`

**Production-readiness implication:** the current test suite is useful, but it is not yet a reliable CI/production gate because integration tests depend on a locally configured MySQL root credential.

| Test Class | Tests | Type | Description |
|-----------|-------|------|-------------|
| `SwitchingApplicationTests` | 1 | Unit | Context loads |
| `FullTransferFlowIntegrationTest` | 24 | Integration | Full inquiry→transfer flow, idempotency, force reject, trace |
| `IsoInquiryFlowIntegrationTest` | ~10 | Integration | ISO XML ACMT.023 inquiry flow |
| `IsoInquiryValidationIntegrationTest` | ~5 | Integration | Validation edge cases |
| `IsoInquiryExpiryIntegrationTest` | ~3 | Integration | Inquiry TTL expiry |
| `OperationsTransferTraceIntegrationTest` | 2 | Integration | Ops trace endpoint with combined timeline |
| `OperationsTransferQueryIntegrationTest` | 5 | Integration | Ops transfer list/query |
| `OperationsIsoInquiryQueryIntegrationTest` | ~5 | Integration | Ops ISO inquiry queries |
| `ParticipantManagementServiceTest` | ~5 | Unit | Participant CRUD |
| `RoutingRuleManagementServiceTest` | 5 | Unit | Routing rule CRUD |
| `ConnectorConfigManagementServiceTest` | 5 | Unit | Connector CRUD |

### Test Pattern
All integration tests:
- `@SpringBootTest` + `@ActiveProfiles("test")`
- Use `switching_clean` database
- Seed fixtures via `JdbcTemplate.update()` with `ON DUPLICATE KEY UPDATE`
- MockMvc for HTTP calls
- `AtomicInteger` counter + millis for unique refs

### Test Hardening Needed

- Replace local MySQL/root dependency with Testcontainers or a dedicated Docker test DB.
- Make `./mvnw test` pass from a clean checkout with documented env setup.
- Split fast unit tests from DB-backed integration tests.
- Add CI that runs tests before packaging or image build.
- Avoid production image builds that silently skip the test gate.

### Key Test Fixtures (seeded in BeforeEach)
```sql
-- Participants
INSERT INTO participants (bank_code, bank_name, status, ...) VALUES ('BANK_A', ...) ON DUPLICATE KEY UPDATE ...
INSERT INTO participants (bank_code, bank_name, status, ...) VALUES ('BANK_B', ...) ON DUPLICATE KEY UPDATE ...

-- Connector
INSERT INTO connector_configs (connector_name='MOCK_BANK_B_CONNECTOR', bank_code='BANK_B', ...) ON DUPLICATE KEY UPDATE ...

-- Routing
DELETE FROM routing_rules WHERE source_bank='BANK_A' AND destination_bank='BANK_B' AND message_type='PACS_008'
INSERT INTO routing_rules (route_code='ROUTE_A_TO_B_PACS008_TEST', ...) VALUES ...
```

---

## 15. Known Issues & Fixed Bugs

### Fixed ✅

| Bug | Location | Fix |
|-----|----------|-----|
| Hardcoded DB password `55919230` in git history | `application.yml`, `application-test.yml` | Removed with `git filter-repo`, replaced with `${DB_PASSWORD}` (no default) |
| SQL text block trailing space stripped | `OperationsTransferTraceService.java` lines 240, 371, 441 | Changed `WHERE """` to `WHERE\s"""` (`\s` = non-strippable space in Java text block) |
| Silent exception swallowing in trace | `OperationsTransferTraceService.java` catch blocks | Added `log.error(...)` before each `warnings.add(...)` so failures are visible in logs |
| `hasInquiry: false` for JSON-path transfers in trace | `OperationsTransferTraceService.findInquiry()` | Added `findJsonPathInquiry()` fallback — queries `inquiries` table when `iso_inquiries` returns null; timeline now shows `JSON_INQUIRY_CREATED` events |
| `json-initiation.enabled` hardcoded to `false` | `application.yml` | Changed to `${JSON_INITIATION_ENABLED:false}`; added `JSON_INITIATION_ENABLED=true` to `.env` for local dev |
| `MAX_RETRY` static final impossible to configure | `OutboxProcessorService`, `OutboxRecoveryService` | Changed to `private final int maxRetry` injected via `@Value("${switching.outbox.worker.max-retry:3}")` |
| Idempotency hash conflict returned 500 instead of 409 | `IdempotencyService.findExistingTransfer()` line 41–44 | `throw new IllegalStateException(...)` changed to `throw new IdempotencyConflictException(...)` → now maps to TRF-002 (409 CONFLICT) via `GlobalExceptionHandler` |
| `OutboxEventNotFoundException` / `OutboxManualRetryNotAllowedException` fell through to 500 SYS-001 | `GlobalExceptionHandler.java`, `ErrorCatalog.java` | Added `OUT_005` (404 NOT_FOUND) to `ErrorCatalog`; registered both handlers in `GlobalExceptionHandler` — outbox retry-not-found now returns 404 and retry-not-allowed returns 409 |
| `audit_logs` query selected non-existent `channel_id` column → 500 | `OperationsAuditLogQueryService.java` SQL + `mapRow()` | Removed `a.channel_id` from SELECT; set `null` in `mapRow()` for `channelId` field (audit_logs table has no channel_id column) |

**Root cause of SQL bug:** Java text blocks automatically strip trailing whitespace from content lines. `WHERE ` before closing `"""` became `WHERE`, producing `WHEREcondition` → MySQL `SQLSyntaxErrorException`. Affected `findInquiry()`, `findIsoMessages()`, `findAuditEvents()`. Exception was silently caught by try-catch, making `hasInquiry=false`.

### Open ⚠️

| Issue | Location | Risk | Fix Needed |
|-------|----------|------|------------|
| ~~DEV_FALLBACK_KEY used when `MESSAGE_CRYPTO_KEY_BASE64` empty~~ | ~~`IsoMessageCryptoService.java`~~ | **FIXED ✅ Phase 1** | Throws `IllegalStateException` outside test profile |
| ~~No authentication/authorization~~ | ~~All endpoints~~ | **FIXED ✅ Phase 1** | API Key auth with RBAC via Spring Security |
| ~~`hasInquiry: false` and missing timeline items for JSON-path transfers~~ | ~~`OperationsTransferTraceService.findInquiry()`~~ | **FIXED ✅ Phase 3** | `findJsonPathInquiry()` fallback added |
| ~~`json-initiation.enabled` hardcoded `false` blocks JSON path locally~~ | ~~`application.yml`~~ | **FIXED ✅ Phase 3** | Env-var driven via `JSON_INITIATION_ENABLED` |
| Integration tests depend on local MySQL root credentials | `application-test.yml`, integration tests | **HIGH** — no repeatable CI gate | Use Testcontainers or dedicated test DB credentials |
| Docker build skips tests | `Dockerfile` | **HIGH** — image can be produced while tests fail | Run tests in CI/build gate; only skip tests for explicit dev builds |
| MySQL root user and insecure connection defaults | `docker-compose.yml`, `application.yml` | **HIGH** — weak prod posture | Use dedicated DB user, TLS, and prod-only env values |
| V8 drops/recreates `routing_rules` after V2 seed | `V8__create_participants_and_routing_rules.sql` | **MEDIUM/HIGH** — fresh DB may lack usable routes | Add current participant/routing seed or onboarding migration |
| ~~Routing cache not invalidated on update~~ | ~~`RoutingService.java`~~ | **FIXED ✅ Phase 3** | `RoutingRuleManagementService.create()` and `update()` call `routingService.clearCache()` |
| Test data accumulates in DB across runs | All integration tests | Low | Add cleanup in `@AfterEach` |
| `inquiry_status_history` not used for ISO path | `iso_inquiries` | Low | Status changes in ISO path not tracked |
| JSON-path `inquiries` trace shows null for ISO-only fields | `OperationsTransferTraceService` | Low | `messageId`, `instructionId`, `endToEndId`, `debtorAccount`, `expiresAt` are null by design for JSON path — expected |

---

## 16. Seed Data (V2 + V9)

```sql
-- V2: Legacy participant banks
INSERT INTO participant_banks (bank_code, bank_name) VALUES ('BANK_A', ...), ('BANK_B', ...)
INSERT INTO routing_rules (route_code='ROUTE_BANK_B_PRIMARY', destination_bank_code='BANK_B', connector_name='MOCK_CONNECTOR')

-- V9: Connector configs
INSERT INTO connector_configs VALUES ('MOCK_BANK_A_CONNECTOR', 'BANK_A', 'MOCK', ...)
INSERT INTO connector_configs VALUES ('MOCK_BANK_B_CONNECTOR', 'BANK_B', 'MOCK', ...)
INSERT INTO connector_configs VALUES ('MOCK_BANK_C_CONNECTOR', 'BANK_C', 'MOCK', ...)
```

**Important:** `participants` table (V8) is NOT seeded in migrations. Must be seeded manually or through `POST /api/participants` or `POST /api/operations/bank-onboarding`. Integration tests seed `BANK_A` and `BANK_B` in `@BeforeEach`.

---

## 17. Module Package Map

```
com.example.switching.
├── audit          → audit_logs table, AuditLogService.log()
├── common         → ApiErrorResponse, ErrorCatalog, GlobalExceptionHandler, RequestIdFilter
├── connector      → BankConnector interface, MockBankConnector, ConnectorRegistry, connector_configs table
├── dashboard      → /api/operations/dashboard-summary
├── demo           → DemoFlowService (non-production helper)
├── idempotency    → idempotency_records table, IdempotencyService
├── inquiry        → inquiries table (JSON path), /api/inquiries
├── iso            → iso_messages, iso_inquiries tables, ISO XML parsing, crypto
├── operations     → /api/operations/* (all ops controllers + services)
├── outbox         → outbox_events table, workers, dispatch
├── participant    → participants table, /api/participants
├── routing        → routing_rules table, RoutingService with cache
└── transfer       → transfers table (core), /api/transfers
```

---

## 18. ISO Message Payload (Outbox JSON)

The `outbox_events.payload` column contains JSON:

```json
{
  "transferRef": "TRX-1234567890-ABCD",
  "isoMessageId": 42,
  "sourceBank": "BANK_A",
  "destinationBank": "BANK_B",
  "routeCode": "ROUTE_A_TO_B_PACS008",
  "connectorName": "MOCK_BANK_B_CONNECTOR",
  "messageType": "PACS_008"
}
```

Note: `messageType` was added later — old payloads may not have it. `OutboxIsoMessageDispatchService` falls back to `iso_messages.message_type` if absent.

---

## 19. Curl E2E Test Script

**Location:** `scripts/curl_e2e_tests.sh`

Sections:
1. Health (`/actuator/health`, `/api/operations/health`)
2. Inquiry (create, get, 404, 400 validation)
3. Transfer (create, get, reuse→409/422, idempotency, list, 404)
4. ISO Messages list
5. Outbox (list, retry 404)
6. Participants (list, get, 404)
7. Routing Rules list
8. Connector Configs list
9. Operations APIs (dashboard, transactions, ISO messages, audit logs, outbox failures, trace)
10. Transfer Trace

Usage: `./scripts/curl_e2e_tests.sh` or `BASE_URL=http://localhost:8080 ./scripts/curl_e2e_tests.sh`

### Automated curl Test Runner

**Location:** `scripts/run_tests.sh`

Latest update:
- Added extended ISO20022 automated curl testcases to Section 11.
- New coverage includes valid ACMT.023 inquiry, extracting `InquiryRef` from ACMT.024, valid PACS.008 using that `InquiryRef`, PACS.002 acceptance check, repeat PACS.008 idempotency, used `InquiryRef` rejection, and unknown `InquiryRef` rejection.
- Helper functions added for XML response checks: `xml_val()` and `body_has()`.
- Runtime state added for ISO flow: `ISO_INQUIRY_REF` and `ISO_TRANSFER_REF`.
- 2026-05-14 fix: TC-103 ACMT.023 XML now matches the local parser profile (`IdVrfctnReq`, `BICFI`, `PtyAndAcctId/Acct/Id/Othr/Id`) instead of the unsupported `AcctMgmtInqDef` shape.
- 2026-05-14 fix: TC-104/TC-106/TC-107 PACS.008 XML now uses `BICFI` and the marked supplementary data profile (`PlcAndNm=LAO_SWITCHING_INQUIRY_REF`).
- 2026-05-14 fix: TC-107b now validates the stable ISO rejection status `TxSts=RJCT` instead of relying on a specific `AddtlInf` text.
- 2026-05-14 fix: TC-104b and TC-105b now accept both PACS.002 accepted statuses `ACCP` and `ACTC`; current implementation returns `ACTC` with `AcctSvcrRef`.

### Manual curl Test Cases

**Location:** `docs/manual-curl-testcases.md`

Purpose:
- Manual testcase checklist for users who want to copy/paste and run `curl` themselves.
- Covers public health, API key authentication, role authorization, admin setup smoke tests, JSON inquiry/transfer flow, idempotency, force-reject, rate limiting, outbox/operations queries, and ISO XML smoke tests.
- Uses seeded demo keys from `V14__create_api_keys.sql`:
  - `sk-admin-switching-2026`
  - `sk-ops-switching-2026`
  - `sk-bank-a-switching-2026`
  - `sk-bank-b-switching-2026`

**Test run result (2026-05-14):** 38 PASS · 0 FAIL · 3 bugs found and fixed during the run:
1. `IdempotencyService`: `IllegalStateException` on hash mismatch → changed to `IdempotencyConflictException` (TRF-002, 409)
2. `GlobalExceptionHandler` + `ErrorCatalog`: missing handlers for `OutboxEventNotFoundException` (OUT-005, 404) and `OutboxManualRetryNotAllowedException` (OUT-004, 409)
3. `OperationsAuditLogQueryService`: SQL selected `a.channel_id` which does not exist in `audit_logs` table → removed column, `mapRow()` returns `null` for channelId

---

## 20. Production Readiness Snapshot

**Current assessment:** Not production-ready yet, but progressing well. Phase 1–3 hardening has closed the largest security, observability, and configurability gaps. Main remaining blockers are repeatable test infrastructure and database hardening.

**Estimated readiness:** 65-70%.

### Strengths

- Clear modular Spring Boot structure: transfer, inquiry, ISO, outbox, routing, participant, connector, operations.
- Transactional outbox pattern with claim step (`PENDING` → `PROCESSING`) and near real-time dispatch via `@TransactionalEventListener(AFTER_COMMIT)`.
- Flyway is enabled and Hibernate uses `ddl-auto=validate`.
- Central `GlobalExceptionHandler` and `ErrorCatalog`.
- Request tracing: `X-Request-Id` header + MDC (`requestId`, `transferRef`, `inquiryRef`, `outboxEventId`).
- Audit log model covers transfer/outbox/ISO/inquiry events.
- Actuator exposure limited to `health,info`.
- **Phase 1 ✅:** API Key authentication with RBAC (ADMIN/OPS/BANK roles), AES/GCM message encryption mandatory in prod.
- **Phase 2 ✅:** Micrometer counters/timers/gauges for transfers and outbox; MDC log correlation across all layers.
- **Phase 3 ✅:** All outbox parameters (poll interval, batch size, max retry, stuck timeout) configurable via env vars; rate limiting (Bucket4j token bucket, 429 response); near real-time outbox dispatch (~50-200ms); routing cache auto-invalidation on rule changes; JSON-path inquiry now visible in transfer trace.

### Main Production Gaps

- Tests are not repeatable without local DB credentials; `./mvnw test` fails on clean checkout.
- Dockerfile packages with `-DskipTests` — image can be built silently while tests fail.
- Docker Compose uses MySQL root for app access.
- Default DB URLs use `useSSL=false` and `allowPublicKeyRetrieval=true`.
- Current migrations do not seed the V8 `participants`/new `routing_rules` tables for a production-like fresh install.
- Multi-instance outbox behavior needs stronger verification under concurrent workers.
- No alerts configured for outbox backlog, repeated downstream failures, or connector health degradation.

---

## 21. Advanced Production Roadmap (8 Phases)

This is the intended direction for the project: move from the current switching API prototype/staging base into an advanced production-grade payment switching platform, while supporting a small number of complete web portals instead of many fragmented applications.

### Phase 0 — Production Baseline Freeze
**Status: 🔵 IN PROGRESS (2026-05-14)**

**Documents created:**
- `docs/risk-register.md` — 40+ risks across Security, DB, Test/CI, Outbox, Observability, Deployment, Business
- `docs/production-checklist.md` — Go/No-Go checklist per phase + full endpoint classification matrix

Goal: establish a stable baseline before deeper hardening.

Scope:
- Freeze new feature work while production gaps are documented.
- Inventory all APIs and classify them as bank-facing, operations, admin, audit, finance, or internal.
- Confirm core flows: ACMT.023 inquiry, PACS.008 transfer, PACS.002 response, JSON dev path, outbox dispatch, retry, trace, audit.
- Define go/no-go production criteria.
- Create a risk register for security, database, outbox, reconciliation, and operational support.

Exit criteria:
- [x] Endpoint matrix exists → `docs/production-checklist.md` (Endpoint Classification Matrix)
- [x] Risk register exists → `docs/risk-register.md` (40+ risks, severity, mitigation, phase)
- [x] Production acceptance checklist exists → `docs/production-checklist.md`
- [x] Current API behavior documented → `overall.md` Sections 4–11
- [ ] Risk register reviewed by team lead
- [ ] Go/no-go production criteria agreed and signed off
- [ ] Phase 1 start date agreed

### Phase 1 — Test & CI Gate

Goal: every change must be verifiable from a clean checkout.

Scope:
- Move integration tests to Testcontainers MySQL or a fully automated Docker test DB.
- Make `./mvnw test` pass without local root MySQL assumptions.
- Split unit, integration, and curl/e2e tests.
- Run `scripts/run_tests.sh` as a staging/e2e gate.
- Add CI for compile, unit test, integration test, package, and Docker image build.
- Do not build production images from untested code.

Exit criteria:
- Clean machine can run tests.
- CI blocks merge/build on failure.
- Test reports are stored as artifacts.
- Docker image build happens only after test gate passes.

### Phase 2 — Production Configuration

Goal: separate dev/staging/prod behavior clearly.

Scope:
- Add or finalize profiles: `dev`, `test`, `staging`, `prod`.
- Require all prod secrets through env/secret manager.
- Prod must fail startup if crypto key, API auth, DB credentials, or required config is missing.
- Disable JSON payment initiation in prod if production is ISO-only.
- Keep actuator exposure minimal in prod.
- Remove demo API keys and demo DB credentials from production deployment path.

Exit criteria:
- Prod cannot start with default secrets.
- Prod config is documented and auditable.
- Staging mirrors prod config as closely as possible.

### Phase 3 — Database & Migration Hardening

Goal: fresh install and upgrade must be predictable.

Scope:
- Review all Flyway migrations as immutable production history.
- Add production-safe seed/onboarding strategy for participants, routing rules, connector configs, and API keys.
- Replace app access via MySQL root with a dedicated least-privilege DB user.
- Enable TLS for DB connections in prod.
- Review indexes for transfer trace, outbox polling, operations lists, ISO messages, reconciliation, and audit search.
- Define backup, restore, and migration rollback procedures.

Exit criteria:
- Fresh environment can be created from migrations plus documented seed/onboarding.
- Restore drill passes.
- App DB user follows least privilege.
- Hibernate validate and Flyway validate pass in each environment.

### Phase 4 — Security Advanced

Goal: bank/payment-grade security.

Scope:
- Store API keys hashed, not plaintext.
- Add key rotation and key expiry.
- Add mTLS for bank-facing ISO endpoints where required.
- Consider HMAC/request signing for ISO inbound messages.
- Add IP allowlist per bank or per client where appropriate.
- Expand roles beyond ADMIN/OPS/BANK into production roles:
  - `ADMIN`
  - `OPS_READ`
  - `OPS_WRITE`
  - `BANK_ADMIN`
  - `BANK_USER`
  - `BANK_VIEWER`
  - `FINANCE`
  - `SETTLEMENT`
  - `DISPUTE_AGENT`
  - `DISPUTE_MANAGER`
  - `RISK_ANALYST`
  - `RISK_MANAGER`
  - `AML_OFFICER`
  - `COMPLIANCE`
  - `AUDITOR`
- Add account number masking and sensitive XML masking in logs, audit views, and portals.
- Add XML/request body size limits and XXE hardening checks.

Exit criteria:
- Key rotation works without downtime.
- Sensitive data is masked by default.
- Bank-facing trust model is documented.
- Security review checklist passes.

### Phase 5 — Reliability & Outbox Advanced

Goal: no duplicate dispatch, no lost event, recoverable failures.

Scope:
- Verify multi-instance outbox claim behavior under concurrent workers.
- Use `next_retry_at` consistently for retry scheduling.
- Add exponential backoff and failure classification.
- Define dead-letter/reviewed policy.
- Ensure manual retry always writes audit trail.
- Add idempotency race-condition tests.
- Add duplicate PACS.008 tests.
- Add stuck PROCESSING recovery tests.
- Add connector timeout/retry behavior tests.
- Add reconciliation job hooks for transfer/outbox/settlement consistency.

Exit criteria:
- Same outbox event cannot dispatch twice under multi-instance load.
- Retry and recovery behavior is deterministic.
- Failed and stuck events are operationally recoverable.
- Idempotency holds under concurrent requests.

### Phase 6 — Observability & Operations

Goal: production incidents must be visible and diagnosable.

Scope:
- Export metrics to Prometheus.
- Build Grafana dashboards for API, transfer, ISO, outbox, connector, DB, JVM.
- Add alerts for:
  - outbox pending backlog
  - stuck PROCESSING events
  - failed transfer spike
  - connector down/degraded
  - DB connection failure
  - ISO parse/validation error spike
  - p95/p99 latency regression
- Use structured JSON logs.
- Include correlation fields: `requestId`, `transferRef`, `inquiryRef`, `messageId`, `outboxEventId`, `bankCode`.
- Define runbooks per alert.

Exit criteria:
- Ops can diagnose a transfer from one `transferRef`.
- Alerts are connected to notification channels.
- Runbooks exist for critical alerts.
- Dashboard covers day-to-day operations.

### Phase 7 — Deployment & Runtime Platform

Goal: production deployment must be repeatable and reversible.

Scope:
- Run containers as non-root user.
- Add liveness/readiness probes.
- Add graceful shutdown behavior for in-flight outbox work.
- Set JVM memory/resource limits.
- Use secret manager or protected env injection.
- Define zero-downtime rollout strategy.
- Define rollback strategy.
- Consider blue/green or canary deployment.
- Run Flyway migration in a controlled deployment step.

Exit criteria:
- Deploy and rollback drills pass.
- Readiness only turns green when DB/Flyway/app dependencies are ready.
- Shutdown does not leave uncontrolled processing state.
- Staging deployment mirrors production.

### Phase 8 — Compliance & Business Readiness

Goal: the platform is ready for bank/business operations.

Scope:
- Define audit retention and data retention.
- Add purge/archive jobs.
- Define RPO/RTO.
- Run disaster recovery drill.
- Add reconciliation and settlement reports.
- Add bank onboarding checklist.
- Add ISO contract/certification tests with bank simulators or partner endpoints.
- Add load and soak tests.
- Add UAT and go-live checklist.

Exit criteria:
- DR drill passes.
- Reconciliation reports are usable by finance/settlement teams.
- Bank onboarding is repeatable.
- Load test meets target throughput and latency.
- Go-live checklist is signed off.

---

## 22. Web Portal Target Architecture

Direction: support production operations with the fewest portals possible while still covering all roles. Do not create 11 separate portals. Build 4 primary business web apps and use external BI/observability/log tools for analytics and technical operations.

### Portal 1 — Operations & Admin Portal

Combines:
- Operations Portal
- Transaction Trace Portal
- ISO Message Portal for internal users
- Outbox / Queue Portal
- Admin/config management
- Compliance/audit read screens for ops context

Core menus:
- Dashboard
- Transactions
- Transfer Trace
- ISO Messages
- Outbox / Queue
- Retry / Stuck Events
- Participants
- Routing Rules
- Connector Configs
- Audit Logs
- Manual Actions

Primary users:
- `ADMIN`
- `OPS_READ`
- `OPS_WRITE`
- `AUDITOR`

Important controls:
- Retry/recover/mark-reviewed actions require write role.
- Viewing decrypted/plain ISO XML requires elevated permission and audit logging.
- All manual actions must create audit records.

### Portal 2 — Member Bank Portal

Purpose: each bank sees and manages only its own data.

Core menus:
- My Transactions
- My Inquiries
- My ISO Messages
- My Transfer Trace
- Download Reports
- Raise Dispute
- API / Certificate Info
- Bank Users

Primary users:
- `BANK_ADMIN`
- `BANK_USER`
- `BANK_VIEWER`

Important controls:
- Every query must be filtered by `bankCode` / participant scope.
- No cross-bank visibility.
- Plain/decrypted XML should be hidden by default or heavily restricted.
- Dispute creation can start here, but dispute operations live in the finance/dispute portal.

### Portal 3 — Finance, Settlement & Dispute Portal

Combines:
- Reconciliation / Settlement Portal
- DRS / Dispute Portal
- Finance-facing audit trail

Core menus:
- Daily Reconciliation
- Settlement Batches
- Bank Statement Import
- Mismatch Review
- Settlement Reports
- Dispute Cases
- Reversal / Adjustment
- Evidence / Timeline
- Approval Workflow
- Finance Audit Trail

Primary users:
- `FINANCE`
- `SETTLEMENT`
- `DISPUTE_AGENT`
- `DISPUTE_MANAGER`
- `COMPLIANCE`
- `AUDITOR`

Important controls:
- Maker/checker approval for settlement adjustment and reversal.
- Evidence and case timeline must be immutable or append-only.
- Settlement exports must be versioned.

### Portal 4 — Risk, Fraud & AML Portal

Purpose: separate sensitive risk/AML workflows from general operations.

Core menus:
- Suspicious Transactions
- Screening Alerts
- Watchlist Hits
- Risk Rules
- Alert Queue
- Case Investigation
- Analyst Notes
- False Positive / Confirmed Fraud
- SAR / AML Report Export
- Risk Audit Trail

Primary users:
- `RISK_ANALYST`
- `RISK_MANAGER`
- `AML_OFFICER`
- `COMPLIANCE`
- `AUDITOR`

Important controls:
- Risk/AML data should not be exposed in normal ops screens.
- Analyst decisions must be audited.
- Rule hit reasons and false-positive handling should be retained.

### External Tools Instead of Custom Portals

Use external tools for analytics and technical observability instead of building custom portals for everything.

BI tool:
- Metabase, Superset, or PowerBI.
- Used for volume, success rate, failed rate, bank performance, settlement summary, SLA, KPI.

Observability tool:
- Prometheus + Grafana.
- Used for API latency, outbox backlog, stuck events, transfer failures, connector health, DB health, JVM metrics.

Log search tool:
- Elasticsearch/Kibana or OpenSearch.
- Used for `requestId`, `transferRef`, `inquiryRef`, `messageId`, error stacks, incident investigation.

### Mapping From Original Portal Ideas

| Original idea | Target home |
|---|---|
| Operations Portal | Operations & Admin Portal |
| Member Bank Portal | Member Bank Portal |
| ISO Message Portal | Operations & Admin Portal + restricted Member Bank views |
| Transaction Trace Portal | Operations & Admin Portal + restricted Member Bank views |
| Outbox / Queue Portal | Operations & Admin Portal |
| Reconciliation / Settlement Portal | Finance, Settlement & Dispute Portal |
| DRS / Dispute Portal | Finance, Settlement & Dispute Portal |
| Risk / Fraud / AML Portal | Risk, Fraud & AML Portal |
| Compliance / Audit Portal | Operations & Admin + Finance + Risk, based on context |
| BI Dashboard | External BI tool |
| Log / Observability | Grafana/Prometheus + Kibana/OpenSearch |

### Portal Build Priority

1. Operations & Admin Portal
2. Member Bank Portal
3. Finance, Settlement & Dispute Portal
4. External BI/Observability/Log tools
5. Risk, Fraud & AML Portal

---

## 23. Phase 0 Deliverables Summary

| Deliverable | File | Status |
|-------------|------|--------|
| Risk Register | `docs/risk-register.md` | ✅ Created 2026-05-14 |
| Production Checklist | `docs/production-checklist.md` | ✅ Created 2026-05-14 |
| Endpoint Matrix | `docs/production-checklist.md` (Section 2) | ✅ Created 2026-05-14 |
| Advanced Roadmap | `overall.md` Section 21 | ✅ Created 2026-05-14 |
| Portal Architecture | `overall.md` Section 22 | ✅ Created 2026-05-14 |

**Risk summary (from `docs/risk-register.md`):**
| Severity | Count | Top priority |
|----------|-------|-------------|
| 🔴 Critical | 9 | RISK-SEC-001, RISK-DB-001, RISK-DB-002, RISK-TEST-001/002/003, RISK-OBS-002, RISK-BIZ-001, RISK-DB-003 |
| 🟠 High | 19 | See risk register |
| 🟡 Medium | 13 | See risk register |
| 🟢 Low | 1 | RISK-ISO-003 |

**Next step:** Start Phase 1 — migrate integration tests to Testcontainers + create CI pipeline.

---

## 24. Quick Reference: File Locations

| Purpose | File |
|---------|------|
| Main entry point | `src/main/java/com/example/switching/SwitchingApplication.java` |
| Create transfer (core logic) | `src/main/java/com/example/switching/transfer/service/CreateTransferService.java` |
| Outbox dispatch | `src/main/java/com/example/switching/outbox/service/OutboxProcessorService.java` |
| ISO crypto | `src/main/java/com/example/switching/iso/security/IsoMessageCryptoService.java` |
| Routing (with cache) | `src/main/java/com/example/switching/routing/service/RoutingService.java` |
| Operations trace (MDC + log.error) | `src/main/java/com/example/switching/operations/service/OperationsTransferTraceService.java` |
| Global error handler | `src/main/java/com/example/switching/common/exception/GlobalExceptionHandler.java` |
| Error catalog | `src/main/java/com/example/switching/common/error/ErrorCatalog.java` |
| DB migrations | `src/main/resources/db/migration/V1–V13__*.sql` |
| Test application config | `src/test/resources/application-test.yml` |
| Main application config | `src/main/resources/application.yml` |
| Full transfer flow test | `src/test/java/com/example/switching/transfer/FullTransferFlowIntegrationTest.java` |
| Operations trace test | `src/test/java/com/example/switching/operations/service/OperationsTransferTraceIntegrationTest.java` |
| Docker Compose | `docker-compose.yml` |
| Run script | `run.sh` |
| E2E curl tests | `scripts/curl_e2e_tests.sh` |
| MDC log correlation | `src/main/java/com/example/switching/common/filter/RequestIdFilter.java` |
| Outbox metrics + gauges | `src/main/java/com/example/switching/outbox/worker/OutboxDispatchWorker.java` |
| Transfer counters + MDC | `src/main/java/com/example/switching/transfer/service/CreateTransferService.java` |
| Dispatch timer + MDC | `src/main/java/com/example/switching/outbox/service/OutboxProcessorService.java` |
