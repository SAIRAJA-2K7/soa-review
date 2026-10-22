# TalentSync Microservice Setup

This folder contains the gateway-based microservices that run alongside the existing project.

## Services

- Gateway: http://localhost:8080
- Auth Service: http://localhost:8081
- Job Service: http://localhost:8083
- Application Service: http://localhost:8084
- Eureka: http://localhost:8761

## Run

Use Java 21 and configure the local environment before starting the services. Copy `env.example` to `.env` and set `DB_PASSWORD`, a private `JWT_SECRET` of at least 32 characters, and a separate `JOB_SYNC_SECRET` of at least 32 characters. Set `MAIL_USERNAME` and `MAIL_PASSWORD` if OTP or password-reset email delivery is required. `APP_PASSWORD_RESET_URL` controls the link included in reset email; its default is the local frontend route. The local `.env` file is ignored by Git.

Start the complete stack from this directory in PowerShell:

```powershell
Copy-Item env.example .env
notepad .env
.\start-microservices.ps1
```

Set your local database password and both private service secrets in `.env` before running the script. The launcher validates configuration and required ports before building, refuses to start if any required port is already occupied, waits for each health endpoint before starting the next, and writes logs under `live-logs/`. It does not stop existing port owners. To build without starting:

```powershell
mvn clean package
```

### Optional local demo accounts

Demo accounts are disabled by default. To create local candidate or recruiter demo accounts, use a disposable development database and set these values in the ignored `microservices/.env` file:

- `SPRING_PROFILES_ACTIVE=dev`
- `APP_DEMO_USERS_ENABLED=true`
- `DEMO_USERS_CANDIDATE_EMAIL` and `DEMO_USERS_CANDIDATE_PASSWORD` (optional)
- `DEMO_USERS_RECRUITER_EMAIL` and `DEMO_USERS_RECRUITER_PASSWORD` (optional)

Choose private passwords for your local environment; there are no built-in demo passwords. Restart the auth service after changing these settings. The initializer creates only accounts that do not already exist and never resets an existing account's password. Recruiter accounts can now register directly and become available after email OTP verification; administrator approval is not part of recruiter onboarding. Never enable demo seeding outside local development or tests, and do not commit `.env`.

Recruiter API permissions remain limited to recruiter workflows and resource ownership. Public registration cannot create an `ADMIN` account. Legacy database accounts with role `ADMIN` are not changed automatically and do not have access to the retired recruiter-approval page or endpoint.

Before using direct recruiter registration with an existing PostgreSQL database, back it up and apply `db/migrations/V1__allow_recruiter_user_role.sql` through pgAdmin's Query Tool. It updates the existing `users_role_check` constraint and promotes only pending rows explicitly marked `requested_role='RECRUITER'`; it does not alter existing `ADMIN` accounts. The migration is not run automatically by the launcher, and the shared `talentsync` database was not changed during this code update.

## Example routes

- http://localhost:8080/api/auth/health
- http://localhost:8080/api/jobs
- http://localhost:8080/api/applications

Public job reads are `GET /api/jobs` and `GET /api/jobs/{id}`. Other job routes and protected APIs require `Authorization: Bearer <JWT>` from a user whose email is verified; tokens issued before OTP verification are rejected for protected requests. Internal job synchronization also requires the separate `JOB_SYNC_SECRET`; it is not routed through the public gateway. Use gateway URLs for browser/Postman requests; downstream ports are for local service-to-service traffic. Routes and service clients use Eureka service IDs by default; set `APPLICATION_SERVICE_URI` or `APPLICATION_SERVICE_URL` only for local overrides.

## Current Limitations

- Auth users use JPA; job, saved-job, application, interview, and company JSON records use service-owned PostgreSQL tables. Persistence across real PostgreSQL service restarts was not exercised in the current test environment.
- Service-owned audit tables record selected authenticated CRUD/list operations. Filter-level denials and auth login, OTP, and password-reset endpoints are not currently audited; representative H2 tests do not establish complete coverage or live PostgreSQL audit reliability.
- Password-reset tokens are stored as hashes and are single-use with a 15-minute expiry. Email delivery and browser reset flows have not been live-verified; per-email throttling is process-local, and existing JWTs are not revoked after password changes.
- Gateway and downstream JWT validation/role checks protect API routes. Keep downstream service ports private in deployments; direct-port network isolation is not configured by this local launcher.
- Two-instance load balancing, performance, production secret rotation, and database migration rollback have not been verified.

The existing frontend remains alongside these microservices; this launcher starts only the microservices.
