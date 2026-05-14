# Switching API — Risk Register

> **Phase 0 — Production Baseline Freeze**
> Created: 2026-05-14
> Owner: Platform / Backend Team
> Review cycle: Every sprint (2 weeks)

---

## How to Read This Document

| Field | Meaning |
|-------|---------|
| **ID** | Unique risk identifier (format: `RISK-{DOMAIN}-{NNN}`) |
| **Severity** | 🔴 Critical · 🟠 High · 🟡 Medium · 🟢 Low |
| **Likelihood** | Probability of occurring: High / Medium / Low |
| **Impact** | Business consequence if it materializes |
| **Status** | `OPEN` · `MITIGATED` · `ACCEPTED` · `RESOLVED` |
| **Phase** | Which roadmap phase addresses this risk |

---

## Summary Table

| ID | Domain | Title | Severity | Likelihood | Status | Phase |
|----|--------|-------|----------|------------|--------|-------|
| RISK-SEC-001 | Security | API keys stored in plaintext | 🔴 Critical | High | OPEN | P4 |
| RISK-SEC-002 | Security | Demo API keys in production path | 🔴 Critical | High | OPEN | P2 |
| RISK-SEC-003 | Security | AES/GCM dev fallback key possible | 🟠 High | Medium | MITIGATED | P1 |
| RISK-SEC-004 | Security | No mTLS on bank-facing ISO endpoints | 🟠 High | Medium | OPEN | P4 |
| RISK-SEC-005 | Security | XML XXE attack surface | 🟠 High | Low | OPEN | P4 |
| RISK-SEC-006 | Security | Account numbers in plaintext logs | 🟠 High | High | OPEN | P4 |
| RISK-SEC-007 | Security | No IP allowlist per bank | 🟡 Medium | Medium | OPEN | P4 |
| RISK-SEC-008 | Security | No API key expiry or rotation | 🟡 Medium | Low | OPEN | P4 |
| RISK-DB-001 | Database | App runs as MySQL root user | 🔴 Critical | High | OPEN | P3 |
| RISK-DB-002 | Database | DB connections use useSSL=false | 🔴 Critical | High | OPEN | P3 |
| RISK-DB-003 | Database | No automated DB backup procedure | 🔴 Critical | Medium | OPEN | P3 |
| RISK-DB-004 | Database | V8 migration drops routing_rules | 🟠 High | High | OPEN | P3 |
| RISK-DB-005 | Database | No participants seeded in migrations | 🟠 High | High | OPEN | P3 |
| RISK-DB-006 | Database | No DB index for outbox polling | 🟡 Medium | Medium | OPEN | P3 |
| RISK-DB-007 | Database | No point-in-time recovery configured | 🟠 High | Low | OPEN | P3 |
| RISK-TEST-001 | Test/CI | Tests fail on clean checkout | 🔴 Critical | High | OPEN | P1 |
| RISK-TEST-002 | Test/CI | Docker image builds skipping tests | 🔴 Critical | High | OPEN | P1 |
| RISK-TEST-003 | Test/CI | No CI pipeline exists | 🔴 Critical | High | OPEN | P1 |
| RISK-TEST-004 | Test/CI | Integration tests use MySQL root | 🟠 High | High | OPEN | P1 |
| RISK-TEST-005 | Test/CI | Test data accumulates across runs | 🟡 Medium | High | OPEN | P1 |
| RISK-OUT-001 | Outbox | Multi-instance duplicate dispatch not tested | 🟠 High | Medium | OPEN | P5 |
| RISK-OUT-002 | Outbox | No exponential backoff on retry | 🟡 Medium | High | OPEN | P5 |
| RISK-OUT-003 | Outbox | Stuck PROCESSING has 2-min blind spot | 🟡 Medium | Medium | OPEN | P5 |
| RISK-OUT-004 | Outbox | Manual retry has no audit trail | 🟡 Medium | High | OPEN | P5 |
| RISK-OUT-005 | Outbox | next_retry_at column not consistently used | 🟡 Medium | Medium | OPEN | P5 |
| RISK-OBS-001 | Observability | No Prometheus/Grafana integration | 🟠 High | High | OPEN | P6 |
| RISK-OBS-002 | Observability | No alerts configured | 🔴 Critical | High | OPEN | P6 |
| RISK-OBS-003 | Observability | Logs are not structured JSON | 🟠 High | High | OPEN | P6 |
| RISK-OBS-004 | Observability | No log aggregation (ELK/OpenSearch) | 🟠 High | High | OPEN | P6 |
| RISK-OBS-005 | Observability | Runbooks do not exist | 🟠 High | High | OPEN | P6 |
| RISK-DEP-001 | Deployment | No K8s / container orchestration | 🟠 High | High | OPEN | P7 |
| RISK-DEP-002 | Deployment | App runs as root inside container | 🟠 High | Medium | OPEN | P7 |
| RISK-DEP-003 | Deployment | No liveness/readiness probes | 🟠 High | High | OPEN | P7 |
| RISK-DEP-004 | Deployment | No graceful shutdown for outbox | 🟡 Medium | Medium | OPEN | P7 |
| RISK-DEP-005 | Deployment | No rollback procedure documented | 🟠 High | Medium | OPEN | P7 |
| RISK-BIZ-001 | Business | No reconciliation/settlement job | 🔴 Critical | High | OPEN | P8 |
| RISK-BIZ-002 | Business | No dispute/reversal mechanism | 🟠 High | Medium | OPEN | P8 |
| RISK-BIZ-003 | Business | No data retention/purge policy | 🟠 High | Low | OPEN | P8 |
| RISK-BIZ-004 | Business | No DR drill or RPO/RTO defined | 🟠 High | Medium | OPEN | P8 |
| RISK-BIZ-005 | Business | No load/soak test results | 🟠 High | High | OPEN | P8 |
| RISK-CFG-001 | Config | Prod can start with empty crypto key (dev fallback) | 🔴 Critical | Low | MITIGATED | P2 |
| RISK-CFG-002 | Config | No secret manager integration | 🟠 High | High | OPEN | P2 |
| RISK-CFG-003 | Config | No staging profile that mirrors prod | 🟡 Medium | Medium | OPEN | P2 |
| RISK-ISO-001 | ISO 20022 | No ISO XML size limit | 🟠 High | Low | OPEN | P4 |
| RISK-ISO-002 | ISO 20022 | Inquiry TTL not enforced at transfer | 🟡 Medium | Medium | OPEN | P5 |
| RISK-ISO-003 | ISO 20022 | inquiry_status_history not used for ISO path | 🟢 Low | High | OPEN | P5 |

