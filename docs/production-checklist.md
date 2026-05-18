# LaoFP Switching API — Production Acceptance Checklist

> **Target Specification:** LaoFP Master System Specification v1.0 (LaoFP-MASTER-001)
> **Phase 0 — Production Baseline Freeze**
> Created: 2026-05-14 | Last updated: 2026-05-18 v2.9 (P9 mTLS Step 3 complete: V21 psp_certificates, MtlsCertificateValidator, MtlsFilter, LFP-2002, MtlsValidationIntegrationTest TC-ML-001–004 PASS; 115/115 green)
> Purpose: Go / No-Go criteria for each production readiness phase.
> Status legend: `[ ]` Not started · `[~]` In progress · `[x]` Done · `[!]` Blocked

---

## How to Use

Before each phase sign-off, every item in that phase's section must be `[x]` or explicitly `ACCEPTED` with a documented reason. Any `[ ]` or `[!]` is a **hard blocker** for that phase unless escalated to the risk register as ACCEPTED.

---

## Changelog

| Date | Version | Tests | Summary |
|------|---------|-------|---------|
| 2026-05-14 | v1.0 | 0 | Initial checklist created — P0 baseline freeze |
| 2026-05-15 | v1.5 | 60/60 | P1: Testcontainers migration, CI pipeline, Dockerfile hardening. P2: Profile separation, startup validators, demo key removal. P3: DB users, migration V15–V19. P5: outbox retry + backoff code. P6: structured logging. P7: K8s manifests, graceful shutdown code |
| 2026-05-15 | v1.9 | 60/60 | P9–P20 LaoFP expansion phases added with code-level detail (DDL V20–V50, Java class specs, API endpoints, config env vars, error codes, test class names) |
| 2026-05-18 | v2.0 | **76/76** | **P5 complete:** OutboxBackoffIntegrationTest (TC-BO-001–004), OutboxConcurrentDispatchIntegrationTest (TC-CC-001–002), IsoInquiryStatusHistoryIntegrationTest (TC-ISH-001–003), IdempotencyIntegrationTest (TC-IDEM-001–003). **P6:** 6 runbooks created (RB-01–RB-06). **P7:** OutboxWorkerShutdownTest (TC-SD-001–004), rollback procedure documented. **ISO path:** inquiry_status_history now written for ELIGIBLE/REJECTED/USED transitions. **SecurityConfig:** actuator endpoints restricted to OPS/ADMIN |
| 2026-05-18 | v2.1 | **80/80** | **P4:** ApiKeyRotationIntegrationTest (TC-KR-001–004) — old key hash not found after rotate(); `MaskingUtil.maskXmlAccounts()` added — regex masks `<DbtrAcct>`/`<CdtrAcct>` leaf `<Id>` values; applied in `IsoPacs008InboundService` + `IsoInquiryInboundService` debug log. **P7:** Trivy scan job added to CI pipeline (aquasecurity/trivy-action@0.28.0, HIGH+CRITICAL, blocks docker-push). **CI:** new integration tests added to ci.yml job matrix |
| 2026-05-18 | v2.2 | **82/82** | **P5 100%:** `IsoInquiryConcurrentIdempotencyIntegrationTest` TC-CI-001/002 — concurrent ACMT.023 race; `IsoInquiryInboundService` catches `DataIntegrityViolationException` on INSERT and uses `SELECT ... LOCK IN SHARE MODE` to return winner's `inquiry_ref` past REPEATABLE READ snapshot |
| 2026-05-18 | v2.3 | **Selected ops masking tests PASS** | **P4:** Operations API response masking added for transfer list/detail, transaction list, transfer trace transfer/inquiry, ISO inquiry query, and audit payload views. `MaskingUtil.maskAccountFieldsInText()` added for JSON-like audit payloads and ISO XML payloads in ops views. |
| 2026-05-18 | v2.4 | **SecurityAuthorizationIntegrationTest 8/8 PASS** | **P4:** Authorization coverage added for missing key → 401, BANK → operations 403, OPS read-only operations access, OPS blocked from ADMIN-only actions/config/API-key management, and ADMIN access to ADMIN-only paths. |
| 2026-05-18 | v2.5 | **RequestSignatureIntegrationTest 4/4 PASS** | **P9:** HMAC-SHA256 request signing foundation added behind `switching.security.signing.enabled`; `RequestSignatureFilter` protects bank-facing POST APIs; `LFP-2003` added for missing/bad/stale signatures. |
| 2026-05-18 | v2.6 | **OAuthTokenServiceIntegrationTest 5/5 PASS** | **P9:** V20 `oauth_clients` migration added with seeded BANK_A/B clients; `OAuthTokenService` added for create/validate/revoke signed bearer tokens and client secret hash verification; `LFP-2001` added for invalid OAuth tokens. |
| 2026-05-18 | v2.7 | **107/107 PASS** | **P9:** `OAuthTokenController` added — `POST /v1/oauth/token` (client_credentials grant, RFC 6749) + `POST /v1/oauth/token/revoke` (RFC 7009); `OAuthTokenResponse` DTO; `/v1/oauth/**` permitted in SecurityConfig; `jti` UUID claim added to `OAuthTokenService.createToken` to prevent same-second token collision; `OAuthTokenFlowIntegrationTest` TC-OA-001–005 PASS. |
| 2026-05-18 | v2.8 | **111/111 PASS** | **P9:** `OAuthTokenFilter` added — reads `Authorization: Bearer`, validates via `OAuthTokenService`, sets `SecurityContextHolder` with `ROLE_BANK` (pspId as principal); filter is skipped when no Bearer header so `ApiKeyAuthFilter` handles X-API-Key in parallel (dual-auth grace period); `SecurityConfig` updated with `oauthEnabled` flag and filter ordering: `OAuthTokenFilter → ApiKeyAuthFilter → RequestSignatureFilter`; `OAuthTokenFilterIntegrationTest` TC-OF-001–004 PASS. |
| 2026-05-18 | v2.9 | **115/115 PASS** | **P9 Step 3 mTLS:** V21 `psp_certificates` migration (cert_id PK, psp_id FK→participants, cert_fingerprint UNIQUE, status ACTIVE/REVOKED); `MtlsCertificateValidator` — URL-decode + parse X.509 PEM via CertificateFactory, SHA-256 fingerprint via HexFormat, DB lookup for ACTIVE/expiry check; `MtlsFilter` — rejects missing/invalid cert with LFP-2002, skips OAuth/actuator/swagger paths; `MtlsCertInvalidException` + `LFP-2002` added to ErrorCatalog & GlobalExceptionHandler; `SecurityConfig` wires `MtlsFilter` after `ApiKeyAuthFilter` when `mtlsEnabled`; `mtls.enabled` + `mtls.cert-header` config properties added; `MtlsValidationIntegrationTest` TC-ML-001–004 PASS (no cert 401, unknown cert 401, revoked cert 401, active cert passes). |

---

## Quick Status

> **Last verified:** 2026-05-18 · **Full test suite:** 111/111 PASS ✅ · **Checklist items done:** 191 / 669

**Foundation Phases — B2B Core Hardening**

| Phase | Title | Status | Completion | Blockers |
|-------|-------|--------|-----------|----------|
| P0 | Baseline Freeze | 🟢 Done | 90% | Team sign-off only |
| P1 | Test & CI Gate | 🟢 Done | 95% | PR branch protection (GitHub config) |
| P2 | Production Config | 🟢 Done | 95% | Secrets manager decision |
| P3 | DB & Migration Hardening | 🟡 In Progress | 65% | SSL, backup — infrastructure |
| P4 | Security Advanced | 🟡 In Progress | 85% | mTLS — infrastructure; role assignment UI/audit — future admin portal |
| P5 | Reliability & FPRE Foundation | 🟢 Done | 100% | All items complete ✅ |
| P6 | Observability | 🟡 In Progress | 55% | Prometheus/Grafana/alerts — infrastructure |
| P7 | Deployment & Runtime | 🟡 In Progress | 90% | Staging deploy/rollback drill — infrastructure |
| P8 | Compliance & Business | ⚪ Not Started | 0% | All business/DR/load items |

**What can be done in code vs infrastructure**

| Category | Status |
|----------|--------|
| ✅ Code complete | P0, P1, P2, P5 — all codeable items done |
| 🔧 Code remaining | P4: none in current API surface; role assignment UI/audit belongs to future admin portal |
| 🏗️ Infrastructure only | P3: DB SSL + backup; P6: Prometheus + Grafana + ELK; P7: staging drill; P8: all |

**LaoFP Expansion Phases — New Modules (not yet started)**

| Priority | Phase | Title | LaoFP Modules | Status | Depends On |
|----------|-------|-------|--------------|--------|------------|
| 1 | P9 | OAuth 2.0 + mTLS + Request Signing | MOD-01, MOD-02 | 🟡 75% | P8 gate |
| 2 | P10 | FPRE Full Compliance | MOD-21 | ⚪ 0% | P9 |
| 3 | P19 | AML / CFT & Risk Engine | MOD-12, MOD-13 | ⚪ 0% | P9, P10 |
| 4 | P12 | Webhook & Notification Engine | MOD-14 | ⚪ 0% | P9 |
| 5 | P13 | Prefunded Pool & Liquidity | MOD-11 | ⚪ 0% | P9, P10 |
| 6 | P14 | Settlement Engine (DNS + RTGS) | MOD-10 | ⚪ 0% | P13 |
| 7 | P11 | VPA / Account Lookup | MOD-06, MOD-03 | ⚪ 0% | P9 |
| 8 | P15 | QR Code Service | MOD-07 | ⚪ 0% | P11 |
| 9 | P16 | Bill Payment Service | MOD-08 | ⚪ 0% | P11 |
| 10 | P17 | Cross-border Payment | MOD-09 | ⚪ 0% | P13, P19 |
| 11 | P18 | Dispute & Refund Manager | MOD-15 | ⚪ 0% | P12 |
| 12 | P20 | Performance & Scale (2K→10K TPS) | MOD-01, MOD-04 | ⚪ 0% | All |

---

## Endpoint Classification Matrix

All API endpoints classified by audience, auth requirement, and production exposure.

### Bank-Facing (ROLE: BANK or ADMIN)

| Method | Path | Auth | Production? | Notes |
|--------|------|------|-------------|-------|
| POST | `/api/inquiries` | BANK / ADMIN | ✅ Yes (JSON path) | Requires `JSON_INITIATION_ENABLED=true` |
| GET | `/api/inquiries/{ref}` | BANK / OPS / ADMIN | ✅ Yes | |
| POST | `/api/transfers` | BANK / ADMIN | ✅ Yes | Core payment flow |
| GET | `/api/transfers` | BANK / OPS / ADMIN | ✅ Yes | |
| GET | `/api/transfers/{ref}` | BANK / OPS / ADMIN | ✅ Yes | |
| GET | `/api/transfers/{ref}/trace` | BANK / OPS / ADMIN | ✅ Yes | Limited bank view needed |
| POST | `/api/iso20022/pacs008` | BANK / ADMIN | ✅ Yes | Primary ISO inbound |
| POST | `/api/iso20022/acmt023` | BANK / ADMIN | ✅ Yes | ISO inquiry |
| GET | `/api/iso-messages` | BANK / OPS / ADMIN | ✅ Yes | |
| GET | `/api/iso-messages/{key}` | BANK / OPS / ADMIN | ✅ Yes | |
| GET | `/api/iso-inquiries/{ref}` | BANK / OPS / ADMIN | ✅ Yes | |
| GET | `/api/outbox-events` | OPS / ADMIN | ⚠️ OPS only | Banks should not see outbox |
| POST | `/api/outbox-events/{id}/retry` | OPS / ADMIN | ⚠️ OPS only | Manual intervention |

### Operations (ROLE: OPS or ADMIN)

| Method | Path | Auth | Production? | Notes |
|--------|------|------|-------------|-------|
| GET | `/api/operations/health` | OPS / ADMIN | ✅ Yes | |
| GET | `/api/operations/dashboard-summary` | OPS / ADMIN | ✅ Yes | |
| GET | `/api/operations/transactions` | OPS / ADMIN | ✅ Yes | |
| GET | `/api/operations/transfers` | OPS / ADMIN | ✅ Yes | |
| GET | `/api/operations/transfers/{ref}` | OPS / ADMIN | ✅ Yes | |
| GET | `/api/operations/transfers/{ref}/trace` | OPS / ADMIN | ✅ Yes | Full trace |
| GET | `/api/operations/iso-messages` | OPS / ADMIN | ✅ Yes | |
| GET | `/api/operations/iso-inquiries` | OPS / ADMIN | ✅ Yes | |
| GET | `/api/operations/iso-inquiries/{ref}` | OPS / ADMIN | ✅ Yes | |
| GET | `/api/operations/audit-logs` | OPS / ADMIN | ✅ Yes | |
| GET | `/api/operations/outbox-failures` | OPS / ADMIN | ✅ Yes | |
| GET | `/api/operations/outbox-stuck` | OPS / ADMIN | ✅ Yes | |
| POST | `/api/operations/outbox-failures/retry-all` | ADMIN | ⚠️ ADMIN only | Destructive batch action |
| POST | `/api/operations/outbox-events/{id}/mark-reviewed` | OPS / ADMIN | ✅ Yes | |
| POST | `/api/operations/outbox-stuck/recover-all` | ADMIN | ⚠️ ADMIN only | |
| GET | `/api/operations/bank-status` | OPS / ADMIN | ✅ Yes | |
| POST | `/api/operations/bank-onboarding` | ADMIN | ⚠️ ADMIN only | Creates participant + routing + connector |
| POST | `/api/operations/bank-onboarding/generate-routes` | ADMIN | ⚠️ ADMIN only | Generates missing inbound/outbound routing rules for an ACTIVE bank |
| GET | `/api/operations/connectors/health` | OPS / ADMIN | ✅ Yes | |
| POST | `/api/operations/connectors/{name}/test` | ADMIN | ⚠️ ADMIN only | |

### Admin / Config (ROLE: ADMIN only)

| Method | Path | Auth | Production? | Notes |
|--------|------|------|-------------|-------|
| GET | `/api/participants` | OPS / ADMIN | ✅ Yes | Read-only for OPS |
| GET | `/api/participants/{bankCode}` | OPS / ADMIN | ✅ Yes | |
| POST | `/api/participants` | ADMIN | ✅ Yes | |
| PATCH | `/api/participants/{bankCode}` | ADMIN | ✅ Yes | |
| GET | `/api/routing-rules` | OPS / ADMIN | ✅ Yes | |
| GET | `/api/routing-rules/resolve` | OPS / ADMIN | ✅ Yes | |
| POST | `/api/routing-rules` | ADMIN | ✅ Yes | |
| PATCH | `/api/routing-rules/{code}` | ADMIN | ✅ Yes | |
| POST | `/api/routing-rules/cache/clear` | ADMIN | ✅ Yes | |
| GET | `/api/connector-configs` | OPS / ADMIN | ✅ Yes | |
| GET | `/api/connector-configs/{name}` | OPS / ADMIN | ✅ Yes | |
| POST | `/api/connector-configs` | ADMIN | ✅ Yes | |
| PATCH | `/api/connector-configs/{name}` | ADMIN | ✅ Yes | |

