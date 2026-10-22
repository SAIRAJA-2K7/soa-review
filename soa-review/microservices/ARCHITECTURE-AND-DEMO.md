# TalentSync Architecture and Demo Guide

## Project Summary

TalentSync is an existing React/Vite hiring application backed by Spring Boot services. The browser uses the API Gateway; the gateway validates signed JWTs and routes requests to authentication, job, and application services. Eureka provides service registration and discovery.

```mermaid
sequenceDiagram
    participant C as Candidate browser
    participant G as API Gateway
    participant A as Auth service
    participant J as Job service
    participant P as Application service
    participant DB as PostgreSQL
    participant R as Recruiter browser

    C->>G: POST /api/auth/login
    G->>A: Forward public login
    A->>DB: Find active user and verify BCrypt password
    A-->>C: Signed JWT (email, role, verified, expiry)
    C->>G: GET /api/jobs + Bearer JWT
    G->>G: Verify signature, expiry, role; replace identity headers
    G->>J: Forward request with verified identity
    J->>J: Verify JWT and enforce candidate/recruiter policy
    J->>DB: Read published job documents
    C->>G: POST /api/jobs/{id}/apply + Bearer JWT
    G->>J: Verify candidate role and forward request
    J->>DB: Persist application-independent job state
    J->>P: POST /api/applications with same bearer token
    P->>A: GET /api/user/profile with same bearer token
    A-->>P: Canonical candidate profile
    P->>P: Verify token, candidate, and job ownership
    P->>DB: Persist application and timeline
    R->>G: GET /api/admin/applications + recruiter JWT
    G->>P: Enforce ADMIN role
    P->>DB: Return only applications for this recruiter
    P-->>R: Recruiter application list
```

## Service Responsibilities

| Service | Port | Responsibility | Data |
|---|---:|---|---|
| Eureka | 8761 | Service registration and lookup | Registry in memory |
| Gateway | 8080 | Routes, JWT validation, role checks, trusted identity forwarding | Stateless |
| Auth | 8081 | Registration, login, persistent OTP, JWT issuance, profile, password reset | PostgreSQL `users`, OTP challenge, reset credential, and audit tables |
| Job | 8083 | Job create/read/update, saved jobs, application submission orchestration | PostgreSQL `job_service_documents` |
| Application | 8084 | Applications, recruiter candidates/pipeline, interviews, company profile, job mirror | PostgreSQL `application_service_documents` |
| Frontend | 5173 (development) | Existing candidate/recruiter user interface | Browser session storage for JWT |

Service data tables are owned by each service and store the existing JSON API payloads in text columns. Hibernate `ddl-auto: update` creates/updates these service-owned tables without dropping the monolith's existing domain tables. Auth users continue using their existing relational JPA model.

## Request and Security Behavior

- Login is public at `POST http://localhost:8080/api/auth/login` and returns `token`, `type`, `email`, and `role`.
- Use `Authorization: Bearer <token>` for protected requests through the gateway.
- Gateway and job/application/auth services verify the JWT signature and expiration with the same `JWT_SECRET`.
- Tokens include account verification state; all API filters reject tokens for unverified users. Auth-service also checks that the account remains active, verified, and on the same role.
- Public job discovery is read-only: `GET /api/jobs` and `GET /api/jobs/{id}` need no JWT. Job mutations, saved-job operations, applications, and recruiter routes remain protected.
- Gateway removes client-provided `X-User-Email` and `X-User-Role` headers and replaces them with verified claims.
- Job/application/auth services validate the bearer token again, enforce role rules, and replace identity headers before controller handling.
- The servlet filters replace `X-User-Email`, `X-User-Role`, and `X-User-Id` for both single-header and enumerated-header access used by Spring MVC; client-supplied identity headers are not authoritative.
- Recruiter endpoints under `/api/admin/**` require `ADMIN`; candidate applications/saved jobs require `USER`; job create/edit and application status changes require `ADMIN`.
- Candidate application reads are scoped to the JWT email. Recruiter job/application/interview/company reads and writes are scoped to the JWT email or the job's persisted owner.
- The public auth service endpoints intentionally remain accessible without a token.
- Job mirror synchronization uses an internal service route outside the gateway's public route table. It requires a verified recruiter JWT plus a separate `JOB_SYNC_SECRET` shared only by job-service and application-service.
- `X-Service-Instance` is emitted only when `LOAD_BALANCING_INSTANCE_HEADER_ENABLED=true`; keep this diagnostic setting disabled outside local verification.

