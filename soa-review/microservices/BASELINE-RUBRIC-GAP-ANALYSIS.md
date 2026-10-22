# TalentSync Baseline and Rubric Gap Analysis

## Baseline

- Repository: existing TalentSync React/Vite application with five Spring Boot microservice modules.
- Initial Git check from the nested project path reported branch `main`. A later authoritative check from the outer repository root found modified Eclipse metadata files in `application-service`, `gateway`, and `job-service` (`.classpath`, `.project`, and resource encoding preferences). These files were not edited or reverted as part of this work; their origin is not established.
- Frontend: React 19, Vite 8, JavaScript/JSX, existing candidate and recruiter workflows.
- Backend: Java 21, Spring Boot 3.3.3, Spring Cloud 2023.0.3, Eureka, Gateway, PostgreSQL configured for auth, JWT, and SMTP OTP.
- Lifecycle: editor diagnostics report Spring Boot 3.3.x is outside its OSS and commercial support windows; this is a baseline dependency-maintenance gap.
- Service ports: Eureka `8761`, gateway `8080`, auth `8081`, job `8083`, application `8084`.
- Existing API families: `/api/auth/**`, `/api/user/**`, `/api/jobs/**`, `/api/applications/**`, and `/api/admin/**` through the gateway.
- Existing data behavior: auth users use JPA; jobs and applications/interviews/company data are held in static in-memory maps/lists.
- Existing security behavior: auth creates signed JWTs; gateway validates tokens on non-auth requests and forwards identity headers. Gateway security itself permits all exchanges, and downstream job/application controllers do not independently authorize the forwarded identity or role.
- Existing automated tests at baseline: `OtpServiceTest` and `UserServicePersistenceTest`; no job-service, application-service, gateway, or end-to-end tests were present.
- Baseline verification: Maven reactor tests passed on Java 21; `npm run build` passed. A browser screenshot was not available in this environment.
- Startup/configuration: `start-microservices.ps1` sets database, SMTP, and JWT settings inline and starts services in sequence. The current gateway YAML requires `JWT_SECRET`; the startup script sets it. Manually launching the gateway without that variable will fail configuration.

## Rubric Gaps

Status meanings: **Verified** means inspected and/or exercised; **Partial** means present but incomplete or not validated end to end; **Missing** means no implementation/evidence was found; **Blocked** means the environment does not support verification.

| # | Rubric | Baseline status | Evidence and remaining gap |
|---|---|---|---|
| 1 | Design Refinement | Partial | Existing service boundaries are recognizable, but controllers contain in-memory business/data state; no architecture decision record or centralized API error contract. |
| 2 | Service Implementation | Partial | Auth CRUD/profile and API endpoints exist. Job/application/interview/company operations use process-local data rather than durable persistence; some user-facing operations are therefore lost on restart. |
| 3 | JWT Authentication | Partial | Signed JWT generation and gateway signature/expiry parsing exist. Role/ownership authorization is incomplete; downstream services trust gateway routing and do not independently validate identity. JWT secrets are not consistently managed for standalone startup. |
| 4 | API Gateway Configuration | Partial | Routes and CORS exist. Authentication filter rejects missing/invalid tokens; route-specific role policy, timeouts, rate limiting, request-size bounds, and explicit unavailable-service behavior are not established. |
| 5 | Inter-service Communication | Partial | Job service calls application service for apply. Communication has a fixed URL and no explicit timeout/error mapping or integration tests. |
| 6 | Integration of Services | Partial | Gateway routes and frontend calls are wired. Full user workflow is not covered by repeatable integration tests; in-memory stores are separate and volatile. |
| 7 | Service Discovery | Verified / Partial | Services register with Eureka and gateway uses discovery for most routes. Runtime registry was not rechecked in this baseline; application-service route is currently direct URL rather than discovery. |
| 8 | Load Balancing | Missing | Eureka and a load-balancer dependency exist, but job/application state is local memory and no multi-instance distribution/failover test exists. |
| 9 | Testing Implementation | Partial | Two auth tests pass. No automated job, application, gateway-security, or cross-service workflow tests were present. |
| 10 | Performance and Reliability | Missing / Partial | No measured performance baseline, pagination, request timeouts, bounded retries, or circuit-breaker evidence. In-memory mutable collections have no durable multi-instance consistency. |
| 11 | Final System Implementation | Partial | Frontend builds and backend tests pass. Full workflow, restart persistence, and failure behavior remain unverified/incomplete. |
| 12 | System Testing and Validation | Partial | Baseline unit/persistence tests and frontend build pass. Browser screenshots and complete live workflow evidence were not captured. |
| 13 | Deployment and Execution | Partial | A PowerShell startup script and health endpoints exist. It embeds operational secrets and has fixed local paths; documentation does not fully reflect required environment setup/order. |
| 14 | Analytical Insights of Design | Missing | Existing README is a brief startup note; no evidence-based architecture, security, communication, or trade-off documentation. |
| 15 | Conceptual Clarity and Articulation | Missing | No evaluator Q&A, rubric mapping, honest feature status, or demo checklist was found. |