### Infrastructure (No auth / Public)

| Method | Path | Auth | Production? | Notes |
|--------|------|------|-------------|-------|
| GET | `/actuator/health` | None | ✅ Yes | Load balancer probe |
| GET | `/actuator/info` | None | ✅ Yes | |
| GET | `/actuator/prometheus` | Internal only | ⚠️ Prometheus only | Not in current code — add in P6 |
| GET | `/actuator/metrics` | None currently | ❌ Restrict in prod | Should require auth or not be public |

---

## Phase 0 — Baseline Freeze Checklist

**Goal:** Know exactly what we have before hardening begins.

### Documentation
- [x] Overall architecture document exists (`overall.md`)
- [x] API endpoint list is complete (see matrix above)
- [x] Endpoint classification matrix created (this document)
- [x] Error catalog documented (`ErrorCatalog.java` + `overall.md` Section 9)
- [x] Risk register created (`docs/risk-register.md`)
- [ ] Production acceptance checklist reviewed by team lead
- [ ] Go/no-go production criteria agreed and signed off

### Core Flow Verification
- [x] JSON inquiry → transfer → outbox → trace works end-to-end
- [x] ISO ACMT.023 inquiry → ACMT.024 response works
- [x] ISO PACS.008 transfer → PACS.002 response works
- [x] Force reject flow works (connector_configs.force_reject=true)
- [x] Idempotency conflict returns 409 (not 500) ← fixed in last test run
- [x] OutboxEventNotFoundException returns 404 (not 500) ← fixed in last test run
- [x] Transfer trace shows inquiry for both JSON and ISO paths

### Known Gaps Inventoried
- [x] Integration tests fail on clean checkout (RISK-TEST-001)
- [x] Docker image builds skip tests (RISK-TEST-002)
- [x] App connects as MySQL root (RISK-DB-001)
- [x] DB connections use useSSL=false (RISK-DB-002)
- [x] API keys stored in plaintext (RISK-SEC-001)
- [x] Demo keys seeded in migrations (RISK-SEC-002)
- [x] No CI pipeline (RISK-TEST-003)
- [x] No Prometheus/Grafana (RISK-OBS-001)
- [x] No alerts (RISK-OBS-002)

**Phase 0 Exit Criteria:**
- [ ] All items above marked `[x]`
- [ ] Risk register reviewed by stakeholders
- [ ] Phase 1 start date agreed

---

## Phase 1 — Test & CI Gate Checklist

**Goal:** Clean machine → run all tests → build Docker image.

### Testcontainers Migration
- [x] `testcontainers-mysql` dependency added to `pom.xml` (TC 2.0.3 matching Spring Boot 4.0.3)
- [x] `AbstractIntegrationTest` base class created — singleton MySQL container + `@DynamicPropertySource`
- [x] `FullTransferFlowIntegrationTest` migrated (extends AbstractIntegrationTest)
- [x] `IsoInquiryFlowIntegrationTest` migrated
- [x] `IsoInquiryValidationIntegrationTest` migrated
- [x] `IsoInquiryExpiryIntegrationTest` migrated
- [x] `OperationsTransferTraceIntegrationTest` migrated
- [x] `OperationsTransferQueryIntegrationTest` migrated
- [x] `OperationsIsoInquiryQueryIntegrationTest` migrated
- [x] `SwitchingApplicationTests` migrated
- [x] `application-test.yml` uses `${TEST_DB_PASSWORD:}` default (overridden by DynamicPropertySource)
- [x] `./mvnw test` passes from a clean checkout with no local MySQL — **60/60 PASS** ✅

### Test Cleanup
- [ ] `@AfterEach` cleanup added to all integration tests (prevent data accumulation)
- [ ] Unit tests separated from integration tests (Maven Surefire groups or separate source sets)
- [ ] `scripts/run_tests.sh` passes against locally running app

### CI Pipeline
- [x] CI config file created: `.github/workflows/ci.yml`
- [x] CI runs on every `push` and `pull_request`
- [x] CI job 1: compile
- [x] CI job 2: unit tests (no DB)
- [x] CI job 3: integration tests (Testcontainers)
- [x] CI job 4: package JAR (only after tests pass)
- [x] CI job 5: Docker image build (only after package)
- [x] CI job 6: push to registry (only on `main` branch)
- [x] CI fails fast: each job uses `needs:` to block on failure
- [x] Test reports stored as CI artifacts
- [ ] PR branch protection rule configured (requires status checks to pass)

### Dockerfile Hardening
- [x] `Dockerfile` refactored to 3-stage build: `deps` → `build` → `runtime`
- [x] `runtime` stage uses `eclipse-temurin:21-jre` (JRE only, not JDK)
- [x] Non-root user `switching` created; container runs as `USER switching`
- [x] JVM container flags: `-XX:+UseContainerSupport`, `-XX:MaxRAMPercentage=75.0`, `-Djava.security.egd=file:/dev/./urandom`
- [x] `deps` stage caches `dependency:go-offline` — only re-runs on `pom.xml` change
- [x] `-DskipTests` in build stage acceptable: CI runs tests before Docker build via `needs:` chain
- [ ] Base image pinned to specific digest (not floating tag)
- [x] Trivy image scan integrated into CI (no CRITICAL CVEs) — `trivy-scan` job added to `.github/workflows/ci.yml` using `aquasecurity/trivy-action@0.28.0`; scans HIGH+CRITICAL, ignore-unfixed, blocks `docker-push`

### run.sh Updates
- [x] `docker:build` command added (build image only)
- [x] `docker:rebuild` command added (force rebuild + start)
- [x] `test:unit` command added (unit tests only, fast, no DB)
- [x] `status` command added (`docker compose ps`)
- [x] `test` / `test:unit` / `test:single` no longer call `load_env` (Testcontainers handles DB)

**Phase 1 Exit Criteria:**
- [x] `./mvnw test` passes on clean machine (no local MySQL) — **60/60 PASS**
- [x] CI pipeline created and blocks on failure
- [x] Dockerfile hardened (3-stage, non-root user, JVM flags)
- [x] Docker image built only after CI test gate passes
- [ ] PR branch protection enabled on `main`
- [ ] Unit tests separated from integration tests (Maven Surefire groups)

---

## Phase 2 — Production Configuration Checklist

**Goal:** Secrets never in code. Prod fails fast on missing config.

### Profile Separation
- [x] 4 Spring profiles defined and documented: `dev`, `test`, `staging`, `prod`
  - `application-dev.yml` — `show-sql=true`, `json-initiation=true` by default
  - `application-staging.yml` — requires `DB_URL`/`DB_USERNAME`, `MESSAGE_CRYPTO_KEY_BASE64` (no fallback)
  - `application-prod.yml` — strict: no defaults for secrets, `api-key.enabled` and `rate-limit.enabled` hardcoded to `true`
  - `application-test.yml` — API key disabled, rate limit disabled, crypto fallback allowed
- [x] `docker-compose.yml` sets `SPRING_PROFILES_ACTIVE: dev`
- [x] Production deployment uses `SPRING_PROFILES_ACTIVE=prod`

### Startup Guards (prod profile)
- [x] `MESSAGE_CRYPTO_KEY_BASE64` has no default in `application-prod.yml` → Spring fails at startup if unset
- [x] `DB_URL` and `DB_USERNAME` have no defaults in `application-prod.yml` → Spring fails if unset
- [x] `DB_PASSWORD` has no default (base `application.yml`) → Spring always fails if unset
- [x] `api-key.enabled: true` hardcoded in `application-prod.yml` (not overridable via env)
- [x] `rate-limit.enabled: true` hardcoded in `application-prod.yml` (not overridable via env)
- [x] `force-reject: false` hardcoded in `application-prod.yml` (mock flag cannot be enabled in prod)
- [x] `ProductionStartupValidator` (`@Profile("prod")`) — hard fails if DB URL contains `allowPublicKeyRetrieval=true` or points to `localhost`; warns if `json-initiation=true`
- [x] `IsoMessageCryptoService.resolveKey()` uses Spring `Environment.getActiveProfiles()` (fixed from fragile `System.getProperty`)
- [x] Actuator exposure: `health,info` only (base config, applies to all profiles)

### Demo Keys Removal
- [x] Demo API keys disabled in production — `ProductionDemoKeyDisableService` (`@Profile("prod")` `ApplicationRunner`) disables all demo keys by name+prefix on prod startup; warn log tells ops to provision a real ADMIN key
- [x] Production key provisioning procedure: on first prod deploy, `ProductionDemoKeyDisableService` auto-disables demo keys → ops must `POST /api/admin/api-keys` with an ADMIN key to create first real key (bootstrap: temporarily use root DB to insert one manually, or use the key before startup disables it)
- [x] No hardcoded credentials in `src/` — demo keys only appear in V14 migration (seed data, expected); `ApiKeyEntity.java` has only a doc comment example

### Secrets Management
- [ ] Decision made: Vault / AWS Secrets Manager / K8s Secrets / `.env` pipeline injection
- [x] `.env` file is in `.gitignore` and never committed
- [ ] Secrets rotation procedure documented

**Phase 2 Exit Criteria:**
- [x] Production cannot start with missing `MESSAGE_CRYPTO_KEY_BASE64`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- [x] `api-key.enabled` and `rate-limit.enabled` cannot be disabled in prod
- [x] Startup validator catches insecure DB URL config at boot
- [ ] Demo API keys disabled in production migration
- [ ] Secrets management approach decided and documented

---

## Phase 3 — Database & Migration Hardening Checklist

**Goal:** Fresh install predictable. DB user least-privilege. Backups working.

### DB User
- [x] `switching_app` DB user SQL defined — `scripts/init-db-users.sh` GRANT SELECT/INSERT/UPDATE/DELETE
- [x] `switching_flyway` DB user SQL defined — same script, GRANT ALL for migrations
- [x] Docker Compose updated — mounts `init-db-users.sh` into `mysql/docker-entrypoint-initdb.d`; app uses `switching_app`; Flyway uses `switching_flyway`
- [x] `application.yml` updated — `FLYWAY_URL/USERNAME/PASSWORD` env vars with fallback to datasource creds (backwards compatible for local Maven run)
- [ ] MySQL root password rotated after migration user setup (infrastructure task — do after first prod deploy)
- [ ] Connection tested: `switching_app` cannot DROP TABLE (verify after first `docker compose up`)

### DB Connections
- [ ] `useSSL=true` in prod JDBC URL
- [ ] `requireSSL=true` in prod JDBC URL
- [ ] MySQL server SSL certificate issued and configured
- [ ] App truststore configured for MySQL CA cert
- [ ] Connection string does not include `allowPublicKeyRetrieval=true` in prod

### Migrations
- [x] All migrations V1–V14 confirmed as frozen (no edits to historical migrations)
- [x] V15 seed migration: `participants` BANK_A, BANK_B, BANK_C (with `ON DUPLICATE KEY UPDATE`)
- [x] V15 seed migration: `routing_rules` for all 4 bidirectional BANK_A↔BANK_B/BANK_C routes (included in same V15)
- [x] Demo key disable handled via `ProductionDemoKeyDisableService` (`@Profile("prod")` ApplicationRunner) — no migration needed
- [x] V16 index migration: 6 performance indexes added (see Indexes section below)
- [x] V17 API key hardening migration: `key_prefix` column, `expires_at` column, `key_value` converted to SHA-256 hex (64 chars)
- [x] V18 maintenance migration: dropped duplicate `idx_outbox_events_status` index (created by both V1 and V16)
- [x] V19 compensating migration: `UPDATE participants SET participant_type = 'DIRECT'` for old `BANK`/`SWITCHING` rows; `INDIRECT` for `SERVICE_PROVIDER` rows — fixes enum mismatch on existing local DBs
- [ ] Flyway `validate` passes in all environments after migrations

### Indexes (V16)
- [x] `transfers (status, created_at DESC)` index added
- [x] `outbox_events (status, next_retry_at)` index added
- [x] `outbox_events (status, updated_at)` index added
- [x] `audit_logs (reference_id, created_at)` index added
- [x] `iso_messages (transfer_ref, direction)` index added
- [x] `idempotency_records (expired_at)` index added
- [ ] Index migration tested with EXPLAIN on all affected queries

### Backup & Recovery
- [ ] Automated daily backup script created and scheduled (cron or cloud job)
- [ ] Backup stored in separate storage (S3, GCS, or off-site)
- [ ] Backup retention policy defined and enforced (recommend: 30 days)
- [ ] MySQL binary logging enabled (`log_bin=ON`, `binlog_format=ROW`)
- [ ] Restore procedure documented step-by-step
- [ ] Restore drill completed (restore to test instance, verify data integrity)
- [ ] RPO target met: max 1 hour data loss with backup strategy

**Phase 3 Exit Criteria:**
- [ ] App connects as `switching_app` (not root) in all environments
- [x] Fresh install from migrations + V15 seed has working participants and routes
- [ ] DB backup and restore drill passed
- [ ] All indexes confirmed with `EXPLAIN` on key queries

---

## Phase 4 — Security Advanced Checklist

**Goal:** Bank/payment-grade authentication and data protection.

### API Key Security
- [x] `api_keys.key_value` stores SHA-256 hex digest (64 chars) — V17 migration converts existing plaintext keys via `SHA2(key_value, 256)`
- [x] `ApiKeyAuthFilter` hashes incoming `X-API-Key` header with `ApiKeyHashUtil.hash()` before DB lookup
- [x] Key is shown to user once (on creation/rotation via `ApiKeyService`) and never stored or retrievable again
- [x] `expires_at` column added to `api_keys` in V17 migration
- [x] Key expiry enforced in `ApiKeyAuthFilter` — keys past `expires_at` are rejected even if enabled
- [x] `key_prefix` column added (first 12 chars of original key) for display/identification without exposing full key
- [x] `GET /api/admin/api-keys` — list all keys (ADMIN only, no plaintext exposed)
- [x] `POST /api/admin/api-keys` — create key, `plainKey` returned once in response (ADMIN only)
- [x] `POST /api/admin/api-keys/{id}/disable` — disable key (ADMIN only)
- [x] `POST /api/admin/api-keys/{id}/rotate` — rotate key, new `plainKey` returned once (ADMIN only)
- [x] Key rotation tested: old key stops working within 1 request after rotation — `ApiKeyRotationIntegrationTest` TC-KR-001..004 verify `findByKeyValueAndEnabledTrue()` returns empty for old hash immediately after rotate()
- [x] `/api/admin/api-keys/**` endpoints tested end-to-end with real ADMIN key — `SecurityAuthorizationIntegrationTest.adminRoleCanAccessApiKeyManagement`

### Role Expansion
- [x] Production role list finalized for current API surface: `ADMIN`, `OPS`, `BANK`
- [x] Role-to-endpoint mapping updated in `SecurityConfig`
- [x] Role-to-endpoint mapping tested — `SecurityAuthorizationIntegrationTest` verifies 401/403/200 behavior for BANK, OPS, and ADMIN paths
- [ ] Role assignment UI exists in Operations Portal or admin API
- [ ] Role changes are audit-logged