## CRUD Audit and Password Recovery

Each service stores its own audit events in PostgreSQL: `auth_service_audit_events`, `job_service_audit_events`, and `application_service_audit_events`. Job and application audit aspects write one event per matched HTTP operation (including list reads, not one per returned row); auth auditing currently covers profile reads and updates. Events include the identity supplied by the verified service filter, role, operation, resource type/ID where available, timestamp, outcome, HTTP status, and sanitized changed-field names. Password, token, secret, authorization, OTP, and email field names are omitted from the changed-field list. There are no applicant/recruiter audit-management endpoints.

This is not a complete or tamper-proof audit subsystem. Filter-level 401/403 denials and auth login, OTP, and password-reset routes are not covered by these aspects. Success events are written in the local service transaction with the business operation; controller failures roll back the mutation and attempt a failure event in a new transaction. If that failure-event write also fails, the original request error is preserved and the audit failure is attached as suppressed information. Cross-service work is not distributed-transactional, and database administrators can still alter audit rows. Representative H2 tests cover job create/read/validation failure and application create/read/status update, but do not prove every CRUD path or PostgreSQL failure behavior.

Password recovery uses `POST /api/auth/password-reset/request` with `{"email":"..."}` and `POST /api/auth/password-reset/consume` with `{"token":"...","password":"..."}`. The request endpoint returns the same generic message for existing and unknown accounts. A cryptographically random token is emailed; only its SHA-256 hash is stored, it expires after 15 minutes, and consumption changes the BCrypt password and removes outstanding reset credentials in one database transaction. The same flow applies to applicant and recruiter accounts in the shared user store. The service throttles requests per normalized email for 60 seconds, but the limiter is process-local and is not shared across auth-service instances. Successful password changes do not revoke already-issued JWTs; existing tokens remain valid until their configured expiry.

`PasswordResetServiceIntegrationTest` exercises hashed storage, generic responses, expired/reused tokens, concurrent consumption, and password update using an H2 database and a mocked mail sender. It does not verify actual SMTP delivery, browser reset-link behavior, role-specific live accounts, or PostgreSQL row locking. Configure SMTP values in the ignored local `.env`; never place tokens or credentials in screenshots, logs, or documentation. Mail delivery failures leave the endpoint's generic response unchanged and are reported only by a sanitized server warning.

## Local Setup

Prerequisites: Java 21, PostgreSQL on the configured database, Node.js, and PowerShell. The workspace includes Maven 3.9.11 under the sibling `maven` directory.

From `microservices/`, create the ignored local environment file and fill in private values:

```powershell
Copy-Item env.example .env
notepad .env
```

Set `DB_PASSWORD`, a private `JWT_SECRET` of at least 32 characters, and a separate `JOB_SYNC_SECRET` of at least 32 characters. Configure SMTP credentials only if email OTP delivery is needed. Do not commit `.env` or share bearer tokens.

Start/rebuild all backend services:

```powershell
.\start-microservices.ps1
```

Start the unchanged frontend in another PowerShell terminal:

```powershell
cd ..
npm run dev
```

Open `http://localhost:5173`. The startup script validates configuration and occupied ports, builds first, then waits for each health endpoint before starting the next. It does not kill existing port owners and stops only its own started processes if startup fails. Confirm health before demoing:

```powershell
Invoke-WebRequest http://localhost:8761
Invoke-WebRequest http://localhost:8080/actuator/health
Invoke-WebRequest http://localhost:8081/api/auth/health
Invoke-WebRequest http://localhost:8083/actuator/health
Invoke-WebRequest http://localhost:8084/actuator/health
```

## Postman Demo Checklist

1. `POST http://localhost:8080/api/auth/login`, raw JSON body:

```json
{"email":"recruiter@demo.com","password":"password123"}
```

2. Copy the returned `token` into Postman **Authorization → Bearer Token**. A Postman environment variable can be set in the login request's Post-response script:

```javascript
pm.environment.set("jwt_token", pm.response.json().token);
```