## Preservation Constraints

- Keep the existing React pages, styles, navigation, route structure, and project/module layout unchanged unless a proven defect requires a compatible frontend adjustment.
- Preserve existing API paths and response shapes where practical.
- Do not reset databases or delete existing user data.
- Do not claim a rubric score of 10/10 without implementation and repeatable evidence.
- Recheck Git diff and rerun the relevant builds/tests after each implementation slice.

## Post-Implementation Evidence

- Gateway JWT role/header-sanitization, unverified-token, public-job-read, and health-probe tests pass.
- Auth JWT filter tests pass for missing tokens, tampered tokens, disabled/unverified users, and trusted identity forwarding.
- JWTs now carry account verification state; auth-service checks current active/verified/role state, and gateway/job/application filters reject unverified or missing verification claims.
- Public job list/detail GET routes remain unauthenticated for compatibility with the existing frontend; job writes and apply routes still require role-appropriate verified tokens.
- Job mirror synchronization now uses an internal, non-gateway route protected by both a verified recruiter JWT and a separate service secret.
- Job writes validate required text fields and numeric salary ranges; public job discovery remains GET-only.
- Job-service H2 persistence tests pass for seeding, create/list ownership, and recruiter-only writes.
- Application-service H2 persistence tests pass for profile-derived candidate details, candidate scoping, and database uniqueness reservations for duplicate applications.
- Full backend Maven reactor tests passed after the final JWT filter exception-handling change.
- Final verification after this audit: `mvn package -q` passed; Surefire reported 34 tests across 9 suites, with 0 failures, 0 errors, and 0 skipped tests.
- Frontend production build passed both before implementation and after the UI-preserving implementation changes.
- The microservices were not restarted against a configured private `microservices/.env`; therefore PostgreSQL persistence across real process restarts and a live gateway-to-Eureka workflow remain unverified here.
- PostgreSQL TCP port `5432` was reachable, but the local `.env`, `DB_PASSWORD`, and `JWT_SECRET` were unavailable and `psql` was not installed; no DB credentials were requested, displayed, or guessed.
- The three Eclipse metadata file groups reported above remain untouched; no frontend source, CSS, routes, or UI files were modified.

## Follow-up Verification

- The following bullets record the earlier follow-up snapshot; the newer results and audit/reset limitations are recorded in **Latest Audit and Password-Recovery Follow-up** below.
- The follow-up pass preserved the original rubric scores as the baseline and updated final estimates only for evidenced changes in `ARCHITECTURE-AND-DEMO.md`.
- The five-module microservices Maven reactor passed on Java 21: 42 tests across 12 suites, 0 failures, 0 errors, and 3 skipped optional PostgreSQL tests. The separate monolithic `backend` project passed `mvn -B clean test`: 5 tests across 2 suites, 0 failures, 0 errors, and 0 skips. Combined: 47 test cases across 14 suites, with the three PostgreSQL tests skipped.
- Frontend production build passed without changing frontend source.
- Saved-job retrieval now batch-loads related job documents rather than issuing one repository lookup per saved item; its regression test passed.
- Application creation no longer scans the entire application table or relies on JVM-local synchronization before reserving the database-unique candidate/job key; concurrent H2 coverage verifies exactly one reservation succeeds.
- OTP challenges now enforce configured expiry, maximum attempts, case-insensitive email matching, and one-time consumption. OTP values are no longer logged, and mail-delivery failure returns an explicit service-unavailable response; focused tests pass.
- Added disabled-by-default per-instance response markers, a repeatable Eureka/load-balancing/latency probe, and safe PostgreSQL tests gated to databases ending in `_test` or `_integration`.
- The launcher now rejects busy ports rather than killing their owners, reports child-process exits, and cleans up only processes started by that invocation after startup failure.
- Both PowerShell scripts passed parser validation. A local Eureka/gateway/job-service run registered two job instances; 120 warm concurrent GET requests (concurrency 8) succeeded with 60 access-log records per instance. Throughput was 452.92 requests/second, median 12.92 ms, and p95 66.63 ms. After stopping the `18085` instance and waiting for Eureka to converge, all 25 follow-up requests succeeded through the surviving `18083` instance.
- The live routing/benchmark run used a disposable shared H2 file database and a read-only route. Docker/Podman, `psql`, a dedicated PostgreSQL test URL, and private database credentials were unavailable; all three PostgreSQL tests remained skipped. PostgreSQL persistence/restart, PostgreSQL cross-instance uniqueness, full cross-service workflow, and production performance remain unverified.
- No files were deleted. Existing user changes, including Eclipse metadata and the unrelated editor settings diff, were not reverted.