---

## Detailed Risk Entries

---

### SECURITY RISKS

---

#### RISK-SEC-001 — API Keys Stored in Plaintext
| | |
|---|---|
| **Severity** | 🔴 Critical |
| **Likelihood** | High |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 4 |

**Description:**
`api_keys.key_value` stores raw API key strings (e.g. `sk-admin-switching-2026`) directly in the database. Any database read access — including by a compromised DB user, a SQL injection, or a backup leak — exposes all active keys immediately.

**Location:**
- `src/main/resources/db/migration/V14__create_api_keys.sql`
- `src/main/java/com/example/switching/security/repository/ApiKeyRepository.java` → `findByKeyValueAndEnabledTrue()`

**Impact:**
Full API compromise. Attacker can impersonate any role (ADMIN, BANK, OPS) without limit.

**Mitigation Plan:**
1. Store keys as `bcrypt` or `SHA-256(key)` hash only.
2. The raw key is shown to the user exactly once (on creation) and never stored.
3. Authentication: hash the incoming `X-API-Key` header, compare to stored hash.
4. Add `created_at`, `expires_at`, `last_rotated_at` columns.
5. Add `POST /api/admin/api-keys/{id}/rotate` endpoint (ADMIN only).

---

#### RISK-SEC-002 — Demo API Keys Remain in Production Path
| | |
|---|---|
| **Severity** | 🔴 Critical |
| **Likelihood** | High |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 2 |

**Description:**
V14 migration seeds four demo keys into the `api_keys` table with well-known values (`sk-admin-switching-2026`, `sk-ops-switching-2026`, etc.). These run in every environment including production unless explicitly removed or disabled.

**Location:**
- `src/main/resources/db/migration/V14__create_api_keys.sql`