### mTLS (Bank-Facing ISO Endpoints)
- [ ] `POST /api/iso20022/pacs008` requires client certificate
- [ ] `POST /api/iso20022/acmt023` requires client certificate
- [ ] Per-bank certificate management procedure documented
- [ ] `bank_certificates` table or column in `connector_configs` stores certificate fingerprint
- [ ] Certificate `CN`/`SAN` validated against `X-Bank-Code` header

### XML Security
- [x] `Acmt023XmlParser` has XXE protection (`FEATURE_SECURE_PROCESSING`, `disallow-doctype-decl`, external entities disabled)
- [x] `Pacs008InboundParser` has XXE protection (same flags as above)
- [x] XML body size limit configured (1MB): `server.tomcat.max-http-form-post-size: 1MB` in `application.yml`
- [ ] XXE penetration test performed and passed

### Data Masking
- [x] `MaskingUtil.maskAccount(String)` utility created — `common/util/MaskingUtil.java`, shows last 4 digits (e.g. `1234567890` → `******7890`)
- [x] `creditorAccount` masked in `CreateTransferService` audit log payloads (`TRANSFER_VALIDATE_REQUEST` + `TRANSFER_CREATED` events)
- [x] `creditorAccount` masked in `CreateInquiryService` audit payloads (`INQUIRY_VALIDATE_REQUEST` + `INQUIRY_CREATED`)
- [x] `creditorAccount` masked in `InquiryLookupService` audit payload (`INQUIRY_LOOKUP`)
- [x] `debtorAccount` + `creditorAccount` masked in `TransferInquiryService` audit payload (`TRANSFER_INQUIRY_LOOKUP`)
- [x] `creditorAccount` masked in `IsoInquiryInboundService` audit payloads (both `auditAcmt023InboundReceived` and `auditInquiryCreated`)
- [x] `OutboxProcessorService` log statements verified — only `outboxEventId`, `transferRef`, `errorCode` logged; no raw account numbers present
- [x] ISO XML payloads in logs have `<DbtrAcct>` and `<CdtrAcct>` masked — `MaskingUtil.maskXmlAccounts(String)` added (regex masks leaf `<Id>` within `<Othr>` in `<DbtrAcct>`/`<CdtrAcct>`); applied in `IsoPacs008InboundService` and `IsoInquiryInboundService` debug log lines
- [x] Operations APIs do not show full account numbers by default — masked in `/api/operations/transfers`, `/api/operations/transfers/{ref}`, `/api/operations/transfers/{ref}/trace`, `/api/operations/transactions`, `/api/operations/iso-inquiries`, and `/api/operations/audit-logs`
- [x] Ops portal does not show full account numbers without elevated permission

**Phase 4 Exit Criteria:**
- [ ] Security review checklist passed
- [ ] No plaintext API keys in DB
- [x] Sensitive data masked in all logs and audit views
- [ ] mTLS verified with bank simulator

---

## Phase 5 — Reliability & Outbox Advanced Checklist

**Goal:** No duplicate dispatch. No lost event. Idempotency solid.

### Multi-Instance Safety
- [x] Concurrency test: 2 app instances compete for same outbox event → only 1 wins — `OutboxConcurrentDispatchIntegrationTest` (TC-CC-001, TC-CC-002)
- [x] `UPDATE WHERE status='PENDING'` confirmed in claim logic — `OutboxEventRepository.claimPendingEvent(id, PENDING, PROCESSING)` atomic UPDATE
- [x] No duplicate `OUTBOX_DISPATCH_STARTED` audit entry for same event in concurrent test

### Retry & Backoff
- [x] `next_retry_at` populated consistently on each retry (`OutboxProcessorService.finalizeTechnicalFailure`)
- [x] Exponential backoff implemented: retry 1 → +30s, retry 2 → +2min, retry 3+ → +10min (`backoffDelay()`)
- [x] Outbox poller filters: `next_retry_at IS NULL OR next_retry_at <= NOW()` (`OutboxEventRepository.findPendingBatch`)
- [x] Backoff tested: retried event is not re-processed until `next_retry_at` — `OutboxBackoffIntegrationTest` TC-BO-004 verifies pending poll SQL excludes future `next_retry_at`

### Audit Trail for Manual Actions
- [x] `OUTBOX_MANUAL_RETRY_REQUESTED` audit log event written on every manual retry (`OutboxManualRetryService`) — includes outboxEventId, transferRef, previousStatus, newStatus, retryCount, manualAction=true
- [x] `OUTBOX_EVENT_MARKED_REVIEWED` audit log event written on every mark-reviewed (`OperationsOutboxMarkReviewedService`) — includes outboxEventId, transferRef, previousStatus, reason, reviewedBy, reviewedAt
- [x] Actor field populated from authenticated API key identity — `AuditActorUtil.currentActor()` reads `SecurityContextHolder`, falls back to `"SYSTEM"` for scheduled workers; applied in `OutboxManualRetryService` and `OperationsOutboxMarkReviewedService`

### Idempotency Tests
- [x] Concurrent POST `/api/transfers` with same inquiry ref → exactly 1 transfer created — `IdempotencyIntegrationTest` TC-IDEM-001 (UNIQUE constraint `uk_transfers_inquiry_ref` protects under concurrent load)
- [x] Sequential POST `/api/transfers` with same idempotencyKey + same payload → returns same transferRef — `IdempotencyIntegrationTest` TC-IDEM-002
- [x] Concurrent POST `/api/iso20022/pacs008` with same `MsgId` → exactly 1 ISO transfer created — `IdempotencyIntegrationTest` TC-IDEM-003
- [x] Concurrent POST `/api/iso20022/acmt023` with same `MsgId` → both threads return same inquiryRef, exactly 1 row — `IsoInquiryConcurrentIdempotencyIntegrationTest` TC-CI-001/002; race loser catches `DataIntegrityViolationException` and uses `LOCK IN SHARE MODE` to read winner's committed row (bypasses REPEATABLE READ snapshot)

### ISO Path Reliability
- [x] Inquiry TTL enforced: PACS.008 with expired `InquiryRef` is rejected — `IsoInquiryExpiryIntegrationTest.pacs008WithExpiredInquiryRefIsRejectedAndInquiryIsNotUsed` + `validateMandatoryIsoInquiry` in `InboundPacs008PersistenceService`
- [x] `inquiry_status_history` writes status transitions for ISO path — `IsoInquiryStatusHistoryIntegrationTest` TC-ISH-001/002/003; `IsoInquiryInboundService.writeStatusHistory()` for ELIGIBLE/REJECTED; `InboundPacs008PersistenceService.markInquiryUsed()` for USED transition

**Phase 5 Exit Criteria:**
- [x] Duplicate dispatch test passes under 2-instance concurrent load — `OutboxConcurrentDispatchIntegrationTest` TC-CC-001/002
- [x] Retry backoff confirmed in integration test — `OutboxBackoffIntegrationTest` TC-BO-001/002/003/004
- [x] All manual actions create audit records — `OUTBOX_MANUAL_RETRY_REQUESTED` + `OUTBOX_EVENT_MARKED_REVIEWED` verified

---

## Phase 6 — Observability Checklist

**Goal:** Incidents diagnosable in < 5 minutes from one transferRef.

### Metrics Export
- [x] `micrometer-registry-prometheus` dependency added to `pom.xml`
- [x] `/actuator/prometheus` endpoint exposed: staging → main port (8080); prod → management port (`${MANAGEMENT_PORT:9090}`, never public)
- [ ] Prometheus server configured to scrape app endpoint (infrastructure task)
- [ ] All existing Micrometer metrics visible in Prometheus UI (verify after Prometheus setup)

### Grafana Dashboards
- [ ] Dashboard 1: API Overview (req/s, p95 latency, error rate by endpoint)
- [ ] Dashboard 2: Transfer Health (created/s, success%, failed%, by bank pair)
- [ ] Dashboard 3: Outbox Monitor (pending count, stuck count, retry rate, dispatch latency)
- [ ] Dashboard 4: ISO Flow (ACMT.023/PACS.008 volume, parse errors, validation failures)
- [ ] Dashboard 5: Connector Health (bank availability, timeout rate, error rate per connector)
- [ ] Dashboard 6: DB & JVM (connection pool, GC, heap, thread count)

### Alerting
- [ ] Alert: `payment_outbox_pending_count > 100` for 5 min → PagerDuty
- [ ] Alert: `payment_outbox_processing_count > 5` for 2 min → PagerDuty
- [ ] Alert: transfer fail rate > 10% in 1-min window → Slack
- [ ] Alert: `NET-001/NET-002` count > 5 in 1 min → PagerDuty
- [ ] Alert: HTTP 5xx rate > 1% → Slack
- [ ] Alert: DB connection pool > 90% → PagerDuty
- [ ] Alert: p99 latency > 2s for 3 min → Slack
- [ ] All alerts connected to notification channel (Slack / LINE / PagerDuty)
- [ ] Alert silence and escalation policy documented

### Structured Logging
- [x] `logstash-logback-encoder` 8.0 added to `pom.xml`
- [x] `logback-spring.xml` created: text format for `default,dev,test`; JSON (`LogstashEncoder`) for `staging,prod`; `ShortenedThrowableConverter` with rootCauseFirst
- [x] All MDC fields included automatically in JSON log: `requestId`, `transferRef`, `inquiryRef`, `outboxEventId`, `bankCode` (LogstashEncoder includes all MDC keys by default)
- [x] No sensitive data (account numbers, API keys) in log JSON — MaskingUtil rollout covers audit payloads, ISO XML debug logs, and operations views

### Log Aggregation
- [ ] Log shipping configured (Fluentd / Filebeat → Elasticsearch / OpenSearch)
- [ ] Kibana/OpenSearch Dashboards accessible to ops team
- [ ] Search by `transferRef` returns all relevant log lines across app instances
- [ ] Log retention policy defined (recommend: 90 days hot, 1 year cold)

### Runbooks
- [x] Runbook: outbox backlog growing — `docs/runbooks/RB-01-outbox-backlog-growing.md`
- [x] Runbook: transfer failure spike — `docs/runbooks/RB-02-transfer-failure-spike.md`
- [x] Runbook: connector timeout/down — `docs/runbooks/RB-03-connector-timeout-down.md`
- [x] Runbook: DB connection exhausted — `docs/runbooks/RB-04-db-connection-exhausted.md`
- [x] Runbook: manual outbox retry procedure — `docs/runbooks/RB-05-manual-outbox-retry.md`
- [x] Runbook: emergency rollback procedure — `docs/runbooks/RB-06-emergency-rollback.md`

**Phase 6 Exit Criteria:**
- [ ] Ops can diagnose any transfer from one `transferRef` in Kibana
- [ ] All critical alerts fire correctly in staging (tested with synthetic failures)
- [ ] Runbooks reviewed by ops team

---

## Phase 7 — Deployment & Runtime Checklist

**Goal:** Deploy repeatably. Rollback safely. No traffic until ready.

### Container Hardening
- [x] `Dockerfile` adds non-root user (`switching`) and sets `USER switching` (Phase 1)
- [x] Container does not run as UID 0 (Phase 1)
- [x] Container image scanned with Trivy or Snyk (no CRITICAL CVEs) — Trivy CI job gates production push
- [ ] Base image pinned to specific digest (not `latest`)
- [x] Image size optimized (multi-stage build confirmed — 3-stage: deps → build → runtime)

### Kubernetes / Orchestration
- [x] `Deployment` manifest: `replicas: 2` (minimum HA) — `k8s/deployment.yaml`
- [x] `resources.requests` and `resources.limits` set — cpu: 250m/1000m, memory: 512Mi/1Gi
- [x] `livenessProbe` configured — GET `/actuator/health/liveness` port 9090, delay 60s
- [x] `readinessProbe` configured — GET `/actuator/health/readiness` port 9090, delay 30s
- [x] `readinessProbe` only green after DB + Flyway healthy (Flyway runs in initContainer first)
- [x] `HorizontalPodAutoscaler` configured — `k8s/hpa.yaml`: CPU 70%, Memory 80%, 2–8 pods
- [x] Flyway migration runs as `initContainer` before app pods start — `k8s/deployment.yaml`

### Graceful Shutdown
- [x] `OutboxDispatchWorker` handles shutdown signal: `volatile boolean shuttingDown` + `@PreDestroy` sets flag; both `onOutboxCreated` and `processPendingEvents` check before dispatching; mid-batch loop exits on flag
- [x] `server.shutdown: graceful` configured in `application.yml` — Spring HTTP drains in-flight requests before shutdown
- [x] `spring.lifecycle.timeout-per-shutdown-phase: 30s` configured in `application.yml`
- [x] Shutdown tested end-to-end: `OutboxWorkerShutdownTest` TC-SD-001/002/003/004 — verifies `shuttingDown` flag stops dispatch immediately; `processPendingEvents()` exits without DB query after `@PreDestroy`

### Zero-Downtime Deploy
- [x] Rolling update strategy: `maxUnavailable: 0, maxSurge: 1` — `k8s/deployment.yaml`
- [ ] Deploy tested: new version rolls out with zero dropped requests (verify with load test)
- [ ] Flyway migration backward-compatible (new columns nullable or with defaults)

### Rollback
- [x] Rollback procedure documented: `kubectl rollout undo deployment/switching-api` — `docs/runbooks/RB-06-emergency-rollback.md` §2 (decision table + kubectl steps)
- [x] DB rollback procedure documented (undo migration or compensating migration) — `docs/runbooks/RB-06-emergency-rollback.md` §5 (compensating migration approach)
- [ ] Rollback drill completed: deploy → verify → rollback → verify

**Phase 7 Exit Criteria:**
- [ ] Deploy and rollback drills pass in staging
- [ ] Container runs as non-root
- [ ] No traffic to new pod until readiness probe passes
- [ ] Graceful shutdown confirmed with outbox

---

## Phase 8 — Compliance & Business Readiness Checklist

**Goal:** Ready for bank/business operations. Regulatory requirements met.

### Data Retention & Privacy
- [ ] Data retention policy defined: transfers (10 years), audit logs (7 years), ISO messages (7 years)
- [ ] PII masking policy defined (account numbers, bank codes, names)
- [ ] Purge/archive job implemented and tested
- [ ] Data deletion request procedure (if applicable under local regulation)

### Reconciliation & Settlement
- [ ] Daily reconciliation job implemented
- [ ] Reconciliation compares switching DB records vs. bank settlement files
- [ ] Mismatch detection and flagging implemented
- [ ] Finance/Settlement Portal can access reconciliation results
- [ ] Reconciliation report tested with sample data

### Disaster Recovery
- [ ] RPO target defined: ≤ 1 hour (max acceptable data loss)
- [ ] RTO target defined: ≤ 4 hours (max time to restore service)
- [ ] DR runbook written and reviewed
- [ ] DR drill completed: simulate datacenter failure, restore from backup, measure time
- [ ] DR drill result meets RPO/RTO targets