## Latest Audit and Password-Recovery Follow-up

- Implemented service-owned PostgreSQL audit tables and controller aspects for auth profile operations, job/saved-job/admin-job-list operations, and application/interview/company/admin-list operations. Success events are committed with each service-local business transaction; handled controller failures roll back the mutation and attempt a failure event in a separate transaction.
- Representative H2 integration tests verify job create/read and validation failure, plus applicant application create/read and recruiter status update. JWT filter tests now verify that single-header and enumerated-header access both expose verified identity rather than client-supplied email, role, or ID.
- Audit scope is intentionally not described as complete: gateway/service-filter denials and auth login, OTP, and password-reset endpoints are not audited; not every supported mutation has a dedicated integration test; no PostgreSQL audit-write-failure or tamper-resistance test ran. Failure-event persistence is best-effort if the database itself rejects the audit write, and no distributed transaction spans services.
- Password reset now accepts request/consume operations, returns an account-enumeration-safe generic request response, stores a SHA-256 token hash with a 15-minute expiry, and changes the password with the existing BCrypt encoder. H2 tests cover hashed storage, generic request responses, expired/reused credentials, and concurrent consumption. Mail delivery is mocked in tests; SMTP and browser delivery were not verified. Per-email throttling is in-memory per auth-service instance, and existing JWTs are not revoked after reset.
- OTP challenge state is persisted as a hash with expiry, failed-attempt limit, and one-time consumption. The tests use H2; cross-instance PostgreSQL locking behavior remains unverified.
- Latest Java 21 verification: `mvn test -q` in `microservices/` passed 51 tests across 16 suites, with 0 failures, 0 errors, and 3 skipped tests. All three skips are the opt-in PostgreSQL integration tests because `SOA_REVIEW_POSTGRES_TEST_URL` was not configured. The password-reset tests include recruiter-role accounts and repeated-request throttling. No dedicated test database was used.
- The nested frontend production build (`npm run build`) passed without frontend source changes. `start-microservices.ps1` passed PowerShell parser validation and `git diff --check` passed.
- No separate `backend` Maven project exists in the current nested application directory; therefore, a distinct backend suite was not run in this pass.
- The requested launcher rebuilt and started the updated service JARs. Eureka, gateway, auth, job, and application health endpoints returned HTTP 200; job/application actuator health reported PostgreSQL `db=UP`; and the gateway job-list request returned HTTP 200.
- A read-only `psql` connection using `.env` verified database `talentsync`, user `postgres`, and port `5432`. The expected users, service documents, audit, OTP, and reset tables exist, as does `users_id_seq`. Startup logs showed connection-pool initialization and no password-authentication or sequence warning. No PostgreSQL account or authentication rule was modified.
- No applicant/recruiter test records were created or altered in the shared database. Live browser CRUD/audit operations, SMTP delivery, reset links, and persistence of business records across a restart remain unverified because no approved disposable database or test accounts were provided.

## Final Verification Pass — 2026-10-21

