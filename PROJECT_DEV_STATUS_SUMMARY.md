# Project Development Status Summary

Last updated: 2026-05-04  
Project path: `/Users/macbookpro/Desktop/Switching`

## Executive Summary

This project is a Java Spring Boot backend for an ISO 20022 payment switching system. The current backend is no longer just a basic prototype: it already contains the main domain modules for inquiry, transfer, ISO message handling, outbox dispatch, participant management, routing rules, connector configuration, audit logs, and operations APIs.

The project is currently at this stage:

```text
Backend core integration / production hardening preparation
```

The baseline test suite is currently green:

```bash
./mvnw clean test -DskipTests=false
```

Latest verified result:

```text
Tests run: 15
Failures: 0
Errors: 0
Skipped: 0
Build: SUCCESS
```

The next development goal should be production hardening of the backend switching path, not frontend work yet.

## Technology Stack

- Java 21
- Spring Boot 4.0.3
- Maven
- Spring Web MVC
- Spring Data JPA / Hibernate
- Flyway
- MySQL
- Spring Actuator
- Validation
- Docker / Docker Compose

Important files:

- `pom.xml`
- `docker-compose.yml`
- `Dockerfile`
- `src/main/resources/application.yml`
- `src/main/resources/application.properties`
- `src/main/resources/db/migration`

## Main Architecture

Package root:

```text
src/main/java/com/example/switching
```

Main modules:

```text
audit
common
config
connector
dashboard
demo
idempotency
inquiry
iso
operations
outbox
participant
routing
transfer
```

Target business flow:

```text
Inquiry
-> Transfer
-> PACS.008 ISO message
-> Encrypted ISO payload
-> Outbox event
-> Routing rule
-> Connector config
-> Bank connector
-> PACS.002 response
-> Transfer/outbox status update
-> Trace, audit, operations visibility
```

## What Has Been Developed

### 1. Inquiry Module

Status: implemented, but still mock-level for real bank/account validation.

Implemented:

- Create inquiry API.
- Inquiry lookup API.
- Inquiry status history.
- Inquiry to transfer query.
- Source bank validation through participant data.
- Destination bank availability check.
- Inquiry status: eligible or not eligible.

Important files:

- `src/main/java/com/example/switching/inquiry/service/CreateInquiryService.java`
- `src/main/java/com/example/switching/inquiry/controller/InquiryController.java`
- `src/main/java/com/example/switching/inquiry/entity/InquiryEntity.java`
- `src/main/java/com/example/switching/inquiry/entity/InquiryStatusHistoryEntity.java`

Production gap:

- Account validation currently accepts any non-empty creditor account.
- Real destination bank account inquiry integration is not implemented yet.

### 2. Transfer Module

Status: implemented for core flow.

Implemented:

- Create transfer API.
- Transfer list and lookup APIs.
- Transfer trace API.
- Inquiry validation before transfer.
- Prevents reusing the same inquiry for multiple transfers.
- Participant active-state validation.
- Route resolution through `RoutingService`.
- Stores route code and connector name on transfer.
- Creates transfer status history.
- Creates encrypted outbound PACS.008 ISO message.
- Enqueues outbox event for dispatch.
- Idempotency support.

Important files:

- `src/main/java/com/example/switching/transfer/service/CreateTransferService.java`
- `src/main/java/com/example/switching/transfer/service/TransferTraceService.java`
- `src/main/java/com/example/switching/transfer/entity/TransferEntity.java`
- `src/main/java/com/example/switching/transfer/entity/TransferStatusHistoryEntity.java`

Production gap:

- Need deeper tests for full inquiry-to-transfer-to-outbox lifecycle.
- Need stronger validation around amount, currency, accounts, and bank codes.
- Need idempotency conflict mapped to the intended domain error consistently.

### 3. ISO 20022 Module

Status: implemented at functional demo/backend-core level.

Implemented:

- ISO message table/entity.
- PACS.008 XML builder.
- PACS.002 XML builder.
- PACS.002 parser.
- ISO message query APIs.
- ISO message validation API.
- ISO message security policy API.
- AES/GCM encryption/decryption service.
- Encrypted outbound PACS.008 creation.
- Inbound PACS.002 persistence.

Important files:

- `src/main/java/com/example/switching/iso/service/IsoMessageService.java`
- `src/main/java/com/example/switching/iso/service/InboundPacs002MessageService.java`
- `src/main/java/com/example/switching/iso/service/IsoMessageSecurityService.java`
- `src/main/java/com/example/switching/iso/security/IsoMessageCryptoService.java`
- `src/main/java/com/example/switching/iso/mapper/Pacs008XmlBuilder.java`
- `src/main/java/com/example/switching/iso/mapper/Pacs002XmlBuilder.java`
- `src/main/java/com/example/switching/iso/parser/Pacs002Parser.java`

Recent progress:

- `IsoMessageSecurityService` now exists under the correct service package path:

```text
src/main/java/com/example/switching/iso/service/IsoMessageSecurityService.java
```

Production gap:

- ISO XML builders are simplified and need production-grade schema validation.
- Need official/controlled ISO 20022 schema validation strategy.
- Need stronger message correlation rules for `messageId`, `endToEndId`, `transferRef`, and inbound PACS.002.
- Decrypted payload handling needs security review.

### 4. Outbox Module

Status: implemented.

Implemented:

- Transactional outbox creation.
- Scheduled outbox dispatch worker.
- Scheduled stuck-processing recovery worker.
- Event claiming from `PENDING` to `PROCESSING`.
- Success/failure handling.
- Retry scheduling for retryable technical errors.
- Manual retry API.
- Outbox list/query APIs.
- Dispatch via encrypted ISO message.
- PACS.002 response handling.

Important files:

- `src/main/java/com/example/switching/outbox/service/OutboxTransactionService.java`
- `src/main/java/com/example/switching/outbox/service/OutboxProcessorService.java`
- `src/main/java/com/example/switching/outbox/service/OutboxIsoMessageDispatchService.java`
- `src/main/java/com/example/switching/outbox/service/OutboxRecoveryService.java`
- `src/main/java/com/example/switching/outbox/worker/OutboxDispatchWorker.java`
- `src/main/java/com/example/switching/outbox/worker/OutboxRecoveryWorker.java`

Production gap:

- Worker delay and retry settings are still hardcoded in code.
- Need integration tests for success, force reject, retryable technical failure, stuck recovery, and manual retry.
- Need schema consistency check for operational columns such as update/error timestamps.

### 5. Participant Module

Status: implemented.

Implemented:

- Participant CRUD-style management APIs.
- Participant list and lookup.
- Bank code normalization.
- Participant status support.
- Active participant validation.

Important files:

- `src/main/java/com/example/switching/participant/service/ParticipantService.java`
- `src/main/java/com/example/switching/participant/service/ParticipantManagementService.java`
- `src/main/java/com/example/switching/participant/controller/ParticipantController.java`
- `src/main/java/com/example/switching/participant/entity/ParticipantEntity.java`

Recent progress:

- Participant management tests are now passing.

Production gap:

- Need role-based access control before exposing management APIs in production.
- Need operational rules for maintenance state and cutover windows.

### 6. Routing Module

Status: implemented.

Implemented:

- Routing rule list and management APIs.
- Route resolution by:

```text
sourceBank + destinationBank + messageType
```

- Route cache.
- Cache clear endpoint.
- Participant validation during route resolution.
- Connector config validation when creating/updating route rules.

Important files:

- `src/main/java/com/example/switching/routing/service/RoutingService.java`
- `src/main/java/com/example/switching/routing/service/RoutingRuleManagementService.java`
- `src/main/java/com/example/switching/routing/controller/RoutingRuleController.java`
- `src/main/java/com/example/switching/routing/entity/RoutingRuleEntity.java`

Recent progress:

- Routing rule management tests are now passing.

Production gap:

- Need integration tests for missing route, disabled route, inactive participant, and route priority behavior.
- Need clear cache strategy for multi-instance deployment.

### 7. Connector Module

Status: partially implemented.

Implemented:

- Connector config entity/table/API.
- Connector registry.
- Config-driven connector resolution.
- Generic mock connector.
- Mock force-reject behavior through config.

Connector types:

```text
MOCK
HTTP
MQ
```

Important files:

- `src/main/java/com/example/switching/connector/registry/ConnectorRegistry.java`
- `src/main/java/com/example/switching/connector/service/ConnectorConfigService.java`
- `src/main/java/com/example/switching/connector/service/ConnectorConfigManagementService.java`
- `src/main/java/com/example/switching/connector/GenericMockConnector.java`
- `src/main/java/com/example/switching/connector/GenericHttpConnector.java`
- `src/main/java/com/example/switching/connector/GenericMqConnector.java`

Recent progress:

- Connector config management tests are now passing.

Production gap:

- `GenericHttpConnector` is still a placeholder.
- `GenericMqConnector` is still a placeholder.
- Real timeout handling, structured downstream errors, and PACS.002 HTTP/MQ response integration are not production-ready yet.

### 8. Audit Module

Status: implemented.

Implemented:

- Audit log entity.
- Audit log service.
- Audit log query service.
- Audit query API.
- Important business events are logged in inquiry, transfer, ISO, and outbox flow.

Important files:

- `src/main/java/com/example/switching/audit/service/AuditLogService.java`
- `src/main/java/com/example/switching/audit/service/AuditLogQueryService.java`
- `src/main/java/com/example/switching/audit/controller/AuditLogController.java`

Production gap:

- Need PII review for audit payloads.
- Need retention policy and search/indexing strategy.

### 9. Operations Module

Status: implemented broadly, needs hardening.

Implemented APIs/services include:

- Operations health.
- Dashboard summary.
- Bank status.
- Bank onboarding.
- Connector health/test.
- Transaction query.
- ISO message query.
- Audit log query.
- Outbox failure query.
- Retry all failed outbox events.
- Stuck outbox query.
- Recover stuck outbox events.
- Mark outbox reviewed.
- Generate routes for bank.

Important files:

- `src/main/java/com/example/switching/operations/controller`
- `src/main/java/com/example/switching/operations/service`
- `src/main/java/com/example/switching/operations/dto`

Production gap:

- Operations APIs need authentication and role-based authorization.
- Some operations SQL should be checked against the real Flyway schema.
- Dangerous operations such as retry/recover must be protected and audited.

### 10. Common Infrastructure

Status: implemented.

Implemented:

- Global exception handler.
- Standard API error response.
- Error catalog and classifier.
- Request ID filter.
- Request hash utility.
- Transfer reference generator.
- JSON/JPA/OpenAPI/web/scheduling config classes.

Important files:

- `src/main/java/com/example/switching/common/exception/GlobalExceptionHandler.java`
- `src/main/java/com/example/switching/common/error/ErrorCatalog.java`
- `src/main/java/com/example/switching/common/error/ErrorClassifier.java`
- `src/main/java/com/example/switching/common/filter/RequestIdFilter.java`

Production gap:

- Need broader error mapping tests.
- Need logging and metrics conventions for production operations.

## Database / Flyway Status

Current migrations:

```text
V1__init_switching_tables.sql
V2__seed_bank_and_routing.sql
V3__add_inquiry_tables.sql
V4__add_unique_constraint_transfers_inquiry_ref.sql
V5__add_updated_at_to_outbox_events.sql
V6__create_iso_messages.sql
V7__add_updated_at_to_idempotency_records.sql
V8__create_participants_and_routing_rules.sql
V9__create_connector_configs.sql
```

Important warning:

The local database validates at Flyway version 9 during tests, but the migration files should still be verified against a brand-new empty database. There are signs that older migrations and current entities may not be fully aligned.

Known concerns:

- Legacy `participant_banks` exists alongside newer `participants`.
- `routing_rules` appears in old and newer migration designs.
- Current entities expect fields that must be verified against migrations, such as `channel_id` in `idempotency_records`.
- Some operations queries reference operational outbox columns that should be checked against the actual schema.

Next database milestone:

```text
Run Flyway from zero on a clean MySQL schema and fix any migration drift safely.
```

## Test Status

Verified command:

```bash
./mvnw clean test -DskipTests=false
```

Verified result:

```text
BUILD SUCCESS
Tests run: 15
Failures: 0
Errors: 0
Skipped: 0
```

Existing tests cover:

- Spring application context.
- Participant management service.
- Connector config management service.
- Routing rule management service.

Missing tests:

- End-to-end inquiry to transfer flow.
- PACS.008 generation assertions.
- Outbox event payload assertions.
- Connector dispatch success.
- Connector force reject.
- Inbound PACS.002 persistence.
- Transfer trace completeness.
- Idempotency conflict.
- Flyway migration from empty schema.
- Operations APIs.

## Security / Production Risks

High-priority risks:

1. Secrets are present in configuration defaults.
2. No visible authentication/authorization layer.
3. HTTP and MQ connectors are not implemented.
4. Inquiry account validation is mock-only.
5. ISO XML handling is simplified.
6. Operations APIs include powerful actions and need protection.
7. Audit payloads may contain sensitive data.
8. Flyway/schema drift needs verification.

Files to review early:

- `src/main/resources/application.yml`
- `src/main/resources/application.properties`
- `docker-compose.yml`
- `src/main/resources/db/migration`

## Current Development Completion Estimate

Approximate backend progress:

```text
Architecture/modules:        75%
Core transfer flow:          60%
ISO 20022 handling:          45%
Outbox pattern:              65%
Participant/routing/config:  70%
Operations APIs:             55%
Production security:         10%
Real bank integration:        5%
Automated test coverage:     20%
```

Overall production readiness estimate:

```text
Around 35-45%
```

Reason:

The backend shape is strong and the baseline now builds successfully, but real production readiness requires security, schema cleanup, real connector implementation, deeper ISO validation, integration tests, and operational hardening.

## Recommended Next Development Order

### Phase 1: Baseline Hardening

Goal:

```text
Make the backend trustworthy before adding new features.
```

Tasks:

- Verify Flyway migrations on a clean database.
- Remove committed secrets from config defaults.
- Add test profile or isolated test database strategy.
- Disable noisy SQL logging by default outside local dev.
- Add CI command documentation.

### Phase 2: Core Flow Integration Tests

Goal:

```text
Prove the switching flow works end to end.
```

Test flow:

```text
Create inquiry
-> Create transfer
-> Create encrypted PACS.008
-> Create outbox event
-> Process outbox
-> Receive PACS.002
-> Update transfer status
-> Verify trace/audit
```

### Phase 3: Connector Implementation

Goal:

```text
Move from mock connector to real integration-ready connector layer.
```

Tasks:

- Implement HTTP connector.
- Define MQ connector technology.
- Add timeout and retry behavior.
- Add structured downstream error mapping.
- Add connector health checks.

### Phase 4: ISO 20022 Hardening

Goal:

```text
Make ISO messages valid, traceable, and operationally safe.
```

Tasks:

- Add schema validation.
- Add message correlation tests.
- Add malformed PACS.002 tests.
- Review encryption/decryption access policy.

### Phase 5: Operations Security

Goal:

```text
Make operations APIs safe for production use.
```

Tasks:

- Add authentication.
- Add role-based authorization.
- Protect retry/recover/management APIs.
- Add audit policy for all operational mutations.

### Phase 6: Frontend

Goal:

```text
Build Nuxt operations portal only after backend APIs are stable.
```

Suggested pages:

- Dashboard
- Transactions
- Transfer trace
- ISO messages
- Outbox events
- Participants
- Routing rules
- Connector configs
- Audit logs

## Useful Commands

Run tests:

```bash
./mvnw clean test -DskipTests=false
```

Start MySQL:

```bash
docker compose up -d mysql
```

Build/start app:

```bash
docker compose up --build app
```

Check git status:

```bash
git status --short --branch
```

## Useful API Areas

Core:

- `POST /api/inquiries`
- `GET /api/inquiries/{inquiryRef}`
- `POST /api/transfers`
- `GET /api/transfers`
- `GET /api/transfers/{transferRef}`
- `GET /api/transfers/{transferRef}/trace`

ISO/outbox:

- `GET /api/iso-messages`
- `GET /api/iso-messages/{messageKey}`
- `POST /api/iso-messages/{messageKey}/validate`
- `GET /api/outbox-events`
- `POST /api/outbox-events/{outboxEventId}/retry`

Management:

- `GET /api/participants`
- `POST /api/participants`
- `PATCH /api/participants/{bankCode}`
- `GET /api/routing-rules`
- `POST /api/routing-rules`
- `PATCH /api/routing-rules/{routeCode}`
- `GET /api/connector-configs`
- `POST /api/connector-configs`
- `PATCH /api/connector-configs/{connectorName}`

Operations:

- `GET /api/operations/health`
- `GET /api/operations/dashboard-summary`
- `GET /api/operations/transactions`
- `GET /api/operations/iso-messages`
- `GET /api/operations/audit-logs`
- `GET /api/operations/outbox-failures`
- `GET /api/operations/outbox-stuck`

## Final Development Summary

The project has a solid backend foundation and the current test baseline is green. Development has reached the point where the main modules are present and connected at a functional level.

The system is not production-ready yet. The next work should focus on hardening the backend: clean migration verification, security, real connectors, ISO validation, deeper integration tests, and operational safety.

Recommended immediate next task:

```text
Verify Flyway from a clean database, then add end-to-end tests for the inquiry -> transfer -> ISO -> outbox -> PACS.002 flow.
```