**Impact:**
Anyone who reads this repository (public or leaked) can authenticate as ADMIN or BANK on production systems using the seeded keys.

**Mitigation Plan:**
1. Add a `prod` Flyway migration (`V15__disable_demo_keys.sql`) that sets `enabled = false` for all seeded demo keys.
2. Or: move demo key seeding to a `dev`-only migration (not run in prod profile).
3. Require initial admin key to be injected via environment variable on first boot.
4. Document key provisioning in the production runbook.

---

#### RISK-SEC-003 — AES/GCM Dev Fallback Key
| | |
|---|---|
| **Severity** | 🟠 High |
| **Likelihood** | Low |
| **Status** | MITIGATED |
| **Roadmap Phase** | Phase 1 (done) |

**Description:**
`IsoMessageCryptoService.resolveKey()` throws `IllegalStateException` at startup if `MESSAGE_CRYPTO_KEY_BASE64` is blank and the active profile is not `test`. The test profile uses a fixed dev key to allow tests to run without the env var.

**Mitigation Applied:**
Startup guard already exists in `IsoMessageCryptoService`. Production cannot start without a key. Test profile uses a distinct fixed key. Risk is low as long as the guard is not removed.

**Residual risk:**
If someone adds `MESSAGE_CRYPTO_KEY_BASE64:` (empty default) back to `application.yml`, the guard is bypassed. Monitor this in code review.

---

#### RISK-SEC-004 — No mTLS on Bank-Facing ISO Endpoints
| | |
|---|---|
| **Severity** | 🟠 High |
| **Likelihood** | Medium |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 4 |

**Description:**
`POST /api/iso20022/pacs008` and `POST /api/iso20022/acmt023` accept connections over TLS (one-way) with only an `X-API-Key` + `X-Bank-Code` header for authentication. Mutual TLS (mTLS) — where the bank must present a client certificate — is not implemented.

**Impact:**
If an API key is leaked, a third party can submit ISO messages as any bank. For financial messaging, mTLS is a standard bank-to-bank trust requirement.

**Mitigation Plan:**
1. Configure Spring Boot `server.ssl.client-auth: need` for ISO endpoints.
2. Issue per-bank client certificates, pinned to `bank_code`.
3. Validate `CN` or `SAN` in the client certificate matches the `X-Bank-Code` header.
4. Store certificate fingerprints in `connector_configs` or a new `bank_certificates` table.

---

#### RISK-SEC-005 — XML XXE Attack Surface
| | |
|---|---|
| **Severity** | 🟠 High |
| **Likelihood** | Low |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 4 |

**Description:**
XML parsers (`Acmt023XmlParser`, `Pacs008InboundParser`) use `DocumentBuilderFactory`. If external entity processing is not explicitly disabled, a malicious bank could submit an XML payload that reads local files or makes internal HTTP requests (XXE / SSRF).

**Location:**
- `src/main/java/com/example/switching/iso/inquiry/Acmt023XmlParser.java`
- `src/main/java/com/example/switching/iso/inbound/Pacs008InboundParser.java`

**Impact:**
File disclosure (private keys, application.yml, /etc/passwd). Internal network scanning. Potential remote code execution in worst case.

**Mitigation Plan:**
```java
DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
dbf.setXIncludeAware(false);
dbf.setExpandEntityReferences(false);
```
Also add request body size limit (max 1MB) in Spring Boot config.

---

#### RISK-SEC-006 — Account Numbers in Plaintext Logs
| | |
|---|---|
| **Severity** | 🟠 High |
| **Likelihood** | High |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 4 |

**Description:**
Transfer creation, inquiry processing, and ISO message parsing log full account numbers, bank codes, amounts, and currency in plaintext via SLF4J. These appear in application logs, which may be shipped to ELK/OpenSearch where a broader audience has access.

**Impact:**
Violates PCI DSS / local financial data protection requirements. Account numbers are PII and must be masked in all logs.

**Mitigation Plan:**
1. Create a `MaskingUtil.maskAccount(String acct)` → shows last 4 digits: `****0001`.
2. Apply to all `log.info/debug/error` calls that contain account numbers, amounts, or XML payloads.
3. Ensure audit log `payload` column stores structured data with masking applied at write time.
4. Add a log scrubber / Logback `PatternLayout` filter as a backstop.

