# resume-auth-service

Single-service repository for `microservices/backend/services/auth-service`.

## Dependencies model (cleanup state)

`resume-auth-service` uses:

- `net.devstudy:resume-shared`
- `net.devstudy:resume-auth-contracts`
- `net.devstudy:resume-profile-contracts`

`resume-web`, `resume-search` and other thick domain jars are banned by Maven Enforcer in the root `pom.xml`.

## Local prerequisites

- JDK 21
- Docker (for integration tests with Testcontainers)
- Maven credentials for GitHub Packages (`server id: github`) or local installed artifacts

If you work fully local, install fresh shared/contracts artifacts from sibling repos:

- `cd ../resume-platform-libs && ./mvnw -DskipTests install`
- `cd ../resume-contracts && ./mvnw -DskipTests install`

## Build and run

- Build and verify: `./mvnw -B -ntp verify`
- Run service: `./mvnw -B -ntp spring-boot:run`

## Test commands

- Unit/web/default checks: `./mvnw -B -ntp verify`
- Integration profile (Postgres Testcontainers + Flyway): `./mvnw -B -ntp -Pintegration verify`
- Contract consumer checks only: `./mvnw -B -ntp -Dtest='*ConsumerContractTest' test`

## CI required checks policy (`main`)

Required checks for PR merge:

- `Build and Verify`
- `Integration Tests`
- `Contract Consumer Checks`

## Architecture and planning docs

- E1-T1 Jira-ready decomposition (v2): `docs/architecture/e1-t1-decomposition-jira-ready-v2.md`
- AS-IS Maven inventory: `docs/architecture/maven-as-is-inventory.md`
- Runtime deps from parent: `docs/architecture/maven-runtime-deps-from-parent.md`