3. With the recruiter token, call `GET http://localhost:8080/api/admin/jobs`, `/api/admin/applications`, `/api/admin/candidates`, and `/api/admin/dashboard`.
4. Sign in as a candidate and use that token to call `GET /api/jobs`, then `POST /api/jobs/{id}/apply` with an empty JSON object (or no body). The candidate email comes from verified JWT claims; application-service obtains the canonical profile from auth-service and the recruiter owner from the persisted job.
5. Sign back in as recruiter and call `GET /api/admin/applications` and `GET /api/admin/candidates`; verify the application appears. Use `PATCH /api/applications/{id}/status` with `{"status":"shortlisted"}` to demonstrate recruiter follow-up.
6. Demonstrate authorization: candidate token on `/api/admin/applications` should return `403`; missing or invalid bearer token should return `401`.

Use the gateway URL for client requests. Direct service requests still require the bearer token; ports are not a substitute for authentication.

## Multi-instance, Failover, and Performance Checks

The gateway routes `JOB-SERVICE`, `APPLICATION-SERVICE`, and `AUTH-SERVICE` through Eureka and Spring Cloud LoadBalancer. Services use service IDs for discovery, and persistent job/application records are stored in PostgreSQL rather than process-local collections. OTP challenge values are stored as hashes in PostgreSQL with expiry, attempt limits, row locking, and one-time consumption. Cross-instance PostgreSQL OTP verification and reset-token consumption have not been live-tested; password-reset throttling remains process-local.

For a local measurement only, enable the opt-in response marker before starting the stack. It is disabled by default and reveals only `service-name:port` in `X-Service-Instance`; leave it disabled outside local verification:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:LOAD_BALANCING_INSTANCE_HEADER_ENABLED = 'true'
.\start-microservices.ps1
```

In the same PowerShell session, start a second instance of each service on a free port. Each process inherits the local configuration loaded by the launcher:

```powershell
$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
$jobInstance2 = Start-Process -PassThru -FilePath $java -WorkingDirectory $PWD `
  -ArgumentList '-jar', (Join-Path $PWD 'job-service\target\job-service-1.0.0.jar'), '--server.port=8085'
$applicationInstance2 = Start-Process -PassThru -FilePath $java -WorkingDirectory $PWD `
  -ArgumentList '-jar', (Join-Path $PWD 'application-service\target\application-service-1.0.0.jar'), '--server.port=8086'
$authInstance2 = Start-Process -PassThru -FilePath $java -WorkingDirectory $PWD `
  -ArgumentList '-jar', (Join-Path $PWD 'auth-service\target\auth-service-1.0.0.jar'), '--server.port=8082'
```

Wait until the corresponding two instances report `UP` in Eureka. Then run repeatable, sequential GET probes through the gateway. The script records the active instance IDs, every response status, the observed `X-Service-Instance` counts, total requests/second, and mean/median/p95 latency:

```powershell
.\verify-load-balancing.ps1 -ServiceId JOB-SERVICE -RequestPath /api/jobs `
  -RequestCount 100 -ExpectedInstanceCount 2 -RequireExactInstanceCount -RequireDistribution
.\verify-load-balancing.ps1 -ServiceId AUTH-SERVICE -RequestPath /api/auth/health `
  -RequestCount 100 -ExpectedInstanceCount 2 -RequireExactInstanceCount -RequireDistribution
```

For application-service, use a valid recruiter token obtained by the login flow above; the token is accepted as a parameter and is not printed by the script:

```powershell
.\verify-load-balancing.ps1 -ServiceId APPLICATION-SERVICE -RequestPath /api/admin/applications `
  -BearerToken $recruiterToken -RequestCount 100 -ExpectedInstanceCount 2 `
  -RequireExactInstanceCount -RequireDistribution
```

To check recovery, stop only the specific secondary process started in this shell (for example, `Stop-Process -Id $jobInstance2.Id`) and wait until Eureka shows exactly one `UP` instance for that service. Re-run the probe with `-ExpectedInstanceCount 1 -RequireExactInstanceCount`. A request made before Eureka removes the stopped instance can still fail; no retry or circuit breaker is configured. Retain the probe output, including the two `service-name:port` counts and HTTP errors, as the load-balancing evidence. The live evidence below covers only the job-service GET path; the auth and application-service procedures remain unexecuted.

### Verified local job-service result

On 2026-10-20, an isolated local runtime used Eureka on `18761`, the gateway on `18080`, and two job-service processes on `18083` and `18085`. Both registered as `UP`. A warm `GET /api/jobs` probe sent 120 requests at concurrency 8 through the gateway. All 120 returned 2xx; Tomcat access logs recorded 60 requests on each instance. Measured throughput was 452.92 requests/second, median latency 12.92 ms, and p95 latency 66.63 ms.