---

#### RISK-SEC-007 — No IP Allowlist per Bank
| | |
|---|---|
| **Severity** | 🟡 Medium |
| **Likelihood** | Medium |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 4 |

**Description:**
Any IP address can attempt to authenticate as BANK_A with a stolen API key. There is no IP-based restriction on which source IPs are allowed to call the ISO endpoints per bank participant.

**Mitigation Plan:**
Add optional `allowed_ip_ranges` column to `api_keys` or `participants`. Enforce in `ApiKeyAuthFilter` or a separate `IpAllowlistFilter` using `X-Forwarded-For` (validated from trusted proxy).

---

#### RISK-SEC-008 — No API Key Expiry or Rotation
| | |
|---|---|
| **Severity** | 🟡 Medium |
| **Likelihood** | Low |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 4 |

**Description:**
API keys have no `expires_at` field. Once created, a key is valid indefinitely unless manually disabled. This is contrary to security best practices for financial APIs.

**Mitigation Plan:**
1. Add `expires_at DATETIME` column to `api_keys`.
2. `ApiKeyAuthFilter` checks `expires_at` → returns 401 if expired.
3. Add `POST /api/admin/api-keys/{id}/rotate` → generates new key, deprecates old.
4. Alert ops 30 days before any key expires.

---

### DATABASE RISKS

---

#### RISK-DB-001 — Application Runs as MySQL Root
| | |
|---|---|
| **Severity** | 🔴 Critical |
| **Likelihood** | High |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 3 |

**Description:**
Both `docker-compose.yml` and `application.yml` default to connecting as the MySQL `root` user. Root can `DROP DATABASE`, `DROP TABLE`, `CREATE USER`, and perform any operation on any schema. A SQL injection, a misconfiguration, or a compromised application instance can destroy all data.

**Location:**
- `docker-compose.yml` → `SPRING_DATASOURCE_USERNAME=root`
- `application.yml` → `username: ${DB_USERNAME:root}`

**Impact:**
Total data loss. Schema destruction. Privilege escalation if MySQL instance is shared.

**Mitigation Plan:**
```sql
CREATE USER 'switching_app'@'%' IDENTIFIED BY '<strong-password>';
GRANT SELECT, INSERT, UPDATE, DELETE ON switching_db.* TO 'switching_app'@'%';
-- Flyway needs CREATE TABLE for migrations — use a separate migration user
CREATE USER 'switching_flyway'@'%' IDENTIFIED BY '<strong-password>';
GRANT ALL PRIVILEGES ON switching_db.* TO 'switching_flyway'@'%';
-- switching_flyway used only during migration, not during normal app operation
```

---

#### RISK-DB-002 — DB Connections Use useSSL=false
| | |
|---|---|
| **Severity** | 🔴 Critical |
| **Likelihood** | High |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 3 |

**Description:**
Default JDBC URL: `jdbc:mysql://localhost:3306/switching_db?useSSL=false&allowPublicKeyRetrieval=true`. All DB traffic (including transfer data, ISO messages, account numbers) is transmitted unencrypted.

**Impact:**
Man-in-the-middle attack on DB traffic. Data exposure on any shared network.

**Mitigation Plan:**
```
# Prod DB URL:
jdbc:mysql://${DB_HOST}:3306/switching_db
  ?useSSL=true
  &requireSSL=true
  &verifyServerCertificate=true
  &trustCertificateKeyStoreUrl=file:/certs/truststore.jks
  &trustCertificateKeyStorePassword=${DB_TRUSTSTORE_PASSWORD}
```
Enable SSL on MySQL server side and issue a server certificate.

---

#### RISK-DB-003 — No Automated DB Backup Procedure
| | |
|---|---|
| **Severity** | 🔴 Critical |
| **Likelihood** | Medium |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 3 |

**Description:**
No documented backup schedule, retention policy, or restore procedure exists. Any disk failure, accidental drop, or corruption will result in permanent data loss.