### Load & Performance
- [ ] Load test tool selected (k6 or Gatling)
- [ ] Test scenarios defined: sustained (500/min), peak burst (1,000/min), soak (24h at 100 tps)
- [ ] Load test passed: p99 < 500ms at 500 transfers/min
- [ ] Soak test passed: 24h run, no memory leak, no error rate increase
- [ ] DB connection pool sized correctly under peak load

### Bank Onboarding
- [ ] Bank onboarding checklist documented (certificate setup, API key provisioning, routing config)
- [ ] Onboarding procedure tested with internal pilot bank
- [ ] ISO 20022 contract tests passed with bank simulator or partner test endpoint
- [ ] ISO certification criteria defined and met

### Go-Live Sign-Off
- [ ] Security review signed off by security team
- [ ] Penetration test passed (no CRITICAL/HIGH findings unresolved)
- [ ] Load test signed off by technical lead
- [ ] DR drill signed off by operations lead
- [ ] Runbooks reviewed and accepted by ops team
- [ ] Compliance review signed off (AML screening integration point defined)
- [ ] Business stakeholder sign-off obtained
- [ ] First pilot bank onboarding checklist completed

**Phase 8 Exit Criteria:**
- [ ] All above items `[x]`
- [ ] Go-live date agreed by all stakeholders
- [ ] On-call rotation established for first 30 days post go-live

---

## LaoFP Expansion — Execution Priority

| Priority | Phase | Name | Criticality | Depends On | Blocks |
|----------|-------|------|-------------|------------|--------|
| 1 | P9 | OAuth 2.0 + mTLS + Request Signing | **Critical** | P8 | All LaoFP phases |
| 2 | P10 | FPRE Full Compliance | **Critical** | P9 | P12, P13 |
| 3 | P19 | AML/CFT + Risk Engine | **Critical** | P9, P10 | P17 |
| 4 | P12 | Webhook & Notification Engine | High | P9 | P10, P11, P13–P18 |
| 5 | P13 | Prefunded Pool & Liquidity | High | P9, P10 | P14, P15, P16, P17 |
| 6 | P14 | Settlement Engine / DNS + RTGS | High | P9, P10, P13 | P15, P16 |
| 7 | P11 | VPA / Account Lookup | High | P9 | P15, P16, P17 |
| 8 | P15 | QR Code Service | Medium | P9, P11, P13 | — |
| 9 | P16 | Bill Payment Service | Medium | P9, P11, P13 | — |
| 10 | P17 | Cross-border Payment | Medium | P9, P11, P13, P19 | — |
| 11 | P18 | Dispute & Refund Manager | Medium | P9, P12, P13 | — |
| 12 | P20 | Performance & Scale | **Cert Gate** | All P9–P19 | Production go-live |

---

## Phase 9 — OAuth 2.0 + mTLS + Request Signing [CRITICAL — Priority 1]

**Spec refs:** LaoFP NFR-2.1 (OAuth), NFR-2.2 (mTLS), NFR-2.3 (HMAC-SHA256), MOD-01 (API Gateway), MOD-02 (IAM)
**Depends on:** P8 complete | **Blocks:** All subsequent LaoFP phases

### DB Migrations
- [x] **V20** — `oauth_clients`: `client_id PK`, `psp_id FK`, `client_secret_hash VARCHAR(64)`, `tier ENUM('TIER1','TIER2','TIER3')`, `scopes TEXT`, `created_at`, `expires_at`, `status ENUM('ACTIVE','REVOKED','SUSPENDED')`
- [x] **V21** — `psp_certificates`: `cert_id PK`, `psp_id FK`, `cert_fingerprint VARCHAR(64) UNIQUE`, `subject_dn TEXT`, `issued_at TIMESTAMP`, `expires_at TIMESTAMP`, `status ENUM('ACTIVE','REVOKED')`

### New Java Classes
- [x] `com.example.switching.security.oauth.OAuthTokenService` — `createToken(clientId, scopes)`, `validateToken(bearerToken)`, `revokeToken(token)` — `jti` UUID claim added for token uniqueness
- [x] `com.example.switching.security.oauth.OAuthTokenController` — `POST /v1/oauth/token` (client_credentials grant), `POST /v1/oauth/token/revoke`
- [x] `com.example.switching.security.oauth.OAuthTokenFilter extends OncePerRequestFilter` — validates `Authorization: Bearer`; populates `SecurityContextHolder` with `ROLE_BANK` (pspId as principal); skips if no Bearer header (grace period dual-auth)
- [x] `com.example.switching.security.mtls.MtlsCertificateValidator` — extracts `X-Client-Cert` header; verifies fingerprint against `psp_certificates`; throws `MtlsCertInvalidException` (LFP-2002)
- [x] `com.example.switching.security.mtls.MtlsFilter extends OncePerRequestFilter` — calls `MtlsCertificateValidator`; rejects with LFP-2002 on invalid/revoked cert
- [x] `com.example.switching.security.signing.HmacSignatureVerifier` — `verify(body, xRequestSignature, xTimestamp, clientSecret)` using HMAC-SHA256; timestamp skew >30s → LFP-2003
- [x] `com.example.switching.security.signing.RequestSignatureFilter extends OncePerRequestFilter` — reads `X-Request-Signature` + `X-Timestamp`; calls `HmacSignatureVerifier`
- [x] `SecurityConfig` updated — `oauthEnabled` + `mtlsEnabled` flags; filter order: `OAuthTokenFilter → ApiKeyAuthFilter → MtlsFilter → RequestSignatureFilter`; dual-auth grace period (Bearer + X-API-Key coexist)
- [ ] `com.example.switching.participant.service.ParticipantCredentialService` — `rotateCredentials(pspId)`, `registerCertificate(pspId, certPem)`, `revokeCertificate(certId)`

### API Endpoints
- [x] `POST /v1/oauth/token` — body: `grant_type=client_credentials&client_id=&client_secret=`; response: `{access_token, token_type:"Bearer", expires_in:3600, scope}`
- [x] `POST /v1/oauth/token/revoke` — body: `token=`; response: 200
- [ ] `POST /v1/participants/{pspId}/credentials/rotate` — response: `{clientId, clientSecret (plain, once only), newCertExpiry}`
- [ ] `POST /v1/participants/{pspId}/certificates/register` — body: `{certPem}`; response: `{certId, fingerprint, expiresAt}`

### Config Properties
- [x] `switching.security.oauth.enabled=true` — env: `SECURITY_OAUTH_ENABLED`
- [x] `switching.security.oauth.jwt-secret` — env: `OAUTH_JWT_SECRET` (min 256-bit, stored in k8s secret)
- [x] `switching.security.oauth.token-ttl-seconds=3600` — env: `OAUTH_TOKEN_TTL_SECONDS`
- [x] `switching.security.mtls.enabled=true` — env: `SECURITY_MTLS_ENABLED`
- [x] `switching.security.mtls.cert-header=X-Client-Cert` — env: `MTLS_CERT_HEADER`
- [x] `switching.security.signing.enabled=true` — env: `SECURITY_SIGNING_ENABLED`
- [x] `switching.security.signing.timestamp-tolerance-seconds=30` — env: `SIGNING_TIMESTAMP_TOLERANCE`

### Error Codes → Exception Classes
- [x] `LFP-2001 INVALID_OAUTH_TOKEN` → `OAuthTokenInvalidException` (HTTP 401) — add to `ErrorCatalog.java` + `GlobalExceptionHandler.java`
- [x] `LFP-2002 INVALID_MTLS_CERT` → `MtlsCertInvalidException` (HTTP 401)
- [x] `LFP-2003 REQUEST_SIGNATURE_INVALID` → `SignatureVerificationException` (HTTP 401) — covers both bad signature and timestamp skew >30s
- [ ] `LFP-2004 PARTICIPANT_SUSPENDED` → `ParticipantSuspendedException` (HTTP 403) — checked in `OAuthTokenFilter` on `oauth_clients.status`

### Integration Tests
- [x] `OAuthTokenFlowIntegrationTest` — TC-OA-001 happy path, TC-OA-002 token reuse, TC-OA-003 wrong grant_type 400, TC-OA-004 wrong secret 401 LFP-2001, TC-OA-005 revoke→reject
- [x] `OAuthTokenFilterIntegrationTest` — TC-OF-001 valid Bearer passes, TC-OF-002 tampered token 401 LFP-2001, TC-OF-003 revoked token 401 LFP-2001, TC-OF-004 no auth 401
- [x] `MtlsValidationIntegrationTest` — TC-ML-001 missing cert (401), TC-ML-002 unknown fingerprint (401), TC-ML-003 revoked cert (401), TC-ML-004 active cert passes
- [x] `RequestSignatureIntegrationTest` — missing header (401), stale timestamp >30s (401), invalid signature (401), valid signature passes
- [ ] `ParticipantCredentialRotationIntegrationTest` — rotate secret + cert; old token immediately rejected

**P9 Exit Criteria:**
- [ ] `X-API-Key` header now returns LFP-2001 (document grace period for PSPs)
- [ ] All POST/PUT endpoints require valid `X-Request-Signature` + `X-Timestamp` within ±30s
- [ ] `psp_certificates` table populated; fingerprint verified per request
- [ ] All 4 integration test classes green

---

## Phase 10 — FPRE Full Compliance [CRITICAL — Priority 2]

**Spec refs:** LaoFP B11 (FPRE flow), MOD-21 (FPRE Engine), FR-10.1–10.4
**Depends on:** P9 | **Blocks:** P12 (FPRE webhooks), P13 (pool hold on reversal)

### DB Migrations
- [ ] **V22** — `ALTER TABLE outbox_events ADD COLUMN failure_class ENUM('TRANSIENT','TRANSIENT_POOL','PERMANENT_BUSINESS','PERMANENT_COMPLIANCE','PERMANENT_EXPIRED','AMBIGUOUS') AFTER status; ADD COLUMN will_retry BOOLEAN DEFAULT FALSE; ADD COLUMN auto_reversal_triggered BOOLEAN DEFAULT FALSE; ADD COLUMN reversal_txn_id VARCHAR(36) NULL`
- [ ] **V23** — `reversal_log`: `reversal_id PK`, `original_txn_id VARCHAR(36)`, `reversal_txn_id VARCHAR(36)`, `triggered_at TIMESTAMP`, `reason ENUM('MAX_RETRIES','COMPLIANCE_BLOCK','EXPIRED')`, `status ENUM('INITIATED','COMPLETED','FAILED')`, `completed_at TIMESTAMP NULL`
- [ ] **V24** — `psp_suspension_log`: `suspension_id PK`, `psp_id FK`, `suspended_at TIMESTAMP`, `reversal_count INT`, `window_minutes INT DEFAULT 30`, `reinstated_at TIMESTAMP NULL`, `reinstated_by VARCHAR(100) NULL`
- [ ] **V25** — backfill: `UPDATE outbox_events SET failure_class='TRANSIENT' WHERE status='FAILED' AND failure_class IS NULL`

### New Java Classes
- [ ] `com.example.switching.outbox.model.FailureClass` — enum: `TRANSIENT, TRANSIENT_POOL, PERMANENT_BUSINESS, PERMANENT_COMPLIANCE, PERMANENT_EXPIRED, AMBIGUOUS`
- [ ] `com.example.switching.outbox.service.OutboxAmbiguousCheckService` — `checkCreditStatus(pspBaseUrl, txnId)`: calls `GET {pspBase}/laofp/transactions/{txnId}/credit-status`; returns `CreditStatusResponse(creditApplied: boolean, checkedAt: Instant)`
- [ ] `com.example.switching.outbox.service.OutboxAutoReversalService` — `triggerReversal(outboxEvent)`: creates reversal transfer, inserts `reversal_log`; emits `TRANSFER.REVERSING` → `TRANSFER.REVERSED` webhooks
- [ ] `com.example.switching.outbox.service.PspAutoSuspensionService` — `checkAndSuspend(pspId)`: counts `reversal_log` rows in 30-min window; if ≥3 → sets `psp_status='INBOUND_SUSPENDED'`, inserts `psp_suspension_log`, fires `PARTICIPANT.STATUS_CHANGED` webhook
- [ ] `com.example.switching.outbox.service.OutboxRetryScheduleService` — `computeNextRetry(attemptCount)`: delays [30s, 60s, 120s, 300s, 600s] each ±10% jitter via `ThreadLocalRandom`
- [ ] Update `OutboxProcessorService` — upgrade from 3-retry to 5-retry; classify `failureClass`; call `OutboxAmbiguousCheckService` when `failureClass=AMBIGUOUS`; call `OutboxAutoReversalService` after attempt 5 fails; delegate scheduling to `OutboxRetryScheduleService`

### Retry Schedule
- [ ] Attempt 1 → delay 30s ± 3s
- [ ] Attempt 2 → delay 60s ± 6s
- [ ] Attempt 3 → delay 120s ± 12s
- [ ] Attempt 4 → delay 300s ± 30s
- [ ] Attempt 5 → delay 600s ± 60s → failure → auto-reversal

### New API Endpoints
- [ ] `GET /v1/transfers/{txnId}/retry-status` — response: `{txnId, attemptCount, maxAttempts:5, nextRetryAt, failureClass, willAutoReverse, willRetry}`
- [ ] `GET /v1/transfers/{txnId}/retry-history` — response: `[{attempt, attemptedAt, failureClass, httpStatus}]`
- [ ] `GET /v1/transfers/pending` — all PENDING for calling PSP (scope: `transfers:read`)
- [ ] `GET /v1/transfers/failed` — all FAILED/retryable for calling PSP
- [ ] `GET /v1/fpre/health` — `{queueDepth, retrySuccessRate, avgResolutionMs, suspendedPspCount}`

### Config Properties
- [ ] `switching.fpre.retry-attempts=5` — env: `FPRE_RETRY_ATTEMPTS`
- [ ] `switching.fpre.retry-delays-seconds=30,60,120,300,600` — env: `FPRE_RETRY_DELAYS_SECONDS`
- [ ] `switching.fpre.jitter-percent=10` — env: `FPRE_JITTER_PERCENT`
- [ ] `switching.fpre.auto-reversal-enabled=true` — env: `FPRE_AUTO_REVERSAL_ENABLED`
- [ ] `switching.fpre.suspension-window-minutes=30` — env: `FPRE_SUSPENSION_WINDOW_MINUTES`
- [ ] `switching.fpre.suspension-reversal-threshold=3` — env: `FPRE_SUSPENSION_REVERSAL_THRESHOLD`

### Error Codes → Exception Classes
- [ ] `LFP-FPRE-001 MAX_RETRIES_REACHED` → `MaxRetriesExceededException` (HTTP 409)
- [ ] `LFP-FPRE-002 AUTO_REVERSAL_FAILED` → `AutoReversalException` (HTTP 500) — triggers critical alert
- [ ] `LFP-FPRE-003 AMBIGUOUS_STATE_UNRESOLVED` → `AmbiguousStateException` (HTTP 202)