- `mvn -f soa-review/microservices/pom.xml test -q` ran with Java `21.0.9` and exited 0: 52 tests across 16 suites, 0 failures, 0 errors, and 3 skipped PostgreSQL tests. The skipped tests are the opt-in auth, job, and application PostgreSQL persistence cases. Application/job persistence and audit tests use H2; auth contexts use embedded H2; password-reset email is mocked.
- `npm run build` passed from the frontend package root. PowerShell parser validation for `start-microservices.ps1` and `git diff --check` passed. Health endpoints currently report gateway, auth, job, and application services `UP`; job/application database health is `UP`.
- No dedicated PostgreSQL test URL, username, or password is configured. PostgreSQL integration requires process-scoped `SOA_REVIEW_POSTGRES_TEST_URL`, `SOA_REVIEW_POSTGRES_TEST_USERNAME`, `SOA_REVIEW_POSTGRES_TEST_PASSWORD`, and `JWT_SECRET` for the auth context. Use a disposable, least-privilege database whose name ends in `_test` or `_integration`; never target the shared `talentsync` database.
- Node.js is present, but Playwright/browser automation is not installed and no frontend dev server was available. No browser workflows or SMTP deliveries were attempted. Local mail settings exist, but successful SMTP authentication/delivery is unverified; no reset mail or token was sent.
- H2 fault injection confirmed an audit-write failure prevents the local job mutation from committing. When both success and failure audit writes fail, the failure event is absent, but the original request error is preserved and the business mutation is rolled back. A guard prevents self-suppression from masking that error. PostgreSQL audit-failure behavior remains unverified.
- Audit correlation fields now retain only canonical UUID request headers; arbitrary header values are replaced with a generated UUID. Job and application H2 integration tests cover this behavior. The auth aspect uses the same guard without a dedicated audit integration test. Filter-level denials, auth registration/login/OTP/reset, and internal mirrored-job writes remain outside the current audit aspects. Audit rows have no ordinary-user API, but there is no database-level append-only enforcement.
- The password-reset and OTP test evidence is H2-only. Reset credentials are SHA-256 hashed, 15-minute and single-use in the service logic; the H2 suite covers expiry, replay, concurrent consumption, generic request responses, recruiter-role accounts, and repeated-request throttling. Throttling is process-local, SMTP is mocked, and multi-instance PostgreSQL behavior/session invalidation are unverified.
- Confirmed route inventory includes applicant profile read/update, job browse/save/remove/apply, application create/read, recruiter job create/update/read, application review/status update, recruiter interview read/create, and company-profile read/update. Job deletion, application withdrawal, interview update/reschedule/cancel, and profile deletion are not implemented. No live applicant or recruiter workflow was executed.
- Auth profile controller startup invokes demo-user seeding with fixed source-level passwords. Those accounts were not used; this is a production security risk that should be removed or explicitly gated before deployment.
- Public registration accepts a requested role, and `UserService.normalizeRole` permits `ADMIN`; no trusted recruiter approval/invitation step was found. This is a confirmed self-promotion defect and blocks acceptance of recruiter authorization boundaries. It was not altered here because changing role provisioning affects recruiter onboarding and needs an explicit policy.
- Running service processes were not restarted after these source changes; health checks refer to the pre-existing JARs. The shared PostgreSQL database was not used for test mutations or restart testing.

## Latest Security Hardening and Evidence — 2026-10-21

The items below supersede the earlier open findings about public recruiter self-assignment and unconditional fixed-password demo seeding. Other rubric limitations above remain unless specifically resolved here.

| Area | Status | Evidence and remaining gap |
|---|---|---|
| Public role self-assignment | **Partially verified** | Public registration persists `USER`; recruiter intent is a pending request. H2 tests cover privileged role input, pending recruiter requests, profile role spoofing, and role-gated approval. Gateway/PostgreSQL end-to-end checks remain blocked. |
| Recruiter approval | **Partially verified** | ADMIN-only approval endpoints are now exposed through a website admin screen. Approved accounts receive the distinct `RECRUITER` role; service authorization tests distinguish recruiter workflows from admin-only approval. H2 covers actor attribution and approval audit. No live browser/PostgreSQL workflow was run; existing legacy recruiter accounts stored as `ADMIN` require operator review. |
| Demo-user seeding | **Verified in H2 dev/prod-profile tests** | Initializer requires `dev`/`test` plus `APP_DEMO_USERS_ENABLED=true`; admin credentials are supplied through `DEMO_USERS_ADMIN_EMAIL/PASSWORD`. Tests verify encoded password, preservation of existing accounts, pending recruiter role, and no production initializer. No real environment was seeded. |
| Audit consistency | **Partially verified** | Auth profile and recruiter approval audit events are persisted in H2; approval success and rejected unverified approval were asserted. Existing job H2 fault injection proves local mutation rollback if success and failure audit writes fail. Failure-event storage remains best-effort; no durable outbox, database immutability, or comprehensive route audit coverage exists. |
| Password reset / OTP | **Partially verified** | Existing H2 tests cover hashed reset credentials, expiry, reuse, concurrent consumption, generic request response, and throttling. Delivery is mocked; throttling is per-process; PostgreSQL concurrency and session/JWT invalidation are unverified. |
| PostgreSQL CRUD/audit/restart | **Blocked** | The dedicated `SOA_REVIEW_POSTGRES_TEST_URL`, `SOA_REVIEW_POSTGRES_TEST_USERNAME`, and `SOA_REVIEW_POSTGRES_TEST_PASSWORD` are absent. No mutations or restart tests targeted shared `talentsync`. Supply an isolated `_test`/`_integration` PostgreSQL database plus test credentials and `JWT_SECRET`. |
| Browser applicant/recruiter acceptance | **Blocked** | Playwright Chromium installed, but no project Playwright test harness, dedicated accounts/test data, test DB, or updated-code safe runtime was available. No browser workflows were run. |
| SMTP delivery | **Unverified** | Local SMTP variables are configured but were not exercised; no mail catcher or delivery evidence is available. |
| Frontend compatibility | **Partially verified** | Production Vite build passed after adding the recruiter-approval-pending message/redirect. Browser rendering, authenticated workflows, and network behavior remain unverified. |