**Mitigation Plan:**
1. Daily logical backup: `mysqldump --single-transaction switching_db | gzip > backup-$(date +%Y%m%d).sql.gz`
2. Store backups in separate storage (S3, GCS, or off-site NFS).
3. Retention: 30 days rolling.
4. Enable MySQL binary logging for point-in-time recovery: `log_bin = ON`, `expire_logs_days = 7`.
5. Monthly restore drill: restore from backup to a test instance, verify data and Flyway checksum.

---

#### RISK-DB-004 — V8 Migration Drops routing_rules After V2 Seed
| | |
|---|---|
| **Severity** | 🟠 High |
| **Likelihood** | High |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 3 |

**Description:**
`V8__create_participants_and_routing_rules.sql` drops and recreates the `routing_rules` table. V2 had seeded `ROUTE_BANK_B_PRIMARY`. After V8 runs on a fresh install, the table is empty and the application cannot route any transfer until routes are manually created.

**Impact:**
Fresh production install has zero routing rules → all transfers fail immediately with `RTE-001 (422)`.

**Mitigation Plan:**
Add `V15__seed_production_routing.sql` (or `V16`) that inserts initial routing rules matching the production participant setup. This migration must be idempotent (`INSERT IGNORE` or `ON DUPLICATE KEY UPDATE`).

---

#### RISK-DB-005 — No Participants Seeded in Migrations
| | |
|---|---|
| **Severity** | 🟠 High |
| **Likelihood** | High |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 3 |

**Description:**
The `participants` table (added in V8) is never seeded by any migration. On a fresh install, `POST /api/inquiries` or `POST /api/iso20022/pacs008` will fail with `PRT-001 (404)` for any bank code.

**Impact:**
Production deploy requires manual onboarding before any transaction can be processed.

**Mitigation Plan:**
1. Document the bank onboarding procedure: `POST /api/operations/bank-onboarding` (already exists).
2. Optionally: add a V15 seed migration for the first production participant bank pair.
3. Add `POST /api/operations/bank-onboarding` to the go-live checklist as a required step.

---

#### RISK-DB-006 — Missing Indexes for Operational Queries
| | |
|---|---|
| **Severity** | 🟡 Medium |
| **Likelihood** | Medium |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 3 |

**Description:**
Several high-frequency queries lack appropriate indexes. As transfer volume grows (millions of rows), these will cause full table scans.

**Missing indexes:**
| Table | Query pattern | Recommended index |
|-------|--------------|-------------------|
| `transfers` | `WHERE status = ? ORDER BY created_at DESC` | `(status, created_at DESC)` |
| `outbox_events` | `WHERE status = 'PENDING' ORDER BY created_at` | `(status, next_retry_at)` |
| `outbox_events` | `WHERE status = 'PROCESSING' AND updated_at < ?` | `(status, updated_at)` |
| `audit_logs` | `WHERE reference_id = ?` | `(reference_id, created_at)` |
| `iso_messages` | `WHERE transfer_ref = ?` | `(transfer_ref, direction)` |
| `idempotency_records` | `WHERE expired_at < NOW()` (cleanup job) | `(expired_at)` |

---

#### RISK-DB-007 — No Point-in-Time Recovery
| | |
|---|---|
| **Severity** | 🟠 High |
| **Likelihood** | Low |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 3 |

**Description:**
MySQL binary logging is not confirmed enabled. Without binlogs, the maximum data loss window equals the time since the last full backup (potentially 24 hours for daily backups).

**Mitigation Plan:**
Enable in `my.cnf`: `log_bin = /var/log/mysql/mysql-bin.log` and `binlog_format = ROW`. Set `expire_logs_days = 7`. Test recovery using `mysqlbinlog` from a backup point.

---

### TEST & CI RISKS

---

#### RISK-TEST-001 — Integration Tests Fail on Clean Checkout
| | |
|---|---|
| **Severity** | 🔴 Critical |
| **Likelihood** | High |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 1 |

**Description:**
`./mvnw test` produces `Tests run: 60, Failures: 0, Errors: 46, Skipped: 0`. Root cause: integration tests require a locally running MySQL instance with root credentials that are not documented or portable. A new developer or CI runner on a clean machine cannot run tests without undocumented setup.

**Impact:**
- Changes cannot be safely verified before merge.
- Bugs introduced by refactoring are not caught by CI.
- Test suite provides false confidence (appears to exist, does not actually gate changes).