### Integration Tests
- [ ] `FpreRetryScheduleIntegrationTest` — 5-step schedule verified; jitter applied; total max ~18 min
- [ ] `FpreAmbiguousCheckIntegrationTest` — mock PSP returns `creditApplied=true` → no re-push; `false` → re-push
- [ ] `FpreAutoReversalIntegrationTest` — attempt 5 fails → `reversal_log` created → REVERSING→REVERSED webhooks
- [ ] `PspAutoSuspensionIntegrationTest` — 3 reversals within 30 min → PSP suspended; `psp_suspension_log` row inserted

**P10 Exit Criteria:**
- [ ] 5-retry schedule with ±10% jitter; `failureClass` set on every failed outbox event
- [ ] AMBIGUOUS: PSP idempotency checked before re-push; `creditApplied=true` → mark COMPLETED without re-push
- [ ] Auto-reversal fires on attempt 5 failure; `reversal_log` row created
- [ ] PSP auto-suspension: ≥3 reversals/30 min → `INBOUND_SUSPENDED`; `PARTICIPANT.STATUS_CHANGED` webhook fired
- [ ] `willRetry` + `failureClass` present in all PENDING/FAILED API responses

---

## Phase 19 — AML/CFT + Risk Engine [CRITICAL — Priority 3]

**Spec refs:** LaoFP C1 (AML regulatory), MOD-12 (Risk & Fraud), MOD-13 (AML/CFT Screening)
**Depends on:** P9, P10 | **Blocks:** P17 (cross-border requires AML)

### DB Migrations
- [ ] **V26** — `sanctions_lists`: `list_id PK`, `list_type ENUM('BOL','OFAC','UN')`, `entity_name VARCHAR(500)`, `entity_type ENUM('PERSON','ENTITY')`, `identifiers JSONB`, `added_at TIMESTAMP`, `source_ref VARCHAR(100)`; index on `entity_name`
- [ ] **V27** — `sanctions_screening_results`: `screen_id PK`, `txn_id FK`, `screened_at TIMESTAMP`, `match_score DECIMAL(5,2)`, `match_entity TEXT NULL`, `list_type VARCHAR(10) NULL`, `outcome ENUM('CLEAR','BLOCKED','MANUAL_REVIEW')`, `screening_ms INT`
- [ ] **V28** — `str_reports`: `str_id PK`, `txn_id FK`, `triggered_at TIMESTAMP`, `submitted_at TIMESTAMP NULL`, `submission_ref VARCHAR(100) NULL`, `status ENUM('PENDING_SUBMISSION','SUBMITTED','ACKNOWLEDGED')`, `report_payload JSONB`
- [ ] **V29** — `fraud_scores`: `score_id PK`, `txn_id FK`, `scored_at TIMESTAMP`, `score DECIMAL(5,2)`, `risk_tier ENUM('LOW','MEDIUM','HIGH','CRITICAL')`, `signals JSONB`, `action_taken ENUM('ALLOW','FLAG','BLOCK')`
- [ ] **V30** — `velocity_checks`: `check_id PK`, `psp_id FK`, `check_type ENUM('AMOUNT_DAILY','COUNT_HOURLY','COUNT_DAILY')`, `window_start TIMESTAMP`, `current_value DECIMAL(20,2)`, `limit_value DECIMAL(20,2)`, `breached BOOLEAN`

### New Java Classes
- [ ] `com.example.switching.aml.service.SanctionsScreeningService` — `screen(txnId, debtorName, creditorName)`: queries `sanctions_lists` with fuzzy match; persists to `sanctions_screening_results`; total SLA <2s; returns `ScreeningResult(outcome, matchEntity, listType, screeningMs)`
- [ ] `com.example.switching.aml.service.SanctionsListSyncService` — `@Scheduled(cron="${switching.aml.sanctions-sync-cron}")`: `syncBoL()`, `syncOFAC()`, `syncUN()`; upserts `sanctions_lists`
- [ ] `com.example.switching.aml.service.StrGenerationService` — `generateStr(txnId, matchedEntity, listType)`: inserts `str_reports` row; `@Scheduled` every 5 min submits PENDING STRs to BoL FIU `POST {bolFiuUrl}/api/str/submit`; retries up to 24h
- [ ] `com.example.switching.risk.service.FraudScoringService` — `score(txnId, amount, sendingPspId, receivingPspId)`: combines velocity + anomaly signals; persists `fraud_scores`; returns `FraudScore(score, riskTier, signals)`
- [ ] `com.example.switching.risk.service.VelocityCheckService` — `checkVelocity(pspId, amount)`: upserts `velocity_checks`; returns `VelocityResult(withinLimits, breachedRule)`
- [ ] `com.example.switching.risk.service.RuleEngineService` — `evaluate(txnContext)`: evaluates DB-configured rules per PSP tier; returns `List<RuleResult>`
- [ ] Update `CreateTransferService` — inject `SanctionsScreeningService` + `FraudScoringService`; both called before `INITIATED`; `BLOCKED` outcome → throw `SanctionsBlockException`; fire `TRANSFER.BLOCKED` webhook
- [ ] Update `IsoPacs008InboundService` — same AML + fraud check on inbound ISO transfers

### API Endpoints (BoL admin only — scope: `compliance:read`)
- [ ] `GET /v1/compliance/sanctions/check?name=&txnId=` — manual name check
- [ ] `GET /v1/compliance/str/{strId}` — STR status + submission ref
- [ ] `GET /v1/compliance/velocity/{pspId}` — current velocity counters
- [ ] `GET /v1/risk/scores/{txnId}` — fraud score + signals

### Config Properties
- [ ] `switching.aml.screening-enabled=true` — env: `AML_SCREENING_ENABLED`
- [ ] `switching.aml.screening-timeout-ms=2000` — env: `AML_SCREENING_TIMEOUT_MS`
- [ ] `switching.aml.bol-fiu-url` — env: `BOL_FIU_URL`
- [ ] `switching.aml.bol-fiu-api-key` — env: `BOL_FIU_API_KEY`
- [ ] `switching.aml.str-submission-interval-minutes=5` — env: `AML_STR_SUBMISSION_INTERVAL`
- [ ] `switching.aml.sanctions-sync-cron=0 0 2 * * *` — env: `AML_SANCTIONS_SYNC_CRON` (02:00 ICT daily)
- [ ] `switching.risk.fraud-scoring-enabled=true` — env: `RISK_FRAUD_SCORING_ENABLED`
- [ ] `switching.risk.high-risk-threshold=0.75` — env: `RISK_HIGH_RISK_THRESHOLD`

### Error Codes → Exception Classes
- [ ] `LFP-SANCTIONS-001 SANCTIONS_HIT_BLOCKED` → `SanctionsBlockException` (HTTP 422)
- [ ] `LFP-SANCTIONS-002 SCREENING_TIMEOUT` → `ScreeningTimeoutException` (HTTP 503) — fail-open vs fail-closed per `AML_SCREENING_ENABLED`
- [ ] `LFP-RISK-001 HIGH_RISK_TRANSACTION_BLOCKED` → `HighRiskBlockException` (HTTP 422)
- [ ] `LFP-RISK-002 VELOCITY_LIMIT_EXCEEDED` → `VelocityLimitException` (HTTP 429)

### Integration Tests
- [ ] `SanctionsScreeningIntegrationTest` — known OFAC name → `BLOCKED` in <2s; `TRANSFER.BLOCKED` webhook; clean name → `CLEAR`
- [ ] `StrGenerationIntegrationTest` — sanctions hit → `str_reports` row; scheduler submits to mock BoL FIU; acknowledgement received
- [ ] `FraudScoringIntegrationTest` — high-velocity pattern → score >0.75 → transaction blocked (LFP-RISK-001)
- [ ] `VelocityCheckIntegrationTest` — 101 transfers in 1 hour → 101st → LFP-RISK-002

**P19 Exit Criteria:**
- [ ] Known sanctioned name blocked in <2s on all transaction entry points (`CreateTransferService`, `IsoPacs008InboundService`)
- [ ] `TRANSFER.BLOCKED` webhook fires with `blockReason` + `matchedEntity` fields
- [ ] STR row created within 24h of hit; `StrGenerationService` submits to BoL FIU; `status=SUBMITTED`
- [ ] Cross-border >LAK 5,000,000: `purposeCode` + `sourceOfFunds` validated by `SanctionsScreeningService`

---

## Phase 12 — Webhook & Notification Engine [HIGH — Priority 4]

**Spec refs:** LaoFP A7 (webhook events), MOD-14 (Notification Service)
**Depends on:** P9 | **Blocks:** P10, P11, P13–P18 event delivery

### DB Migrations
- [ ] **V31** — `webhook_registrations`: `webhook_id PK`, `psp_id FK`, `url VARCHAR(500)`, `events TEXT NOT NULL` (JSON array), `secret_hash VARCHAR(64)`, `status ENUM('ACTIVE','PAUSED','FAILED')`, `failed_deliveries INT DEFAULT 0`, `created_at TIMESTAMP`
- [ ] **V32** — `webhook_delivery_log`: `delivery_id PK`, `webhook_id FK`, `event_type VARCHAR(100)`, `payload JSONB`, `attempt_count INT DEFAULT 0`, `last_attempt_at TIMESTAMP`, `response_status INT NULL`, `delivered_at TIMESTAMP NULL`, `status ENUM('PENDING','DELIVERED','FAILED_FINAL')`

### New Java Classes
- [ ] `com.example.switching.webhook.model.WebhookRegistration` — entity mapped to `webhook_registrations`
- [ ] `com.example.switching.webhook.repository.WebhookRegistrationRepository`
- [ ] `com.example.switching.webhook.repository.WebhookDeliveryLogRepository`
- [ ] `com.example.switching.webhook.service.WebhookDeliveryService` — `deliver(pspId, eventType, payload)`: looks up active registrations matching `eventType`; inserts `webhook_delivery_log`; calls `WebhookHttpSender`
- [ ] `com.example.switching.webhook.service.WebhookHttpSender` — HTTP POST to PSP endpoint; adds `X-Webhook-Signature: HMAC-SHA256(secret, payload)`; timeout 5s; non-2xx → mark PENDING for retry
- [ ] `com.example.switching.webhook.service.WebhookRetryService` — `@Scheduled` every 30s; picks PENDING rows with `attempt_count < 5`; exponential backoff; auto-pauses webhook at `failed_deliveries >= 10`
- [ ] `com.example.switching.webhook.service.WebhookEventPublisher` — `publish(WebhookEvent)`: central injectable used by all domain services; routes to `WebhookDeliveryService`
- [ ] `com.example.switching.webhook.controller.WebhookController`

### API Endpoints
- [ ] `POST /v1/webhooks/register` — body: `{url, events:[], signingSecret}`; response: `{webhookId, status:"ACTIVE"}`
- [ ] `GET /v1/webhooks` — list all webhooks for calling PSP
- [ ] `GET /v1/webhooks/{webhookId}` — details + delivery stats (`failedDeliveries`, `lastDeliveredAt`)
- [ ] `DELETE /v1/webhooks/{webhookId}` — status → PAUSED
- [ ] `POST /v1/webhooks/{webhookId}/test` — fires `TEST.PING`; returns delivery status
- [ ] `GET /v1/notifications/{notifId}` — delivery log: status, attempts, responseStatus

### Event Coverage — all 20 LaoFP A7 events required
- [ ] `TRANSFER.INITIATED`, `TRANSFER.COMPLETED`, `TRANSFER.FAILED`, `TRANSFER.PENDING`
- [ ] `TRANSFER.RETRY_ATTEMPT` (attempt ≥3), `TRANSFER.MAX_RETRIES_REACHED`
- [ ] `TRANSFER.REVERSING`, `TRANSFER.REVERSED`, `TRANSFER.EXPIRED`, `TRANSFER.BLOCKED`
- [ ] `TRANSFER.POOL_HOLD_RELEASED`
- [ ] `QR.PAYMENT.COMPLETED` (P15), `BILL.PAYMENT.CONFIRMED` (P16)
- [ ] `SETTLEMENT.CYCLE.COMPLETED` (P14), `DISPUTE.STATUS_CHANGED` (P18)
- [ ] `LIQUIDITY.LOW_ALERT` (P13), `PARTICIPANT.STATUS_CHANGED` (P10 + P9)
- [ ] `TEST.PING` (test-fire endpoint)

### Config Properties
- [ ] `switching.webhook.delivery-timeout-ms=5000` — env: `WEBHOOK_DELIVERY_TIMEOUT_MS`
- [ ] `switching.webhook.retry-interval-seconds=30` — env: `WEBHOOK_RETRY_INTERVAL_SECONDS`
- [ ] `switching.webhook.max-delivery-attempts=5` — env: `WEBHOOK_MAX_DELIVERY_ATTEMPTS`
- [ ] `switching.webhook.auto-pause-threshold=10` — env: `WEBHOOK_AUTO_PAUSE_THRESHOLD`

### Error Codes → Exception Classes
- [ ] `LFP-7001 WEBHOOK_URL_UNREACHABLE` — delivery log status only (not transaction-blocking)
- [ ] `LFP-7002 WEBHOOK_EVENT_NOT_SUPPORTED` → `WebhookEventNotSupportedException` (HTTP 400) — on register with unknown event type

### Integration Tests
- [ ] `WebhookRegistrationIntegrationTest` — register, list, delete, test-fire; unknown event type → LFP-7002
- [ ] `WebhookDeliveryIntegrationTest` — transfer completes → `TRANSFER.COMPLETED` fired to mock PSP endpoint; `X-Webhook-Signature` verified
- [ ] `WebhookRetryIntegrationTest` — PSP endpoint returns 500; retry schedule fires; `failed_deliveries` increments; auto-pause at 10
- [ ] `WebhookAllEventTypesIntegrationTest` — stub all 20 event types; each fires with correct payload schema

**P12 Exit Criteria:**
- [ ] All 20 LaoFP A7 event types emit correctly-shaped JSON webhooks
- [ ] `X-Webhook-Signature: HMAC-SHA256` verifiable by PSP consumer
- [ ] Delivery retry works to 5 attempts; auto-pause at ≥10 failures
- [ ] `webhook_delivery_log` row created for every attempt

---

## Phase 13 — Prefunded Pool & Liquidity Management [HIGH — Priority 5]

**Spec refs:** LaoFP FR-3.1 (prefunded model), MOD-11 (Liquidity Manager)
**Depends on:** P9, P10 (reversal releases hold) | **Blocks:** P14, P15, P16, P17

### DB Migrations
- [ ] **V33** — `psp_pools`: `pool_id PK`, `psp_id FK UNIQUE`, `balance DECIMAL(20,4)`, `held_amount DECIMAL(20,4) DEFAULT 0`, `available_balance DECIMAL(20,4) GENERATED ALWAYS AS (balance - held_amount) STORED`, `currency CHAR(3) DEFAULT 'LAK'`, `minimum_balance DECIMAL(20,4)`, `alert_threshold_percent DECIMAL(5,2) DEFAULT 120`, `last_updated_at TIMESTAMP`
- [ ] **V34** — `pool_transactions`: `pool_txn_id PK`, `pool_id FK`, `txn_id FK`, `operation ENUM('HOLD','CONFIRM','RELEASE','TOPUP','ADJUSTMENT')`, `amount DECIMAL(20,4)`, `balance_before DECIMAL(20,4)`, `balance_after DECIMAL(20,4)`, `occurred_at TIMESTAMP`