The two instances shared a disposable H2 file database (`AUTO_SERVER=TRUE`), not PostgreSQL. After the `18085` process was stopped, Eureka converged to the single `18083` instance; 25/25 subsequent gateway requests returned 2xx and all 25 appeared in the survivor's access log. Port `18085` was no longer listening. No requests were issued during Eureka eviction, so transient behavior before convergence was not measured.

The benchmark is a single warm local smoke run, not a before/after comparison or production capacity claim. The requests were read-only; they do not verify duplicate writes, PostgreSQL consistency, or horizontal scaling of auth/application-service. Runtime output and access logs were collected under ignored `microservices/target/lb-verification/`; temporary runtime files were removed after verification.

The same script provides a small, dependency-free **serial** latency/throughput smoke benchmark against the public job listing:

```powershell
.\verify-load-balancing.ps1 -RequestPath /api/jobs -RequestCount 100
```

Record the request count, warm/cold state, instance count, requests/second, mean/median/p95 latency, and errors from the output. This script is a sequential smoke benchmark; the concurrency-8 result above came from a separate local .NET `HttpClient` probe. Neither run is a before/after performance comparison.

## PostgreSQL Integration Tests and Restart Persistence

The regular Maven suite uses H2 for isolated tests. Three opt-in PostgreSQL tests exercise auth-user, job-document, and application-document persistence. They are skipped unless `SOA_REVIEW_POSTGRES_TEST_URL` is set, and reject any database name not ending in `_test` or `_integration`. They use `ddl-auto=update`, make only uniquely keyed test records, and delete those records; demo initialization can still add seed data, so use a dedicated disposable database only.

In a separate PowerShell session, set values for a dedicated test database and run:

```powershell
$env:SOA_REVIEW_POSTGRES_TEST_URL = 'jdbc:postgresql://localhost:5432/talentsync_test'
$env:SOA_REVIEW_POSTGRES_TEST_USERNAME = '<dedicated-test-database-user>'
$env:SOA_REVIEW_POSTGRES_TEST_PASSWORD = '<dedicated-test-database-password>'
$env:JWT_SECRET = '<private-JWT-secret-from-local-configuration>'
& ..\..\maven\apache-maven-3.9.11\bin\mvn.cmd -B `
  -pl auth-service,job-service,application-service -am `
  -Dtest=PostgresPersistenceIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

To verify real restart persistence after the stack is configured, create a recruiter-owned job through `POST http://localhost:8080/api/jobs` using the recruiter bearer token, save the returned `id`, stop the Java process listening on port `8083` only after confirming its command line is the `job-service-1.0.0.jar`, then relaunch that JAR on port `8083` with the same environment. After `/actuator/health` returns 200, call `GET http://localhost:8080/api/jobs/{id}` and compare the persisted fields with the original response. Do not use this procedure against a non-test database without permission; the POST creates a real record.

## Evaluator Questions

**Where is the JWT generated?** `auth-service/config/JwtUtil.java`, called from `auth-service/controller/HealthController.java` after successful authentication.

**Where is it validated?** `gateway/JwtAuthenticationFilter.java` validates and routes; `auth-service/config/AuthJwtFilter.java`, `job-service/security/ServiceJwtFilter.java`, and `application-service/security/ServiceJwtFilter.java` independently validate downstream requests.

**How are roles enforced?** Gateway and service filters distinguish `ADMIN` and `USER`; recruiter endpoints require `ADMIN`, and candidate write/read surfaces enforce candidate access and identity ownership.

**How are services discovered?** Each service registers with Eureka. Gateway and job-service use service IDs; job-service communication uses Spring Cloud LoadBalancer.

**What is stored durably?** Auth users; JSON payload records for jobs, saved jobs, applications, interviews, and company data; plus audit events, OTP challenge hashes, and password-reset credential hashes. Eureka's registry and browser session JWT are not durable application records. Business-record persistence across a service restart was not exercised in this pass.

**What is not yet demonstrated?** The updated services now start against PostgreSQL and report healthy, and focused H2 tests cover representative audit and reset operations. A live candidate-to-recruiter CRUD/audit workflow, SMTP delivery, live password reset, persistence of approved business records across service restarts, database migration rollback, and production secret management remain unverified.