**Mitigation Plan:**
Replace MySQL root dependency with Testcontainers:
```java
@Testcontainers
@SpringBootTest
class FullTransferFlowIntegrationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("switching_clean")
        .withUsername("root")
        .withPassword("test");
    ...
}
```
`application-test.yml` reads `spring.datasource.url` from the Testcontainers-provided JDBC URL via `DynamicPropertySource`.

---

#### RISK-TEST-002 — Docker Image Builds with -DskipTests
| | |
|---|---|
| **Severity** | 🔴 Critical |
| **Likelihood** | High |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 1 |

**Description:**
`Dockerfile` line: `./mvnw clean package -DskipTests`. A broken build that fails all 60 tests will still produce a deployable Docker image. This image can reach production without any test validation.

**Location:** `Dockerfile` — build stage

**Impact:**
Production can receive a broken image. No safety net between code change and deployment.

**Mitigation Plan:**
1. Add a CI step that runs tests before the Docker build.
2. In `Dockerfile`, only skip tests for an explicit dev/fast build target.
3. Production image build in CI always requires passing tests first:
   ```yaml
   jobs:
     test:
       runs: ./mvnw test
     docker-build:
       needs: test
       runs: docker build ...
   ```

---

#### RISK-TEST-003 — No CI Pipeline Exists
| | |
|---|---|
| **Severity** | 🔴 Critical |
| **Likelihood** | High |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 1 |

**Description:**
There is no `.github/workflows/`, `.gitlab-ci.yml`, or any other CI configuration in the repository. Every code change is merged and deployed without automated validation.

**Impact:**
Every merge is a manual trust exercise. Regression bugs, security issues, and broken builds can reach production undetected.

**Mitigation Plan:**
Create CI pipeline (GitHub Actions example):
```
on: [push, pull_request]
jobs:
  1. compile          → mvnw compile -q
  2. unit-test        → mvnw test -Dgroups=unit
  3. integration-test → mvnw test -Dgroups=integration (Testcontainers)
  4. package          → mvnw package -DskipTests (only if #3 passes)
  5. docker-build     → docker build (only if #4 passes)
  6. docker-push      → push to registry (only on main branch)
  7. smoke-test       → scripts/run_tests.sh against staging (optional)
```

---

### OUTBOX RISKS

---

#### RISK-OUT-001 — Multi-Instance Duplicate Dispatch Not Tested
| | |
|---|---|
| **Severity** | 🟠 High |
| **Likelihood** | Medium |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 5 |

**Description:**
`OutboxProcessorService` claims events with `PENDING → PROCESSING` in a `PROPAGATION_REQUIRES_NEW` transaction, relying on DB-level row locking to prevent two instances from processing the same event. This pattern is correct in theory, but it has never been tested under concurrent multi-instance load.

**Impact:**
Duplicate PACS.008 dispatched to the same destination bank — double payment. Financial loss. This is the highest business impact scenario.

**Mitigation Plan:**
1. Write a concurrency test: spin up 2 application contexts, both pick up the same outbox event, assert only one `SUCCESS` result.
2. Verify `SELECT ... FOR UPDATE` is used or `UPDATE WHERE status = 'PENDING'` with optimistic locking.
3. Add a `version` column (optimistic lock) as a secondary guard.
4. Add duplicate `transferRef` detection in `MockBankConnector` (for testing).

---

#### RISK-OUT-002 — No Exponential Backoff on Retry
| | |
|---|---|
| **Severity** | 🟡 Medium |
| **Likelihood** | High |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 5 |

**Description:**
Retried outbox events are immediately re-queued as `PENDING` with no delay. If a downstream connector is down, all retry attempts will happen in rapid succession, wasting resources and potentially hitting the downstream bank with a burst of failed requests.

**Current behavior:** retry immediately, 3 attempts, all within seconds.

**Mitigation Plan:**
Use `next_retry_at` column (already in schema) with exponential backoff:
```
Attempt 1: immediate
Attempt 2: now + 30 seconds
Attempt 3: now + 2 minutes
Attempt 4: now + 10 minutes
Attempt 5+: FAILED
```
Outbox poller filters: `WHERE status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= NOW())`.

---