### New Java Classes
- [ ] `com.example.switching.liquidity.service.PoolService` — `holdFunds(pspId, txnId, amount)`, `confirmHold(txnId)`, `releaseHold(txnId)`, `getAvailableBalance(pspId)`: all operations in single `@Transactional` with `SELECT ... FOR UPDATE`; throws `InsufficientPoolBalanceException` (LFP-4001) if `available_balance < amount`
- [ ] `com.example.switching.liquidity.service.LiquidityAlertService` — `@Scheduled` every 60s; queries pools where `available_balance < minimum_balance * (alert_threshold_percent/100)`; fires `LIQUIDITY.LOW_ALERT` via `WebhookEventPublisher`; throttle: 1 alert per PSP per 15 min via `last_alert_sent_at` column
- [ ] `com.example.switching.liquidity.service.PoolTopUpService` — `requestTopUp(pspId, amount)`: creates pending pool transaction; sends RTGS instruction via `RtgsGatewayService`
- [ ] `com.example.switching.liquidity.controller.LiquidityController`
- [ ] Update `CreateTransferService` — call `PoolService.holdFunds()` before routing; `confirmHold()` on COMPLETED; `releaseHold()` on reversal/failure

### API Endpoints
- [ ] `GET /v1/settlement/balance` — response: `{pspId, balance, heldAmount, availableBalance, currency, minimumBalance, lastUpdatedAt}`
- [ ] `POST /v1/settlement/liquidity/topup` — body: `{amount, reference}`; response: `{topupId, status:"PENDING_RTGS"}`
- [ ] `GET /v1/settlement/positions` — net positions per PSP for current cycle (BoL admin only)
- [ ] `GET /v1/settlement/pool-history` — paginated `pool_transactions` for calling PSP

### Config Properties
- [ ] `switching.liquidity.alert-check-interval-seconds=60` — env: `LIQUIDITY_ALERT_CHECK_INTERVAL`
- [ ] `switching.liquidity.alert-throttle-minutes=15` — env: `LIQUIDITY_ALERT_THROTTLE_MINUTES`
- [ ] `switching.liquidity.wallet-minimum-float=100000000` — env: `LIQUIDITY_WALLET_MINIMUM_FLOAT` (LAK 100M)

### Error Codes → Exception Classes
- [ ] `LFP-4001 INSUFFICIENT_POOL_BALANCE` → `InsufficientPoolBalanceException` (HTTP 422)
- [ ] `LFP-4002 POOL_HOLD_NOT_FOUND` → `PoolHoldNotFoundException` (HTTP 404) — on confirm/release of unknown txnId

### Integration Tests
- [ ] `PoolHoldIntegrationTest` — hold → confirm; hold → release restores balance; double-hold on same txnId → idempotent
- [ ] `PoolInsufficientBalanceIntegrationTest` — balance LAK 5M, transfer LAK 6M → LFP-4001; balance unchanged
- [ ] `LiquidityAlertIntegrationTest` — balance drops below 120% of minimum → `LIQUIDITY.LOW_ALERT` fires; second drop within 15 min → no duplicate
- [ ] `PoolConcurrencyIntegrationTest` — 50 concurrent transfers against pool of exactly 50× single transfer amount; exactly 50 succeed, 0 oversell

**P13 Exit Criteria:**
- [ ] No transfer routes without successful `holdFunds()`; `available_balance` never goes negative
- [ ] Wallet operator minimum float LAK 100M enforced; transfer blocked (LFP-4001) if `available_balance < amount`
- [ ] `LIQUIDITY.LOW_ALERT` fires at 120% of minimum balance; throttle prevents spam
- [ ] Pool balance restored exactly on reversal (`releaseHold()`)

---

## Phase 14 — Settlement Engine / DNS + RTGS [HIGH — Priority 6]

**Spec refs:** LaoFP FR-8.1–8.4, MOD-10 (Settlement Engine)
**Depends on:** P9, P10, P13 | **Blocks:** P15, P16

### DB Migrations
- [ ] **V35** — `settlement_cycles`: `cycle_id PK`, `cycle_name ENUM('CYCLE_1','CYCLE_2','CYCLE_3','CYCLE_4')`, `period_start TIMESTAMP`, `period_end TIMESTAMP`, `status ENUM('OPEN','CLOSING','COMPUTING','SETTLED')`, `settled_at TIMESTAMP NULL`
- [ ] **V36** — `settlement_positions`: `position_id PK`, `cycle_id FK`, `psp_id FK`, `net_position DECIMAL(20,4)`, `gross_credit DECIMAL(20,4)`, `gross_debit DECIMAL(20,4)`, `transaction_count INT`, `computed_at TIMESTAMP`
- [ ] **V37** — `settlement_instructions`: `instruction_id PK`, `cycle_id FK`, `debtor_psp_id FK`, `creditor_psp_id FK`, `net_amount DECIMAL(20,4)`, `instruction_ref VARCHAR(100)`, `rtgs_msg_id VARCHAR(100) NULL`, `status ENUM('PENDING','SENT_RTGS','CONFIRMED','FAILED')`
- [ ] **V38** — `ALTER TABLE transfers ADD COLUMN settlement_cycle_id FK REFERENCES settlement_cycles(cycle_id)` — tagged at INITIATED time
- [ ] **V39** — `settlement_reports`: `report_id PK`, `cycle_id FK`, `psp_id FK`, `report_type ENUM('CAMT054','REGULATORY')`, `report_content JSONB`, `generated_at TIMESTAMP`

### New Java Classes
- [ ] `com.example.switching.settlement.service.SettlementCycleService` — `@Scheduled` cron at 08:45/11:45/15:15/19:45 ICT: `closeCycle()`, then `computePositions()`, then `generateInstructions()`; opens next cycle immediately after close
- [ ] `com.example.switching.settlement.service.SettlementPositionService` — `computeNetPositions(cycleId)`: streams `transfers` grouped by PSP pair; <60s for 500K transactions (cursor-based batch); writes `settlement_positions`
- [ ] `com.example.switching.settlement.service.RtgsGatewayService` — `sendPacs009(instruction)`: builds ISO 20022 pacs.009 XML; POST to `{bolRtgsUrl}`; `POST /v1/settlement/rtgs-callback` receives confirmation; updates `settlement_instructions.status=CONFIRMED`
- [ ] `com.example.switching.settlement.service.Camt054ReportService` — `generateCamt054(cycleId, pspId)`: builds camt.054 XML; stores in `settlement_reports`; fires `SETTLEMENT.CYCLE.COMPLETED` webhook with download URL
- [ ] `com.example.switching.settlement.controller.SettlementController`
- [ ] Update `CreateTransferService` — tag `settlement_cycle_id` at INITIATED; query current OPEN cycle via `SettlementCycleService.getCurrentOpenCycle()`

### API Endpoints
- [ ] `GET /v1/settlement/cycles` — list cycles with `status`, `period_start`, `period_end`, `transactionCount`
- [ ] `GET /v1/settlement/cycles/{cycleId}` — details: gross credit/debit, net, settled_at
- [ ] `GET /v1/settlement/cycle/{cycleId}/report` — camt.054 XML download for calling PSP
- [ ] `GET /v1/reports/reconciliation/{date}` — daily summary; available by 22:00 ICT
- [ ] `GET /v1/reports/regulatory` — BoL regulatory report (scope: `regulatory:read`)
- [ ] `POST /v1/settlement/rtgs-callback` — RTGS confirmation from BoL (IP-restricted to BoL RTGS range)

### Config Properties
- [ ] `switching.settlement.cycle1-cutoff=08:45` — env: `SETTLEMENT_CYCLE1_CUTOFF`
- [ ] `switching.settlement.cycle2-cutoff=11:45` — env: `SETTLEMENT_CYCLE2_CUTOFF`
- [ ] `switching.settlement.cycle3-cutoff=15:15` — env: `SETTLEMENT_CYCLE3_CUTOFF`
- [ ] `switching.settlement.cycle4-cutoff=19:45` — env: `SETTLEMENT_CYCLE4_CUTOFF`
- [ ] `switching.settlement.rtgs-threshold-lak=500000000` — env: `SETTLEMENT_RTGS_THRESHOLD_LAK`
- [ ] `switching.settlement.bol-rtgs-url` — env: `BOL_RTGS_URL`
- [ ] `switching.settlement.rtgs-callback-ip-whitelist` — env: `RTGS_CALLBACK_IP_WHITELIST`

### Error Codes → Exception Classes
- [ ] `LFP-8001 SETTLEMENT_CYCLE_CLOSED` → `CycleClosedException` (HTTP 409)
- [ ] `LFP-8002 RTGS_SUBMISSION_FAILED` → `RtgsSubmissionException` (HTTP 502) — triggers critical alert; manual fallback procedure

### Integration Tests
- [ ] `SettlementCycleIntegrationTest` — open cycle, 10 transfers, trigger cutoff, net positions computed, instructions generated
- [ ] `RtgsHighValueIntegrationTest` — transfer LAK 600M → pacs.009 sent to mock BoL; callback → COMPLETED
- [ ] `Camt054ReportIntegrationTest` — cycle settles → camt.054 XML valid; `SETTLEMENT.CYCLE.COMPLETED` webhook fires
- [ ] `SettlementCycleTaggingIntegrationTest` — transfer tagged with `settlement_cycle_id` at INITIATED time; survives cycle cutoff

**P14 Exit Criteria:**
- [ ] 4 DNS cycles fire on schedule (cron verified in test)
- [ ] Net positions computed in <60s for test dataset
- [ ] Transfers >LAK 500M bypass DNS → RTGS path confirmed
- [ ] camt.054 XML valid against ISO schema
- [ ] `SETTLEMENT.CYCLE.COMPLETED` webhook delivered to all PSPs after each cycle

---

## Phase 11 — VPA / Account Lookup [HIGH — Priority 7]

**Spec refs:** LaoFP B1 (Account lookup flow), MOD-06 (Account Lookup Service)
**Depends on:** P9 | **Blocks:** P15, P16, P17

### DB Migrations
- [ ] **V40** — `vpa_registrations`: `vpa_id PK`, `vpa_type ENUM('MSISDN','NATIONAL_ID','EMAIL','QR_STATIC','MERCHANT_ID')`, `vpa_value VARCHAR(200)`, `psp_id FK`, `account_ref VARCHAR(200)`, `account_type ENUM('BANK_ACCOUNT','WALLET')`, `display_name VARCHAR(200)`, `is_primary BOOLEAN DEFAULT TRUE`, `status ENUM('ACTIVE','INACTIVE')`, `created_at TIMESTAMP`; `UNIQUE(vpa_type, vpa_value)` where `status='ACTIVE'`
- [ ] **V41** — `beneficiary_tokens`: `token_id VARCHAR(36) PK`, `vpa_id FK`, `issued_at TIMESTAMP`, `expires_at TIMESTAMP` (issued_at + 300s), `used BOOLEAN DEFAULT FALSE`, `used_at TIMESTAMP NULL`

### New Java Classes
- [ ] `com.example.switching.vpa.service.VpaRegistrationService` — `register(pspId, vpaType, vpaValue, accountRef)`, `update(vpaId, accountRef)`, `deregister(vpaId)`: deregister sets `status=INACTIVE`
- [ ] `com.example.switching.vpa.service.VpaLookupService` — `resolve(vpaType, vpaValue)`: queries `vpa_registrations`; creates `beneficiary_tokens` (5-min TTL); returns `LookupResult(beneficiaryToken, displayName, receivingPspId, accountType)`; SLA <500ms P95
- [ ] `com.example.switching.vpa.service.BeneficiaryTokenService` — `issue(vpaId)`, `validate(tokenId)`: throws `BeneficiaryTokenExpiredException` if past `expires_at`; `consume(tokenId)`: sets `used=true`
- [ ] `com.example.switching.vpa.controller.VpaController`
- [ ] Update `CreateTransferService` — accept `beneficiaryToken` in transfer request; call `BeneficiaryTokenService.validate()` + `consume()` at INITIATED

### API Endpoints
- [ ] `POST /v1/lookup/resolve` — body: `{vpaType, vpaValue}`; response: `{beneficiaryToken, displayName, receivingPspId, accountType, expiresAt}`; P95 <500ms
- [ ] `POST /v1/lookup/vpa/register` — body: `{vpaType, vpaValue, accountRef, accountType, displayName}`
- [ ] `PUT /v1/lookup/vpa/{vpaId}` — body: `{accountRef}`
- [ ] `DELETE /v1/lookup/vpa/{vpaId}` — status → INACTIVE
- [ ] `GET /v1/lookup/vpa/{vpaId}` — details (calling PSP only)
- [ ] Update `POST /v1/transfers/initiate` — accept `beneficiaryToken` field; validated and consumed on INITIATED

### Config Properties
- [ ] `switching.vpa.token-ttl-seconds=300` — env: `VPA_TOKEN_TTL_SECONDS`
- [ ] `switching.vpa.rate-limit-per-psp-per-minute=100` — env: `VPA_RATE_LIMIT_RPM`

### Error Codes → Exception Classes
- [ ] `LFP-3001 VPA_NOT_FOUND` → `VpaNotFoundException` (HTTP 404)
- [ ] `LFP-3002 VPA_DUPLICATE` → `VpaDuplicateException` (HTTP 409) — same value active at multiple PSPs
- [ ] `LFP-3003 BENEFICIARY_TOKEN_EXPIRED` → `BeneficiaryTokenExpiredException` (HTTP 422)
- [ ] `LFP-3004 BENEFICIARY_TOKEN_ALREADY_USED` → `BeneficiaryTokenUsedException` (HTTP 422)
- [ ] `LFP-5001 LOOKUP_RATE_LIMIT_EXCEEDED` — reuse existing `RateLimitException` (HTTP 429)

### Integration Tests
- [ ] `VpaRegistrationIntegrationTest` — register all 5 VPA types; duplicate rejected (LFP-3002); deregister → INACTIVE
- [ ] `VpaLookupIntegrationTest` — resolve known VPA <500ms; expired token (>5 min) → LFP-3003; used token → LFP-3004
- [ ] `VpaTransferIntegrationTest` — `beneficiaryToken` accepted in `/transfers/initiate`; token consumed; second use → LFP-3004
- [ ] `VpaLookupRateLimitIntegrationTest` — 101st lookup in 1 min from same PSP → LFP-5001

**P11 Exit Criteria:**
- [ ] VPA resolve P95 <500ms in test environment
- [ ] `beneficiaryToken` 5-min TTL enforced; consumed on use
- [ ] Duplicate VPA across PSPs rejected (LFP-3002)
- [ ] All 5 VPA types (MSISDN, NATIONAL_ID, EMAIL, QR_STATIC, MERCHANT_ID) register and resolve correctly

---

## Phase 15 — QR Code Service [MEDIUM — Priority 8]

**Spec refs:** LaoFP FR-5.1–5.4, MOD-07 (QR Code Service)
**Depends on:** P9, P11 (merchant VPA), P13 (pool debit) | **Blocks:** nothing

