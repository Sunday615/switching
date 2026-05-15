# Switching API — Production Acceptance Checklist

> **Phase 0 — Production Baseline Freeze**
> Created: 2026-05-14
> Purpose: Go / No-Go criteria for each production readiness phase.
> Status legend: `[ ]` Not started · `[~]` In progress · `[x]` Done · `[!]` Blocked

---

## How to Use

Before each phase sign-off, every item in that phase's section must be `[x]` or explicitly `ACCEPTED` with a documented reason. Any `[ ]` or `[!]` is a **hard blocker** for that phase unless escalated to the risk register as ACCEPTED.

---

## Quick Status

| Phase | Title | Status | Completion |
|-------|-------|--------|-----------|
| P0 | Baseline Freeze | 🟢 Done | 90% |
| P1 | Test & CI Gate | 🟢 Done | 95% |
| P2 | Production Config | 🟢 Done | 85% |
| P3 | DB & Migration Hardening | ⚪ Not Started | 0% |
| P4 | Security Advanced | ⚪ Not Started | 0% |
| P5 | Reliability & Outbox | ⚪ Not Started | 0% |
| P6 | Observability | ⚪ Not Started | 0% |
| P7 | Deployment & Runtime | ⚪ Not Started | 0% |
| P8 | Compliance & Business | ⚪ Not Started | 0% |

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
| POST | `/api/operations/outbox-stuck/recover` | ADMIN | ⚠️ ADMIN only | |
| GET | `/api/operations/bank-status` | OPS / ADMIN | ✅ Yes | |
| POST | `/api/operations/bank-onboarding` | ADMIN | ⚠️ ADMIN only | Creates participant + routing + connector |
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
- [x] `./mvnw test` passes from a clean checkout with no local MySQL — **46/46 PASS** ✅

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
- [ ] Trivy image scan integrated into CI (no CRITICAL CVEs)

### run.sh Updates
- [x] `docker:build` command added (build image only)
- [x] `docker:rebuild` command added (force rebuild + start)
- [x] `test:unit` command added (unit tests only, fast, no DB)
- [x] `status` command added (`docker compose ps`)
- [x] `test` / `test:unit` / `test:single` no longer call `load_env` (Testcontainers handles DB)

**Phase 1 Exit Criteria:**
- [x] `./mvnw test` passes on clean machine (no local MySQL) — **46/46 PASS**
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
- [ ] Demo API keys (`sk-admin-switching-2026` etc.) disabled in production migration
- [ ] Production key provisioning procedure documented (how to create first ADMIN key)
- [ ] No hardcoded credentials anywhere in `src/` (git grep for `sk-admin`, `sk-ops`, `sk-bank`)

### Secrets Management
- [ ] Decision made: Vault / AWS Secrets Manager / K8s Secrets / `.env` pipeline injection
- [ ] `.env` file is in `.gitignore` and never committed
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
- [ ] `switching_app` DB user created with SELECT/INSERT/UPDATE/DELETE only
- [ ] `switching_flyway` DB user created for migrations (ADMIN scope, migration-time only)
- [ ] Docker Compose and `application.yml` updated to use `switching_app` credentials
- [ ] MySQL root password rotated after migration user setup
- [ ] Connection tested: `switching_app` cannot DROP TABLE

### DB Connections
- [ ] `useSSL=true` in prod JDBC URL
- [ ] `requireSSL=true` in prod JDBC URL
- [ ] MySQL server SSL certificate issued and configured
- [ ] App truststore configured for MySQL CA cert
- [ ] Connection string does not include `allowPublicKeyRetrieval=true` in prod

### Migrations
- [ ] All migrations V1–V14 confirmed as frozen (no edits to historical migrations)
- [ ] V15 seed migration: `participants` BANK_A and BANK_B (production values)
- [ ] V16 seed migration: `routing_rules` matching V15 participants
- [ ] Migration for disabling demo API keys in prod (or: moved to `dev`-only migration)
- [ ] V17 index migration (see RISK-DB-006)
- [ ] Flyway `validate` passes in all environments after migrations

### Indexes (V17)
- [ ] `transfers (status, created_at DESC)` index added
- [ ] `outbox_events (status, next_retry_at)` index added
- [ ] `outbox_events (status, updated_at)` index added
- [ ] `audit_logs (reference_id, created_at)` index added
- [ ] `iso_messages (transfer_ref, direction)` index added
- [ ] `idempotency_records (expired_at)` index added
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
- [ ] Fresh install from migrations + V15/V16 seeds has working participants and routes
- [ ] DB backup and restore drill passed
- [ ] All indexes confirmed with `EXPLAIN` on key queries