#### RISK-OUT-004 — Manual Retry Has No Audit Trail
| | |
|---|---|
| **Severity** | 🟡 Medium |
| **Likelihood** | High |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 5 |

**Description:**
`POST /api/outbox-events/{id}/retry` allows OPS/ADMIN to manually re-queue a failed outbox event. This action is not recorded in `audit_logs`. There is no record of who triggered the retry, when, or what the event state was at the time.

**Impact:**
Audit gap. For financial operations, every manual intervention must be traceable to an actor.

**Mitigation Plan:**
Add `AuditLogService.log("OUTBOX_MANUAL_RETRY", "OUTBOX_EVENT", outboxEventId, actor, ...)` in `OutboxManualRetryService`.

---

### OBSERVABILITY RISKS

---

#### RISK-OBS-001 — No Prometheus/Grafana Integration
| | |
|---|---|
| **Severity** | 🟠 High |
| **Likelihood** | High |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 6 |

**Description:**
Micrometer metrics are registered and exported via `/actuator/metrics`, but there is no Prometheus scrape config or Grafana dashboard. The data exists but no one can see it in real time.

**Mitigation Plan:**
1. Add `micrometer-registry-prometheus` dependency.
2. Expose `/actuator/prometheus` endpoint.
3. Configure Prometheus to scrape it.
4. Import Grafana dashboards (Spring Boot JVM dashboard + custom payment dashboard).

---

#### RISK-OBS-002 — No Alerts Configured
| | |
|---|---|
| **Severity** | 🔴 Critical |
| **Likelihood** | High |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 6 |

**Description:**
There are no alerting rules. If the outbox builds up 1,000 stuck events, if all transfers start failing, or if the connector goes down, nobody is notified. Issues will be discovered by users, not by the operations team.

**Critical alerts needed (minimum):**
| Alert | Condition | Channel |
|-------|-----------|---------|
| Outbox backlog | `outbox_pending > 100` for 5 min | PagerDuty / Slack |
| Stuck events | `outbox_stuck_processing > 5` for 2 min | PagerDuty |
| Transfer failures spike | failed rate > 10% in 1 min | Slack |
| Connector down | `NET-001/NET-002` errors > 5 in 1 min | PagerDuty |
| API 5xx spike | 500 response rate > 1% | Slack |
| DB connection exhausted | Pool usage > 90% | PagerDuty |

---

#### RISK-OBS-003 — Logs Are Not Structured JSON
| | |
|---|---|
| **Severity** | 🟠 High |
| **Likelihood** | High |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 6 |

**Description:**
Current `logback-spring.xml` (or default Logback config) outputs plain text logs. When logs are shipped to ELK/OpenSearch, they cannot be reliably parsed or filtered by `transferRef`, `requestId`, etc., without a Grok parser — which is fragile and error-prone.

**Mitigation Plan:**
Add `logstash-logback-encoder` dependency and configure JSON output:
```xml
<appender name="JSON_STDOUT" class="ch.qos.logback.core.ConsoleAppender">
  <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
</appender>
```
MDC fields (`requestId`, `transferRef`, `inquiryRef`, `outboxEventId`) will be automatically included in every log line as JSON fields.

---

### DEPLOYMENT RISKS

---

#### RISK-DEP-002 — App Runs as Root Inside Container
| | |
|---|---|
| **Severity** | 🟠 High |
| **Likelihood** | Medium |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 7 |

**Description:**
`Dockerfile` does not set a non-root user. The JVM process runs as UID 0 inside the container. If the container is compromised, the attacker has root access to the container filesystem and potentially the host (with container escape).

**Mitigation Plan:**
```dockerfile
RUN addgroup --system switching && adduser --system --ingroup switching switching
USER switching
```

---

#### RISK-DEP-003 — No Liveness/Readiness Probes
| | |
|---|---|
| **Severity** | 🟠 High |
| **Likelihood** | High |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 7 |

**Description:**
`/actuator/health` exists but is not configured as a Kubernetes probe. During a rolling deployment, new pods may receive traffic before Flyway migrations complete and the DB connection pool is established — causing 500 errors.