**Is the framework baseline currently supported?** Editor diagnostics report Spring Boot 3.3.x support ended. A supported upgrade requires coordinating Spring Boot and Spring Cloud versions and rerunning compatibility tests; that separate framework migration has not been performed.

## Rubric Scorecard (Evidence-Based Estimate)

Scores are estimates from the implementation and evidence in this workspace, not a guarantee of evaluator results. “Original” is the supplied audit score; a score changes only where this pass added a verifiable implementation or documentation improvement.

| # | Rubric | Original | Final estimate | Improvement/evidence in this pass | Exact requirement still missing for 10/10 |
|---|---|---:|---|---|---|
| 1 | Design Refinement | 6/10 | 6/10 | Existing service boundaries and persistence ownership remain explicit. | Map-based controller contracts, duplicated document-store patterns, and no shared API error schema. |
| 2 | Service Implementation | 7/10 | 7/10 | Auth, jobs, applications, saved jobs, interviews, and company data persist; OTP challenge hashes and reset credential hashes are stored in PostgreSQL, with expiry and one-time-use tests. | Cross-instance PostgreSQL consumption, live SMTP/reset delivery, distributed rate limiting, broader profile/onboarding coverage, and job deletion are unverified or absent. |
| 3 | JWT Authentication | 8/10 | 8/10 | Verified-account, signature/expiry, identity sanitization, and role/ownership tests pass. | No refresh/revocation, denylist, or demonstrated key rotation. |
| 4 | API Gateway Configuration | 7/10 | 7/10 | Discovery routes, public/protected route behavior, role filter, and health access tests pass. | No rate limiting, request-size policy, or gateway-wide timeout/outage integration evidence. |
| 5 | Inter-service Communication | 6/10 | 6/10 | Bounded connect/read timeouts, internal sync authentication, rollback unit coverage, and discovery-based service IDs. | Synchronous calls, no retry/circuit breaker/correlation ID; remote commit/response-loss consistency unverified. |
| 6 | Integration of Services | 7/10 | 7/10 | Existing contracts and H2 integration tests pass; a live gateway-to-Eureka-to-job-service request path was exercised. | No real PostgreSQL-backed candidate-to-recruiter workflow or cross-service write was executed. |
| 7 | Service Discovery | 7/10 | 7/10 | Eureka reported both job instances `UP`, then converged to the surviving instance after one process stopped. | Runtime discovery was exercised for job-service only; restart/re-registration and other service registries remain unverified. |
| 8 | Load Balancing | 4/10 | 7/10 | A 120-request concurrent gateway probe reached both job instances (60 each); after one instance stopped, all 25 subsequent requests reached the survivor. | Only a local read-only job route was exercised with shared H2; auth/application distribution, cross-instance writes, PostgreSQL, and production conditions remain unverified. |
| 9 | Testing Implementation | 7/10 | 7/10 | H2 coverage remains; added saved-job batch-lookup test and opt-in PostgreSQL tests for three services. | PostgreSQL tests were not executed; full HTTP workflow/browser tests are absent. |
| 10 | Performance and Reliability | 5/10 | 6/10 | Removed saved-job N+1 document reads; measured a small local 120-request/concurrency-8 smoke run and retained a repeatable latency/error probe. | No before/after baseline; unbounded list reads, no pagination/pool tuning/circuit breaker, and DB query plans remain. |
| 11 | Final System Implementation | 7/10 | 7/10 | Updated microservice JARs started against PostgreSQL; H2 regression tests and the existing frontend build pass. | No live applicant/recruiter workflow or business-record persistence across restart was verified. |
| 12 | System Testing and Validation | 6/10 | 7/10 | Microservices suite, frontend build, live PostgreSQL connectivity/schema, gateway route, and service health were verified. | PostgreSQL CRUD/audit/reset scenarios, browser E2E, SMTP delivery, and a separate backend suite remain unverified/not present in this project directory. |
| 13 | Deployment and Execution | 6/10 | 7/10 | Launcher rebuilt and started services with ignored local configuration; script syntax and service health checks pass. | No production deployment, least-privilege DB account, secret rotation, or rollback evidence. |
| 14 | Analytical Insights of Design | 7/10 | 8/10 | Architecture, trust boundaries, audit consistency limits, password-reset/OTP scaling caveats, multi-instance results, and measured benchmark conditions are documented. | No formal ADR history, query-plan analysis, or before/after benchmark comparison. |
| 15 | Conceptual Clarity and Articulation | 8/10 | 9/10 | Demo, persistence limitations, multi-instance/failover results, measured performance, and remaining gaps are documented. | No evaluator feedback; PostgreSQL and complete workflow commands still need execution in a safe configured environment. |