### DB Migrations
- [ ] **V42** — `qr_codes`: `qr_id PK`, `merchant_id VARCHAR(100)`, `psp_id FK`, `qr_type ENUM('STATIC','DYNAMIC')`, `payload_text TEXT`, `amount DECIMAL(20,4) NULL`, `currency CHAR(3) DEFAULT 'LAK'`, `txn_ref VARCHAR(100) NULL`, `UNIQUE(txn_ref)` for DYNAMIC, `expires_at TIMESTAMP NULL` (NULL = static), `used BOOLEAN DEFAULT FALSE`, `created_at TIMESTAMP`

### New Java Classes
- [ ] `com.example.switching.qr.service.QrGeneratorService` — `generateStatic(merchantId, pspId)`, `generateDynamic(merchantId, pspId, amount, txnRef)`: EMVCo QRCPS-MPM payload format; appends CRC-16/CCITT checksum
- [ ] `com.example.switching.qr.service.QrDecodeService` — `decode(qrPayload)`: parse EMVCo fields; verify CRC-16; check merchant ACTIVE; check `expires_at`; check `used=false` for DYNAMIC
- [ ] `com.example.switching.qr.service.QrPaymentService` — `pay(qrId, issuingPspId)`: calls `PoolService.holdFunds()`; routes INITIATED; on COMPLETED marks `used=true`; fires `QR.PAYMENT.COMPLETED` webhook to both PSPs
- [ ] `com.example.switching.qr.service.QrRefundService` — `refund(originalTxnId, amount)`: 30-day window check; creates reversal transfer
- [ ] `com.example.switching.qr.controller.QrController`

### API Endpoints
- [ ] `POST /v1/qr/generate/static` — body: `{merchantId, description}`; response: `{qrId, payload, qrImageUrl}`
- [ ] `POST /v1/qr/generate/dynamic` — body: `{merchantId, amount, currency, txnRef, expiresInSeconds}`; response: `{qrId, payload, expiresAt}`
- [ ] `POST /v1/qr/decode` — body: `{qrPayload}`; response: `{qrId, merchantId, amount, currency, valid, expiryStatus}`
- [ ] `POST /v1/qr/pay` — body: `{qrId, issuingPspId}`; response: `{txnId, status, completedAt}`
- [ ] `POST /v1/qr/refund` — body: `{originalTxnId, amount}`; response: `{refundTxnId, status}`

### Config Properties
- [ ] `switching.qr.sla-ms=10000` — env: `QR_SLA_MS` (10s P95 per FR-5.3)
- [ ] `switching.qr.dynamic-max-expiry-minutes=1440` — env: `QR_DYNAMIC_MAX_EXPIRY_MINUTES`

### Error Codes → Exception Classes
- [ ] `LFP-QR-001 QR_EXPIRED` → `QrExpiredException` (HTTP 422)
- [ ] `LFP-QR-002 QR_ALREADY_USED` → `QrAlreadyUsedException` (HTTP 422) — dynamic single-use enforced
- [ ] `LFP-QR-003 DUPLICATE_TXN_REF` → `DuplicateTxnRefException` (HTTP 409)
- [ ] `LFP-QR-004 MERCHANT_NOT_ACTIVE` → `MerchantNotActiveException` (HTTP 422)
- [ ] `LFP-QR-005 QR_CHECKSUM_FAIL` → `QrChecksumException` (HTTP 422)

### Integration Tests
- [ ] `QrGenerationIntegrationTest` — static + dynamic generated; CRC-16 verified; dynamic `txn_ref` UNIQUE enforced (LFP-QR-003)
- [ ] `QrPaymentIntegrationTest` — full Scan-Pay-Confirm; pool debited; `QR.PAYMENT.COMPLETED` fires; SLA measured <10s
- [ ] `QrRefundIntegrationTest` — refund within 30 days succeeds; day 31 → HTTP 422
- [ ] `QrSingleUseIntegrationTest` — second pay on same DYNAMIC QR → LFP-QR-002

**P15 Exit Criteria:**
- [ ] Static and dynamic QR generation with valid EMVCo + CRC-16 payload
- [ ] Cross-PSP QR payment works (issuing PSP ≠ acquiring PSP)
- [ ] Dynamic QR single-use enforced (LFP-QR-002)
- [ ] QR payment SLA <10s measured in load test

---

## Phase 16 — Bill Payment Service [MEDIUM — Priority 9]

**Spec refs:** LaoFP FR-6.1–6.3, MOD-08 (Bill Payment Service)
**Depends on:** P9, P13 (pool) | **Blocks:** nothing

### DB Migrations
- [ ] **V43** — `billers`: `biller_id PK`, `biller_code VARCHAR(50) UNIQUE`, `biller_name VARCHAR(200)`, `category ENUM('UTILITY','TELECOM','GOVERNMENT','LOAN','INSURANCE')`, `api_url VARCHAR(500)`, `api_key_hash VARCHAR(64)`, `timeout_seconds INT DEFAULT 30`, `status ENUM('ACTIVE','INACTIVE')`
- [ ] **V44** — `bill_tokens`: `token_id PK`, `biller_id FK`, `bill_ref VARCHAR(200)`, `bill_amount DECIMAL(20,4)`, `due_date DATE NULL`, `customer_name VARCHAR(200)`, `details JSONB`, `fetched_at TIMESTAMP`, `expires_at TIMESTAMP` (fetched_at + 10 min), `used BOOLEAN DEFAULT FALSE`
- [ ] **V45** — `bill_payments`: `payment_id PK`, `token_id FK`, `txn_id FK`, `biller_id FK`, `bill_ref VARCHAR(200)`, `receipt_number VARCHAR(200) NULL`, `status ENUM('INITIATED','CONFIRMED','FAILED')`, `initiated_at TIMESTAMP`, `confirmed_at TIMESTAMP NULL`; `UNIQUE(biller_id, bill_ref, DATE(initiated_at))` — 24h duplicate block

### New Java Classes
- [ ] `com.example.switching.billpayment.service.BillerService` — `findActiveBillers()`, `fetchBill(billerId, billRef)`: calls `GET {billerUrl}/bills/{ref}` with 30s timeout; creates `bill_tokens` row; returns `BillToken`
- [ ] `com.example.switching.billpayment.service.BillPaymentService` — `pay(tokenId, payingPspId)`: validates token not expired, not used, 24h dup check; calls `PoolService.holdFunds()`; sends `BILL.PAYMENT_INSTRUCTION` POST to biller API; 30s ACK; stores `receipt_number`; fires `BILL.PAYMENT.CONFIRMED` webhook
- [ ] `com.example.switching.billpayment.client.BillerApiClient` — HTTP client with `RestTemplate`/`WebClient`; configurable per-biller timeout
- [ ] `com.example.switching.billpayment.controller.BillPaymentController`

### API Endpoints
- [ ] `GET /v1/billers` — list ACTIVE billers: `[{billerId, billerCode, billerName, category}]`
- [ ] `GET /v1/billers/{billerId}` — biller details + supported bill reference formats
- [ ] `GET /v1/bills/fetch?billerId=&ref=` — response: `{billId, amount, dueDate, customerName, validUntil}` (10-min token)
- [ ] `POST /v1/bills/pay` — body: `{billId, payingPspId}`; response: `{paymentId, txnId, receiptNumber, status}`

### Config Properties
- [ ] `switching.billpayment.biller-api-timeout-seconds=30` — env: `BILLPAYMENT_BILLER_API_TIMEOUT`
- [ ] `switching.billpayment.token-ttl-minutes=10` — env: `BILLPAYMENT_TOKEN_TTL_MINUTES`
- [ ] `switching.billpayment.duplicate-window-hours=24` — env: `BILLPAYMENT_DUPLICATE_WINDOW_HOURS`

### Error Codes → Exception Classes
- [ ] `LFP-6001 BILL_NOT_FOUND` → `BillNotFoundException` (HTTP 404)
- [ ] `LFP-6002 BILL_TOKEN_EXPIRED` → `BillTokenExpiredException` (HTTP 422)
- [ ] `LFP-6003 DUPLICATE_BILL_PAYMENT` → `DuplicateBillPaymentException` (HTTP 409) — same billRef within 24h
- [ ] `LFP-6004 BILLER_TIMEOUT` → `BillerTimeoutException` (HTTP 504) — biller ACK >30s; FPRE retries within token window

### Integration Tests
- [ ] `BillFetchIntegrationTest` — fetch from mock biller; 10-min TTL enforced; unknown ref → LFP-6001
- [ ] `BillPaymentIntegrationTest` — full Fetch-Pay-Confirm; receipt returned; `BILL.PAYMENT.CONFIRMED` fires
- [ ] `BillDuplicatePaymentIntegrationTest` — same billRef twice in 24h → LFP-6003 on second
- [ ] `BillPaymentFpreIntegrationTest` — biller returns 500; FPRE retries within 10-min window; window expires → PSP must re-fetch

**P16 Exit Criteria:**
- [ ] Full Fetch-and-Pay with mock biller; receipt number in response
- [ ] 24h duplicate block enforced (LFP-6003)
- [ ] FPRE retry within 10-min token window; expired token → LFP-6002
- [ ] `BILL.PAYMENT.CONFIRMED` webhook fires on success

---

## Phase 17 — Cross-border Payment [MEDIUM — Priority 10]

**Spec refs:** LaoFP FR-7.1–7.4, MOD-09 (Cross-border Gateway)
**Depends on:** P9, P11, P13, **P19** (AML mandatory before routing) | **Blocks:** nothing

### DB Migrations
- [ ] **V46** — `fx_corridors`: `corridor_id PK`, `source_currency CHAR(3)`, `dest_currency CHAR(3)`, `target_network ENUM('PROMPTPAY','CNAPS','NAPAS','SWIFT')`, `min_amount DECIMAL(20,4)`, `max_amount DECIMAL(20,4)`, `fee_percent DECIMAL(5,4)`, `fee_fixed DECIMAL(20,4) DEFAULT 0`, `status ENUM('ACTIVE','SUSPENDED')`
- [ ] **V47** — `fx_quotes`: `quote_id PK`, `corridor_id FK`, `source_amount DECIMAL(20,4)`, `dest_amount DECIMAL(20,4)`, `rate DECIMAL(20,8)`, `fee DECIMAL(20,4)`, `issued_at TIMESTAMP`, `expires_at TIMESTAMP` (issued_at + 30s), `used BOOLEAN DEFAULT FALSE`
- [ ] **V48** — `crossborder_transfers`: `cb_id PK`, `txn_id FK`, `quote_id FK`, `purpose_code VARCHAR(50) NULL`, `source_of_funds VARCHAR(200) NULL`, `beneficiary_name VARCHAR(200)`, `beneficiary_bank VARCHAR(200)`, `beneficiary_account VARCHAR(200)`, `target_network VARCHAR(50)`, `network_txn_id VARCHAR(200) NULL`, `compliance_check_id FK`

### New Java Classes
- [ ] `com.example.switching.crossborder.service.FxQuoteService` — `getIndicativeRates(from, to)`, `createQuote(corridorId, amount)`: 30-second binding quote; stores to `fx_quotes`
- [ ] `com.example.switching.crossborder.service.CrossBorderTransferService` — `initiate(quoteId, beneficiary, purposeCode, sourceOfFunds, pspId)`: validates quote not expired; enforces `purposeCode` + `sourceOfFunds` for >LAK 5M (LFP-CB-003); calls `SanctionsScreeningService`; routes to corridor adapter
- [ ] `com.example.switching.crossborder.adapter.PromptPayAdapter` — `send(cb)` to PromptPay API
- [ ] `com.example.switching.crossborder.adapter.CnapsAdapter` — to China CNAPS
- [ ] `com.example.switching.crossborder.adapter.NapasAdapter` — to Vietnam NAPAS
- [ ] `com.example.switching.crossborder.adapter.SwiftAdapter` — to SWIFT MT103/gpi
- [ ] `com.example.switching.crossborder.controller.CrossBorderController`

### API Endpoints
- [ ] `GET /v1/crossborder/fx-rates?from=LAK&to=THB` — response: `{from, to, indicativeRate, spread, validFor:30}` (indicative only)
- [ ] `POST /v1/crossborder/quote` — body: `{corridorId, amount, currency}`; response: `{quoteId, rate, fee, destAmount, expiresAt}` (30s binding)
- [ ] `POST /v1/crossborder/initiate` — body: `{quoteId, beneficiary:{name,bank,account,country}, purposeCode, sourceOfFunds}`
- [ ] `GET /v1/crossborder/corridors` — active corridors: network, currencies, limits, fees

### Config Properties
- [ ] `switching.crossborder.quote-ttl-seconds=30` — env: `CROSSBORDER_QUOTE_TTL_SECONDS`
- [ ] `switching.crossborder.purpose-code-threshold-lak=5000000` — env: `CROSSBORDER_PURPOSE_CODE_THRESHOLD`
- [ ] `switching.crossborder.promptpay.url` — env: `PROMPTPAY_API_URL`
- [ ] `switching.crossborder.cnaps.url` — env: `CNAPS_API_URL`
- [ ] `switching.crossborder.napas.url` — env: `NAPAS_API_URL`
- [ ] `switching.crossborder.swift.bic` — env: `SWIFT_BIC`

### Error Codes → Exception Classes
- [ ] `LFP-CB-001 FX_QUOTE_EXPIRED` → `FxQuoteExpiredException` (HTTP 422)
- [ ] `LFP-CB-002 CORRIDOR_NOT_AVAILABLE` → `CorridorNotAvailableException` (HTTP 422)
- [ ] `LFP-CB-003 PURPOSE_CODE_REQUIRED` → `PurposeCodeRequiredException` (HTTP 422) — amount >LAK 5M
- [ ] `LFP-CB-004 CROSSBORDER_SANCTIONS_HIT` — reuses `SanctionsBlockException` from P19 (HTTP 422)

### Integration Tests
- [ ] `FxQuoteIntegrationTest` — quote issued; 30s expiry enforced → LFP-CB-001; corridor rates correct
- [ ] `CrossBorderInitiateIntegrationTest` — happy path: quote → initiate → routed to mock PromptPay; AML called
- [ ] `PurposeCodeRequiredIntegrationTest` — LAK 6M without `purposeCode` → LFP-CB-003
- [ ] `CrossBorderAmlBlockIntegrationTest` — sanctioned beneficiary → LFP-CB-004 before corridor routing

**P17 Exit Criteria:**
- [ ] All 4 corridor adapters implemented (mock or real); routing by `target_network` correct
- [ ] FX quote 30s binding expiry enforced
- [ ] AML screening called before routing; sanctioned names blocked
- [ ] `purposeCode` + `sourceOfFunds` required and enforced for >LAK 5M

---

## Phase 18 — Dispute & Refund Manager [MEDIUM — Priority 11]

**Spec refs:** LaoFP FR-9.1–9.3, MOD-15 (Dispute & Refund Manager)
**Depends on:** P9, P12 (webhooks), P13 (pool for auto-refund) | **Blocks:** nothing