**Mitigation Plan:**
```yaml
livenessProbe:
  httpGet: { path: /actuator/health/liveness, port: 8080 }
  initialDelaySeconds: 30
  periodSeconds: 10
readinessProbe:
  httpGet: { path: /actuator/health/readiness, port: 8080 }
  initialDelaySeconds: 20
  periodSeconds: 5
```
Configure `management.endpoint.health.group.readiness.include: db, flyway` so readiness only turns green after DB and migrations are healthy.

---

### BUSINESS RISKS

---

#### RISK-BIZ-001 — No Reconciliation or Settlement Job
| | |
|---|---|
| **Severity** | 🔴 Critical |
| **Likelihood** | High |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 8 |

**Description:**
There is no process to compare the switching system's transfer records against the destination bank's settlement records. Discrepancies (transfers marked SUCCESS in switching but not settled at the bank, or vice versa) will accumulate silently.

**Impact:**
Financial losses. Regulatory violations. Inability to detect systemic failures or fraud patterns.

**Mitigation Plan:**
1. Build a daily reconciliation job that compares `transfers WHERE status='SUCCESS' AND DATE(created_at) = yesterday` against bank settlement files.
2. Flag mismatches as `RECONCILIATION_MISMATCH` entries.
3. Route mismatches to Finance/Settlement Portal for human review.

---

#### RISK-BIZ-004 — No DR Drill or RPO/RTO Defined
| | |
|---|---|
| **Severity** | 🟠 High |
| **Likelihood** | Medium |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 8 |

**Description:**
There are no defined Recovery Point Objective (RPO) or Recovery Time Objective (RTO) targets. No DR drill has been performed. If a datacenter failure occurs, recovery time and data loss are unknown.

**Mitigation Plan:**
1. Define RPO: ≤ 1 hour (max data loss acceptable).
2. Define RTO: ≤ 4 hours (max time to restore service).
3. Design backup strategy to meet RPO.
4. Document recovery runbook.
5. Run DR drill quarterly (restore from backup, verify, measure time).

---

#### RISK-BIZ-005 — No Load or Soak Test Results
| | |
|---|---|
| **Severity** | 🟠 High |
| **Likelihood** | High |
| **Status** | OPEN |
| **Roadmap Phase** | Phase 8 |

**Description:**
The application has never been tested under production-like load. There are no benchmarks for:
- Maximum sustainable transfers per minute
- DB connection pool behavior under load
- Outbox worker throughput
- Memory behavior over 24+ hours (soak test)

**Target performance (to be validated):**
| Scenario | Target |
|----------|--------|
| Sustained load | 500 transfers/min |
| Peak burst | 1,000 transfers/min for 1 min |
| p99 API latency | < 500ms |
| Outbox dispatch | < 200ms p95 |
| Soak test | 24 hours at 100 tps, no memory leak |

---

## Risk Heatmap

```
         LIKELIHOOD
         Low        Medium       High
       ┌──────────┬────────────┬────────────────────────────┐
CRIT   │SEC-003✅  │DB-003      │SEC-001 SEC-002             │
🔴     │CFG-001✅  │BIZ-004     │DB-001  DB-002              │
       │           │            │TEST-001 TEST-002 TEST-003  │
       │           │            │OBS-002                     │
       │           │            │BIZ-001                     │
├──────┼──────────┼────────────┼────────────────────────────┤
HIGH   │SEC-004    │DB-007      │DB-004  DB-005              │
🟠     │SEC-005    │OUT-001     │TEST-004                    │
       │           │DEP-002     │OBS-001 OBS-003 OBS-004     │
       │           │DEP-005     │OBS-005                     │
       │           │BIZ-002     │DEP-001 DEP-003             │
       │           │            │BIZ-005                     │
├──────┼──────────┼────────────┼────────────────────────────┤
MED    │SEC-008    │SEC-007     │SEC-006                     │
🟡     │ISO-001    │OUT-003     │DB-006                      │
       │           │OUT-005     │TEST-005                    │
       │           │DEP-004     │OUT-002 OUT-004             │
       │           │CFG-003     │CFG-002                     │
├──────┼──────────┼────────────┼────────────────────────────┤
LOW    │           │            │ISO-003                     │
🟢     │           │            │                            │
       └──────────┴────────────┴────────────────────────────┘
```

---

## Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2026-05-14 | 1.0 | Initial risk register — Phase 0 baseline freeze |