- Final Java 21 reactor command: `mvn -f soa-review/microservices/pom.xml test -q` from the outer workspace; exit code 0. Surefire totals: **58 tests across 18 suites, 0 failures, 0 errors, 3 skipped**; all skips are opt-in PostgreSQL persistence tests. Auth/job/application tests executed here used H2.
- The separate `backend/pom.xml` suite was also run with Java 21 and exited 0: **5 tests across 2 suites, 0 failures, 0 errors, 0 skips**, using H2.
- `npm run build` passed; PowerShell parser validation for `start-microservices.ps1` passed; `git diff --check` passed. Java 21 launcher detection was not changed.
- Read-only health probes returned HTTP 200 on ports 8080, 8083, 8084, and 8761, and via the gateway auth health path. Direct auth port 8081 was unavailable. These probes reached existing processes, not binaries rebuilt from this source state; no restart was attempted against the shared database.
- No SMTP/reset token, real credentials, or PostgreSQL test values were printed. No commit was created. Existing unrelated `.vscode`/Eclipse metadata and other pre-existing work were not intentionally modified.

## Recruiter Registration Policy Update — 2026-10-21

This update supersedes the earlier approval-flow findings above:

| Area | Current status | Evidence and remaining limitation |
|---|---|---|
| Recruiter registration | **Verified in H2 tests** | Explicit `RECRUITER` registration creates a recruiter-role account directly. `ADMIN`, unknown, and arbitrary role inputs create only `USER`; profile payloads cannot alter role. Email OTP verification is still required before protected requests. |
| Admin approval | **Removed from current source** | The approval controller, `/admin` frontend page, admin-specific route, and demo-admin seed were removed. Auth and gateway tests reject the retired approval route. This does not remove pre-existing `ADMIN` rows from PostgreSQL. |
| Recruiter authorization | **Partially verified** | Gateway and service authorization continue to require recruiter identity for recruiter workflows. H2/gateway tests verify role boundaries, but browser and live PostgreSQL ownership scenarios have not been run. |
| PostgreSQL role constraint | **Blocked pending operator migration** | `db/migrations/V1__allow_recruiter_user_role.sql` adds `RECRUITER` to the existing `users_role_check` and promotes only pending recruiter-request rows. The migration was not applied to the shared database; back up and apply it to the intended local database before recruiter registration there. |
| Demo-user seeding | **Verified in H2 tests** | The opt-in dev/test initializer now seeds candidate/recruiter accounts only; no admin account is seeded. Existing rows and passwords are not overwritten. |
| Live/browser acceptance | **Blocked / unverified** | No database migration, shared-database mutation, service restart, browser signup, SMTP delivery, or recruiter workflow was performed. |

- Updated Java 21 verification: `mvn -f soa-review/microservices/pom.xml test -q` passed: **59 tests across 19 suites, 0 failures, 0 errors, 3 skipped**. The skips are opt-in PostgreSQL tests; executed service persistence tests use H2.
- The existing frontend production build passed after final frontend cleanup. `git diff --check` and PowerShell launcher syntax validation passed. No launcher code was changed.
- Recruiter signup remains email-verified by design; removing admin approval does not remove OTP verification. No `.env` values were read, printed, or changed.