## Verification Record

- Latest verification on 2026-10-21: the five-module microservices reactor passed `mvn test -q` on Java 21 with 51 tests across 16 suites, 0 failures, 0 errors, and 3 skipped PostgreSQL integration tests. The optional PostgreSQL tests were skipped because `SOA_REVIEW_POSTGRES_TEST_URL` was not configured; password-reset tests also cover recruiter-role accounts and per-email request throttling.
- Latest frontend `npm run build` passed from the nested frontend directory; no frontend source was changed in this pass. The PowerShell parser check and `git diff --check` passed.
- There is no separate `backend` Maven project under the current nested application directory, so no separate backend suite was run in this pass.
- The audit/password-reset integration tests use H2 and a mocked mail sender. They are code-level regression evidence, not live PostgreSQL, SMTP, or browser end-to-end verification.
- The requested launcher rebuilt and started the updated service JARs. Eureka, gateway, auth, job, and application endpoints returned HTTP 200; gateway, job, and application health reported `UP`, and job/application actuator database components reported `UP`. The public gateway job-list request returned HTTP 200.
- A read-only `psql` connection using the same local `.env` database settings succeeded for `talentsync` on port 5432 as `postgres`. The `users`, service document, three audit, OTP challenge, and password-reset tables were present; `users_id_seq` exists. Updated auth/job/application startup logs showed pool initialization and no password-authentication or sequence warning. The configured account is PostgreSQL's `postgres` superuser; no account or authentication rules were changed. Consider migrating to a dedicated least-privilege runtime account after defining and reviewing the required schema privileges.
- The initial PostgreSQL error was a rejected password for `postgres`; the active `.env` settings now authenticate to the expected local database/port. The previous rejected password value was not printed or retained for comparison, so its exact textual mismatch cannot be established. No password was guessed or reset.
- No applicant or recruiter test records were created or modified in `talentsync`. Without an approved disposable test database or explicit test accounts, live CRUD/audit/reset operations and business-record persistence across service restarts were not attempted. SMTP credentials are configured locally but delivery was not tested; `APP_PASSWORD_RESET_URL` is absent from `.env`, so the local default URL is used.

## Final Verification Pass — 2026-10-21

- Re-ran the five-module reactor with Java `21.0.9` using `mvn -f soa-review/microservices/pom.xml test -q` from the outer repository root. Exit code was 0; the Surefire XML reports contain 52 tests across 16 suites, 0 failures, 0 errors, and 3 skips. The skipped cases are the three opt-in PostgreSQL persistence tests. The initial invocation without `-f` failed because the outer root has no POM; the explicit reactor-POM invocation succeeded.
- Application/job persistence and audit integration suites use H2. Auth service test contexts use embedded H2; reset mail is mocked. The three PostgreSQL tests only test simple persistence operations and were not run.
- The frontend `npm run build` succeeded from the nested frontend root. No frontend files were changed during this verification pass. The launcher PowerShell parser check and `git diff --check` both passed.
- Read-only health checks returned `UP` for gateway, auth (via `/api/auth/health`), job, and application services; job and application reported `db=UP`. No business API route or database mutation was used in this pass.
- `SOA_REVIEW_POSTGRES_TEST_URL`, `SOA_REVIEW_POSTGRES_TEST_USERNAME`, and `SOA_REVIEW_POSTGRES_TEST_PASSWORD` are absent from both the process environment and local `.env`; the tests also require `JWT_SECRET` for the auth-service PostgreSQL context. PostgreSQL CRUD/audit verification and restart persistence remain **blocked**. To enable them safely, provision a disposable database such as `talentsync_verification_test`, then set those variables in the test process only, using a least-privilege test role and a private secret source. Do not point them at `talentsync`; the URL must use PostgreSQL JDBC and the database name must end in `_test` or `_integration`.
- Node.js is available, but the project-local Playwright package/browser is unavailable and the frontend dev server was not running. No browser flow was run. Although local mail settings are present, SMTP credentials/delivery were not validated, and no reset email was sent.
- H2 fault injection verified a failed success-audit write rolls back a job mutation. When the separate failure-event write also fails, no audit row persists; the original request fails and its local business mutation remains rolled back. A self-suppression guard prevents the same exception instance from masking the original failure. This tests the job-service path only; PostgreSQL failure handling remains unverified.
- Audit events now accept incoming correlation IDs only when they are canonical UUIDs; other client header values are replaced with a generated UUID. Job and application H2 audit tests verify arbitrary values are not stored. Auth profile auditing uses the same guard but has no dedicated integration test. No ordinary audit API endpoint was found, but database-level immutability is not enforced. Filter-level rejections and auth registration/login/OTP/password-reset paths are outside the current aspects; the internal mirrored-job write is also outside the application-service audit path.
- The supported API surface has profile GET/PATCH; job list/detail/create/update; saved-job list/add/remove; application create/list/detail and recruiter status update; recruiter dashboard/list reads, interview creation, and company-profile read/update. No job-delete, application-withdraw, interview-update/reschedule/cancel, or candidate-profile-delete route was found. Job mirroring crosses service-local transactions without a distributed transaction; an application-service mirror could therefore remain if the job-service audit/commit later fails.
- The auth profile controller invokes demo-user seeding during construction, and the seed definitions contain fixed source-level passwords. These accounts were not used for verification. Treat this as a production security risk: remove or explicitly gate demo seeding before production deployment, and rotate any such credentials if they were ever exposed.
- Public registration accepts a client-supplied role and `UserService.normalizeRole` preserves `ADMIN`; no recruiter approval or invitation gate was found. This permits self-registration with recruiter/admin privileges and is a confirmed authorization defect. It was not changed in this verification pass because forcing a role would alter recruiter onboarding policy; define a trusted approval/provisioning rule before real-account testing or deployment.
- The currently running JAR processes were not restarted after the source-only audit changes. Their health checks confirm the pre-existing live processes and database connectivity, not that the new correlation-ID and failure-handling code is deployed. No restart or mutation was performed against the shared database.