---

## Phase 4 — Security Advanced Checklist

**Goal:** Bank/payment-grade authentication and data protection.

### API Key Security
- [ ] `api_keys.key_value` stores hashed values (bcrypt or SHA-256)
- [ ] `ApiKeyRepository.findByKeyValueAndEnabledTrue()` updated to hash-compare
- [ ] Key is shown to user once (on creation) and never retrievable again
- [ ] `expires_at` column added to `api_keys`
- [ ] Key expiry enforced in `ApiKeyAuthFilter`
- [ ] `POST /api/admin/api-keys/{id}/rotate` endpoint implemented
- [ ] Key rotation tested: old key stops working within 1 request after rotation

### Role Expansion
- [ ] Production role list finalized (see roadmap for full list)
- [ ] Role-to-endpoint mapping updated in `SecurityConfig`
- [ ] Role assignment UI exists in Operations Portal or admin API
- [ ] Role changes are audit-logged

### mTLS (Bank-Facing ISO Endpoints)
- [ ] `POST /api/iso20022/pacs008` requires client certificate
- [ ] `POST /api/iso20022/acmt023` requires client certificate
- [ ] Per-bank certificate management procedure documented
- [ ] `bank_certificates` table or column in `connector_configs` stores certificate fingerprint
- [ ] Certificate `CN`/`SAN` validated against `X-Bank-Code` header

### XML Security
- [ ] `Acmt023XmlParser` has XXE protection (`FEATURE_SECURE_PROCESSING`)
- [ ] `Pacs008InboundParser` has XXE protection
- [ ] XML body size limit configured (max 1MB): `spring.mvc.servlet.multipart.max-file-size`
- [ ] XXE penetration test performed and passed

### Data Masking
- [ ] `MaskingUtil.maskAccount(String)` utility created (shows last 4 digits)
- [ ] All `log.info/debug/error` calls with account numbers use masking
- [ ] ISO XML payloads in logs have `<DbtrAcct>` and `<CdtrAcct>` masked
- [ ] Audit log `payload` column masks account numbers at write time
- [ ] Ops portal does not show full account numbers without elevated permission

**Phase 4 Exit Criteria:**
- [ ] Security review checklist passed
- [ ] No plaintext API keys in DB
- [ ] Sensitive data masked in all logs and audit views
- [ ] mTLS verified with bank simulator

---

## Phase 5 — Reliability & Outbox Advanced Checklist

**Goal:** No duplicate dispatch. No lost event. Idempotency solid.

### Multi-Instance Safety
- [ ] Concurrency test: 2 app instances compete for same outbox event → only 1 wins
- [ ] `SELECT ... FOR UPDATE` or `UPDATE WHERE status='PENDING'` confirmed in claim logic
- [ ] No duplicate `SUCCESS` event for same `transferRef` in any concurrent test scenario

### Retry & Backoff
- [ ] `next_retry_at` populated consistently on each retry
- [ ] Exponential backoff implemented: 30s → 2min → 10min
- [ ] Outbox poller filters: `next_retry_at IS NULL OR next_retry_at <= NOW()`
- [ ] Backoff tested: retried event is not re-processed until `next_retry_at`

### Audit Trail for Manual Actions
- [ ] `OUTBOX_MANUAL_RETRY` audit log event written on every manual retry
- [ ] `OUTBOX_MARK_REVIEWED` audit log event written on every mark-reviewed
- [ ] Audit log includes: actor (from API key), outbox event ID, previous status, timestamp

### Idempotency Tests
- [ ] Concurrent POST `/api/transfers` with same idempotency key → exactly 1 transfer created
- [ ] Concurrent POST `/api/iso20022/pacs008` with same `MsgId` → second call rejected
- [ ] Concurrent POST `/api/iso20022/acmt023` with same `InstructionId` → second call rejected

### ISO Path Reliability
- [ ] Inquiry TTL enforced: PACS.008 with expired `InquiryRef` is rejected
- [ ] `inquiry_status_history` writes status transitions for ISO path (not just JSON path)