### DB Migrations
- [ ] **V49** — `disputes`: `dispute_id PK`, `txn_id FK`, `raising_psp_id FK`, `responding_psp_id FK`, `dispute_type ENUM('NOT_RECEIVED','WRONG_AMOUNT','DUPLICATE_CHARGE','FRAUD','MERCHANT_DISPUTE','TECHNICAL_ERROR')`, `status ENUM('OPEN','AWAITING_RESPONSE','UNDER_REVIEW','RESOLVED_REFUND','RESOLVED_NO_ACTION','ESCALATED','CLOSED')`, `raised_at TIMESTAMP`, `sla_deadline TIMESTAMP`, `resolved_at TIMESTAMP NULL`, `evidence JSONB DEFAULT '[]'`, `auto_ruled BOOLEAN DEFAULT FALSE`
- [ ] **V50** — `refund_transactions`: `refund_id PK`, `dispute_id FK NULL`, `original_txn_id FK`, `refund_txn_id FK NULL`, `amount DECIMAL(20,4)`, `status ENUM('INITIATED','COMPLETED','FAILED')`, `initiated_at TIMESTAMP`, `completed_at TIMESTAMP NULL`

### New Java Classes
- [ ] `com.example.switching.dispute.service.DisputeRaiseService` — `raise(txnId, disputeType, raisingPspId, evidence)`: enforces 90-day window; computes `sla_deadline` by type; inserts `disputes`; fires `DISPUTE.STATUS_CHANGED {status:"OPEN"}` webhook
- [ ] `com.example.switching.dispute.service.DisputeResolutionService` — `respond(disputeId, pspId, evidence)`, `resolve(disputeId, pspId, decision)`: `RESOLVED_REFUND` → call `DisputeAutoRefundService`; fires `DISPUTE.STATUS_CHANGED` webhook to both PSPs
- [ ] `com.example.switching.dispute.service.DisputeSlaEnforcementService` — `@Scheduled` every 10 min; finds `AWAITING_RESPONSE` disputes past `sla_deadline`; sets `auto_ruled=true`; resolves in favour of raising PSP; fires webhook
- [ ] `com.example.switching.dispute.service.DisputeAutoRefundService` — `initiateRefund(disputeId)`: inserts `refund_transactions`; calls `PoolService.releaseHold()` on responding PSP; `holdFunds()` on raising PSP; fires `TRANSFER.REVERSED` or `DISPUTE.REFUND.COMPLETED`
- [ ] `com.example.switching.dispute.controller.DisputeController`

### SLA by Dispute Type (encoded in `DisputeSlaEnforcementService.computeSlaDeadline`)
- [ ] `TECHNICAL_ERROR` → 1 business day
- [ ] `NOT_RECEIVED` → 2 business days
- [ ] `WRONG_AMOUNT` → 3 business days
- [ ] `FRAUD`, `MERCHANT_DISPUTE`, `DUPLICATE_CHARGE` → 5 business days

### API Endpoints
- [ ] `POST /v1/disputes/raise` — body: `{txnId, disputeType, description, evidence:[]}`; response: `{disputeId, status:"OPEN", slaDeadline}`
- [ ] `GET /v1/disputes/{disputeId}` — full details + timeline + evidence
- [ ] `PUT /v1/disputes/{disputeId}/respond` — body: `{evidence:[]}` (responding PSP only)
- [ ] `POST /v1/disputes/{disputeId}/resolve` — body: `{decision:"REFUND"|"NO_ACTION", note}`; response: `{disputeId, status, refundTxnId?}`
- [ ] `GET /v1/disputes` — paginated list for calling PSP

### Config Properties
- [ ] `switching.dispute.window-days=90` — env: `DISPUTE_WINDOW_DAYS`
- [ ] `switching.dispute.sla-check-interval-minutes=10` — env: `DISPUTE_SLA_CHECK_INTERVAL`
- [ ] `switching.dispute.retention-years=7` — env: `DISPUTE_RETENTION_YEARS`

### Error Codes → Exception Classes
- [ ] `LFP-9001 DISPUTE_WINDOW_EXPIRED` → `DisputeWindowExpiredException` (HTTP 422) — >90 days since txn
- [ ] `LFP-9002 DISPUTE_TYPE_INVALID` → `DisputeTypeInvalidException` (HTTP 400)
- [ ] `LFP-9003 DISPUTE_ALREADY_EXISTS` → `DisputeAlreadyExistsException` (HTTP 409) — open dispute for same txnId
- [ ] `LFP-9004 DISPUTE_NOT_AUTHORIZED` → `DisputeNotAuthorizedException` (HTTP 403) — PSP not party to this dispute

### Integration Tests
- [ ] `DisputeRaiseIntegrationTest` — all 6 types; day 89 OK; day 91 → LFP-9001; SLA computed correctly per type
- [ ] `DisputeResolutionIntegrationTest` — `RESOLVED_REFUND` → auto-refund; `DISPUTE.STATUS_CHANGED` webhook to both PSPs
- [ ] `DisputeSlaEnforcementIntegrationTest` — mock time past deadline; scheduler fires; `auto_ruled=true`; resolved
- [ ] `DisputeAutoRefundIntegrationTest` — pool rebalanced between PSPs; `refund_transactions` row created

**P18 Exit Criteria:**
- [ ] All 6 dispute types raised, responded, and resolved
- [ ] SLA auto-ruling: `auto_ruled=true` when responding PSP misses deadline
- [ ] `RESOLVED_REFUND` triggers pool rebalance between PSPs
- [ ] Dispute records immutable; `retention_years=7` enforced by DB constraint

---

## Phase 20 — Performance & Scale [CERTIFICATION GATE — Priority 12]

**Spec refs:** LaoFP NFR-4.1–4.5, CERT-001 to CERT-112
**Depends on:** All P9–P19 complete | **Blocks:** LaoFP production go-live

### No New Domain Migrations — DB Tuning Only
- [ ] Verify indexes: `transfers(settlement_cycle_id)`, `vpa_registrations(vpa_type, vpa_value)`, `outbox_events(next_retry_at, status)`, `sanctions_lists(entity_name)` — run `EXPLAIN ANALYZE` on each critical query
- [ ] HikariCP `maximumPoolSize`: formula `Ncores × 2 + 1` per pod; target 50 pods × 17 = 850 total connections
- [ ] `psp_pools.available_balance` GENERATED column — no aggregation query at read time

### K8s / Infrastructure
- [ ] Update `k8s/hpa.yaml`: `maxReplicas: 8` → `maxReplicas: 50` for 10,000 TPS burst
- [ ] Add `k8s/pdb.yaml`: `apiVersion: policy/v1, kind: PodDisruptionBudget, spec: {minAvailable: 2}`
- [ ] JVM tuning in `k8s/deployment.yaml` env: `JAVA_OPTS=-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -Xms512m -Xmx900m`
- [ ] `spring.datasource.hikari.connection-timeout=3000` — env: `HIKARI_CONNECTION_TIMEOUT_MS`

### Config Properties
- [ ] `server.tomcat.threads.max=400` — env: `TOMCAT_MAX_THREADS`
- [ ] `spring.datasource.hikari.maximum-pool-size=50` — env: `HIKARI_MAX_POOL_SIZE`
- [ ] `spring.datasource.hikari.minimum-idle=10` — env: `HIKARI_MIN_IDLE`
- [ ] `switching.outbox.dispatch-batch-size=100` — env: `OUTBOX_DISPATCH_BATCH_SIZE` (tune up from 50)

### LaoFP Certification Tests (CERT suite)
- [ ] **CERT-001** Sustained load: 2,000 TPS × 300s; P95 <5s; error rate <0.05%
- [ ] **CERT-010** Burst load: 10,000 TPS × 60s; HPA scales to ≥20 pods; no requests dropped
- [ ] **CERT-020** VPA lookup: 500 concurrent; P95 <500ms
- [ ] **CERT-030** QR payment: 200 concurrent; SLA <10s P95
- [ ] **CERT-040** Settlement cycle: 500K transactions net positions computed in <60s
- [ ] **CERT-050** Failover (RTO): kill primary DB; service resumes in <30s
- [ ] **CERT-060** RPO = 0: DB commit before 200 OK; kill pod mid-transaction; replay from outbox; no data loss
- [ ] **CERT-070** Sanctions screening: 1,000 concurrent screen calls; P95 <2s
- [ ] **CERT-080** FPRE auto-reversal: 100 concurrent transfers at max retries; pool balance consistent
- [ ] **CERT-090** Webhook delivery: 10,000 events; all delivered within 30s; zero missing
- [ ] **CERT-100** Chaos: random pod kill every 30s for 5 min; P95 latency <5s throughout
- [ ] **CERT-112** Multi-zone HA: AZ-1 failure; traffic fails over to AZ-2 in <30s; no data loss

### LaoFP Certification Sign-Offs
- [ ] BoL technical architecture review completed (CERT-101 to CERT-112 docs submitted)
- [ ] Penetration test by BoL-approved security firm: no CRITICAL/HIGH findings unresolved
- [ ] HSM/KMS review: production secrets managed by HSM (MOD-19); no plaintext env vars in prod
- [ ] PCI-DSS SAQ-D: card-adjacent flows reviewed and signed off
- [ ] DR drill sign-off: RTO <30s, RPO=0 verified by ops team
- [ ] All CERT-001 to CERT-112 pass: 100% required for production license

**P20 Exit Criteria:**
- [ ] CERT-001: 2,000 TPS sustained; P95 <5s; error rate <0.05%
- [ ] CERT-010: 10,000 TPS burst; HPA scales to ≥20 pods; no drops
- [ ] CERT-050: automated failover RTO <30s
- [ ] CERT-060: RPO = 0 confirmed via outbox replay test
- [ ] CERT-112: multi-zone HA confirmed
- [ ] All 112 CERT tests pass; BoL production license issued

---

## Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2026-05-14 | 1.0 | Initial checklist — Phase 0 baseline freeze |
| 2026-05-14 | 1.1 | Phase 0 marked 90% done; Phase 1 updated to 95% — Testcontainers 46/46 PASS, CI pipeline, Dockerfile 3-stage + non-root, run.sh commands, TC-071 fix |
| 2026-05-14 | 1.2 | Phase 2 at 85% — Spring profiles dev/staging/prod, ProductionStartupValidator, IsoMessageCryptoService fix, docker-compose SPRING_PROFILES_ACTIVE=dev; 60/60 tests PASS |
| 2026-05-15 | 1.3 | P3 at 45% — V15 seed (participants+routing_rules), V16 performance indexes (6), V17 API key hardening migration; P4 at 30% — SHA-256 key hashing, key_prefix, expires_at, ApiKeyAuthFilter expiry check, ApiKeyService (create/list/disable/rotate), ApiKeyController ADMIN-only endpoints, SecurityConfig updated; XXE marked done for both parsers |
| 2026-05-15 | 1.4 | P2 at 95% — ProductionDemoKeyDisableService (@Profile("prod") disables demo keys by name+prefix on startup), .env in .gitignore confirmed, no hardcoded keys in src/; P3 at 65% — init-db-users.sh (switching_app DML-only, switching_flyway DDL), docker-compose.yml updated (mounts init script, app uses switching_app, Flyway uses switching_flyway), application.yml FLYWAY_URL/USERNAME/PASSWORD env vars; P4 at 40% — MaskingUtil.maskAccount(), XML body size 1MB |
| 2026-05-15 | 1.5 | Bug fixes: ParticipantType enum changed to DIRECT/INDIRECT (was BANK/SWITCHING/SERVICE_PROVIDER); V19 compensating migration fixes existing DB rows; V18 drops duplicate outbox index; GlobalExceptionHandler adds log.error to expose swallowed exceptions; 60/60 Maven tests PASS. P5 started (35%) — exponential backoff 30s/2min/10min, next_retry_at filter in outbox poller (findPendingBatch), nextRetryAt entity field. P7 started (25%) — graceful shutdown: volatile shuttingDown flag + @PreDestroy in OutboxDispatchWorker, server.shutdown:graceful + timeout-per-shutdown-phase:30s. P4 45% — MaskingUtil applied to creditorAccount in CreateTransferService audit payloads. Test fix: TC-103–107 ISO tests switched to BANK_B_KEY (BANK_B→BANK_A) to avoid BANK_A_KEY rate-limit exhaustion. |
| 2026-05-15 | 1.6 | P5 55% — audit trail for manual retry (OUTBOX_MANUAL_RETRY_REQUESTED) and mark-reviewed (OUTBOX_EVENT_MARKED_REVIEWED) confirmed already present in OutboxManualRetryService and OperationsOutboxMarkReviewedService. P6 30% — micrometer-registry-prometheus added (pom.xml, no version = Spring Boot BOM), /actuator/prometheus exposed (staging: main port 8080; prod: separate MANAGEMENT_PORT:9090 to protect public API), logstash-logback-encoder 8.0 added, logback-spring.xml created (text format for default/dev/test; JSON LogstashEncoder + ShortenedThrowableConverter rootCauseFirst for staging/prod). 60/60 tests PASS. |
| 2026-05-15 | 1.7 | P4 50% — MaskingUtil.maskAccount() applied to creditorAccount in CreateInquiryService + InquiryLookupService + IsoInquiryInboundService; debtorAccount + creditorAccount in TransferInquiryService. P5 65% — AuditActorUtil.currentActor() reads SecurityContextHolder (fallback "SYSTEM" for workers); replaced hardcoded "API" in OutboxManualRetryService + OperationsOutboxMarkReviewedService. P7 55% — 6 K8s manifests created: k8s/namespace.yaml, k8s/configmap.yaml, k8s/secret.yaml, k8s/deployment.yaml (Flyway initContainer, RollingUpdate maxUnavailable:0/maxSurge:1, probes on port 9090, startupProbe 2min max), k8s/service.yaml (ClusterIP 80→8080 + 9090), k8s/hpa.yaml (CPU 70%/Memory 80%, 2–8 pods, scaleDown stabilization 300s). Container hardening items (non-root user, multi-stage build) marked [x] from Phase 1. |
| 2026-05-15 | 1.8 | LaoFP alignment — doc renamed to LaoFP Switching API; Quick Status table split into Foundation (P0–P8) and LaoFP Expansion (P9–P20); 12 new LaoFP-specific phase checklists added (P9: OAuth/mTLS/HMAC; P10: FPRE full compliance; P11: VPA lookup; P12: webhooks; P13: pool/liquidity; P14: DNS/RTGS settlement; P15: QR; P16: bill payment; P17: cross-border; P18: dispute/refund; P19: AML/CFT/risk; P20: 2K→10K TPS/CERT suite). LaoFP compliance score: ~15–18% current (foundation solid, 82% new modules). |
| 2026-05-15 | 1.9 | P9–P20 upgraded to code-level detail — execution priority table added (P9→P10→P19→P12→P13→P14→P11→P15→P16→P17→P18→P20 ordered by criticality). Each phase now includes: DB migrations V20–V50 with exact DDL, new Java classes with full package paths + method signatures, API endpoints with req/resp shapes, config properties with env var names, LFP-xxxx error codes mapped to exception classes, integration test class names, and specific exit criteria. |