- Baseline before implementation: Java 21 Maven tests passed; `npm run build` passed. The initial nested-path Git status printed no changed paths; the final outer-repository status includes Eclipse metadata changes not manually edited by this work.
- Final follow-up verification on Java 21: the five-module `mvn -B test` reactor passed with 42 tests across 12 suites, 0 failures, 0 errors, and 3 skipped tests. The skips are the opt-in PostgreSQL tests because `SOA_REVIEW_POSTGRES_TEST_URL` was not configured.
- The separate monolithic `backend` project passed `mvn -B clean test` with 5 tests across 2 suites and no failures or errors. The initial incremental run encountered stale compiled test classes; the clean run rebuilt successfully.
- `npm run build` passed in the nested frontend project. No frontend source, CSS, UI, route, navigation, or layout files were modified.
- Both PowerShell scripts passed parser validation; an isolated launcher check confirmed it rejects a missing `DB_PASSWORD` before starting a build or process. The launcher was not run with private configuration.
- A saved-job retrieval regression test passed after replacing per-job lookups with a batch repository query.
- A local isolated runtime used Eureka `18761`, gateway `18080`, and job-service instances `18083` and `18085`; Eureka showed both instances `UP`. A warm 120-request gateway probe at concurrency 8 returned 120/120 2xx responses, with 60 requests recorded by each instance's Tomcat access log. Measured throughput was 452.92 requests/second, median latency 12.92 ms, and p95 latency 66.63 ms.
- After the `18085` job-service process was stopped and Eureka converged to one `UP` instance (`18083`), all 25 follow-up gateway requests returned 2xx and all 25 appeared in the surviving instance's access log; port `18085` was no longer listening. Requests were not sent during the Eureka eviction interval, so transient failure behavior before registry convergence was not measured.
- This was a local, read-only job-list smoke test using two instances sharing a disposable H2 file database, not PostgreSQL. It does not establish balanced behavior for auth/application services, cross-instance write consistency, or production performance. The latency result is one warm run with no before/after baseline.
- PostgreSQL persistence, process-restart durability, the complete candidate-to-recruiter workflow, PostgreSQL cross-instance duplicate rejection, and distributed rollback behavior remain unverified because Docker/Podman, `psql`, the private `.env`, and dedicated test credentials were unavailable. No credentials were guessed and no unknown database endpoint was used.
- Editor diagnostics report the existing Spring Boot `3.3.3` baseline is outside its OSS and commercial support windows. Coordinate an upgrade with a compatible Spring Cloud release before production use; it was not mixed into this targeted security/persistence implementation.
- This verification found and fixed an unverified-account JWT bypass: JWTs now carry `verified`, and gateway plus downstream filters reject false/missing verification claims. Auth-service also checks live account active/verified/role state.
- This verification restricted public job reads to GET list/detail, protected all other job APIs, and moved internal job synchronization off the gateway route table behind `JOB_SYNC_SECRET`.
- Application-service now resolves candidate profile fields directly from auth-service and uses a database uniqueness reservation to prevent duplicate candidate/job applications across instances.
- Job-service rejects malformed required fields and invalid salary ranges; failed downstream synchronization rolls back the local job record.
- Live prerequisites: no `microservices/.env`, `DB_PASSWORD`, `JWT_SECRET`, or `JOB_SYNC_SECRET` were available. Docker/Podman and `psql` were unavailable, so a disposable PostgreSQL service could not be started and PostgreSQL authentication/schema/restart checks were not run.
- Database duplicate-apply reservations use the service-owned unique `(collection_name, record_key)` constraint. H2 tests verify collision rejection; PostgreSQL behavior remains unverified because local DB credentials are unavailable.