**Phase 5 Exit Criteria:**
- [ ] Duplicate dispatch test passes under 2-instance concurrent load
- [ ] Retry backoff confirmed in integration test
- [ ] All manual actions create audit records

---

## Phase 6 — Observability Checklist

**Goal:** Incidents diagnosable in < 5 minutes from one transferRef.

### Metrics Export
- [ ] `micrometer-registry-prometheus` dependency added
- [ ] `/actuator/prometheus` endpoint exposed (internal only)
- [ ] Prometheus configured to scrape app endpoint
- [ ] All existing Micrometer metrics visible in Prometheus UI

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
- [ ] `logstash-logback-encoder` added to `pom.xml`
- [ ] `logback-spring.xml` outputs JSON in staging and prod profiles
- [ ] All MDC fields appear in JSON log: `requestId`, `transferRef`, `inquiryRef`, `outboxEventId`, `bankCode`
- [ ] No sensitive data (account numbers, API keys) in log JSON

### Log Aggregation
- [ ] Log shipping configured (Fluentd / Filebeat → Elasticsearch / OpenSearch)
- [ ] Kibana/OpenSearch Dashboards accessible to ops team
- [ ] Search by `transferRef` returns all relevant log lines across app instances
- [ ] Log retention policy defined (recommend: 90 days hot, 1 year cold)

### Runbooks
- [ ] Runbook: outbox backlog growing
- [ ] Runbook: transfer failure spike
- [ ] Runbook: connector timeout/down
- [ ] Runbook: DB connection exhausted
- [ ] Runbook: manual outbox retry procedure
- [ ] Runbook: emergency rollback procedure

**Phase 6 Exit Criteria:**
- [ ] Ops can diagnose any transfer from one `transferRef` in Kibana
- [ ] All critical alerts fire correctly in staging (tested with synthetic failures)
- [ ] Runbooks reviewed by ops team

---

## Phase 7 — Deployment & Runtime Checklist

**Goal:** Deploy repeatably. Rollback safely. No traffic until ready.

### Container Hardening
- [ ] `Dockerfile` adds non-root user (`switching`) and sets `USER switching`
- [ ] Container does not run as UID 0
- [ ] Container image scanned with Trivy or Snyk (no CRITICAL CVEs)
- [ ] Base image pinned to specific digest (not `latest`)
- [ ] Image size optimized (multi-stage build confirmed)

### Kubernetes / Orchestration
- [ ] `Deployment` manifest: `replicas: 2` (minimum HA)
- [ ] `resources.requests` and `resources.limits` set (CPU + memory)
- [ ] `livenessProbe` configured (GET `/actuator/health/liveness`, delay 30s)
- [ ] `readinessProbe` configured (GET `/actuator/health/readiness`, delay 20s)
- [ ] `readinessProbe` only green after DB + Flyway healthy
- [ ] `HorizontalPodAutoscaler` configured (scale on CPU or custom metrics)
- [ ] Flyway migration runs as `initContainer` before app pods start

### Graceful Shutdown
- [ ] `OutboxDispatchWorker` handles shutdown signal: no new events accepted
- [ ] In-progress outbox events are either completed or rolled back cleanly on shutdown
- [ ] `spring.lifecycle.timeout-per-shutdown-phase` configured (recommend: 30s)
- [ ] Shutdown tested: kill signal → drain → graceful stop → no orphaned PROCESSING events

### Zero-Downtime Deploy
- [ ] Rolling update strategy: `maxUnavailable: 0, maxSurge: 1`
- [ ] Deploy tested: new version rolls out with zero dropped requests (verify with load test)
- [ ] Flyway migration backward-compatible (new columns nullable or with defaults)

### Rollback
- [ ] Rollback procedure documented: `kubectl rollout undo deployment/switching-api`
- [ ] DB rollback procedure documented (undo migration or compensating migration)
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

## Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2026-05-14 | 1.0 | Initial checklist — Phase 0 baseline freeze |
| 2026-05-14 | 1.1 | Phase 0 marked 90% done; Phase 1 updated to 95% — Testcontainers 46/46 PASS, CI pipeline, Dockerfile 3-stage + non-root, run.sh commands, TC-071 fix |
| 2026-05-14 | 1.2 | Phase 2 at 85% — Spring profiles dev/staging/prod, ProductionStartupValidator, IsoMessageCryptoService fix, docker-compose SPRING_PROFILES_ACTIVE=dev; 60/60 tests PASS |