## Security Hardening Follow-up — 2026-10-21

- Recruiter onboarding no longer uses an administrator approval step. The frontend sends the explicit `RECRUITER` role; auth registration assigns only `RECRUITER` or `USER`, and attempts to request `ADMIN` or another role fall back to `USER`. Email OTP verification remains required before protected requests are accepted.
- The `/admin` recruiter-approval page and `/api/auth/admin/**` controller have been removed. The gateway denies the retired auth-admin route, and auth-service denies unknown auth routes. Recruiter API authorization and resource-ownership checks remain separate; registration does not grant admin privileges.
- Demo seeding remains opt-in for `dev`/`test` only, but now supports candidate and recruiter accounts only. Existing accounts are not overwritten. Admin demo seeding is removed; existing database accounts carrying legacy `ADMIN` are not automatically changed.
- Existing PostgreSQL databases must back up and apply `db/migrations/V1__allow_recruiter_user_role.sql` before recruiter signup can persist. It updates the role check, promotes only pending recruiter requests marked `requested_role='RECRUITER'`, and retains `ADMIN` for existing rows. This script was not executed against the shared database.
- Earlier statements in this section about an approval screen, approval audit events, and administrator demo seeding describe the prior implementation and are superseded by this policy update. Audit coverage outside that removed approval workflow is unchanged.
- Audit failure events remain best-effort when the database itself rejects audit persistence; there is no durable outbox or database-enforced append-only policy. Existing JWTs are not revoked after password reset, and reset throttling remains process-local; these are documented limitations, not verified protections.
- Full five-module Maven reactor on Java `21.0.9`: **58 tests, 0 failures, 0 errors, 3 skipped**. The three skips are opt-in PostgreSQL tests. All executed service tests use H2; they do not prove PostgreSQL behavior.
- Separate `backend/pom.xml` regression suite on Java 21 also passed: 5 tests across 2 suites, 0 failures/errors/skips, using its H2 test datasource.
- Frontend `npm run build` passed. `start-microservices.ps1` parsed successfully and `git diff --check` passed. Only the pending-approval message/flow was changed in the existing frontend; no layout, styles, or routes were redesigned.
- A read-only probe found HTTP 200 at gateway `8080`, job `8083`, application `8084`, Eureka `8761`, and gateway-routed auth health. Direct auth port `8081` was unavailable. These are existing processes, not the source revision tested above. No restart was performed because there is no approved disposable PostgreSQL database; no mutations were made against shared `talentsync`.
- `SOA_REVIEW_POSTGRES_TEST_URL`, `SOA_REVIEW_POSTGRES_TEST_USERNAME`, and `SOA_REVIEW_POSTGRES_TEST_PASSWORD` remain unavailable. PostgreSQL persistence, audit rows, and restart behavior are therefore blocked; use a disposable `_test`/`_integration` database and a least-privilege test role, never `talentsync`.
- Node.js `24.4.1` is available. `npx --yes playwright install chromium` exited 0 and installed Chromium after a transient FFmpeg download DNS retry. No project Playwright test package/config, dedicated applicant/recruiter test accounts, disposable database, local test mail receiver, or safe updated-code server was available, so browser workflows and mail delivery were not run. SMTP configuration presence is not evidence of successful delivery.