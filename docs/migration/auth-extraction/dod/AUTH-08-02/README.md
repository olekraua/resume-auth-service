# AUTH-08-02 DoD Checklist

Repository: `resume-auth-service`  
Task: `AUTH-08-02 — DoD checklist run`  
Acceptance target: confirm absence of imports/dependencies on domain logic from `resume-platform-libs`.

## Checklist results

1. Maven Enforcer banned dependencies check (`validate`) is green.
   - Command: `./mvnw -B -ntp -DskipTests validate`
   - Result: `BUILD SUCCESS`, rule `enforce-banned-dependencies` passed.

2. ArchUnit forbidden module imports test is green.
   - Command: `./mvnw -B -ntp -Dtest=ForbiddenModuleImportsArchTest test`
   - Result: `Tests run: 1, Failures: 0, Errors: 0`.

3. Service dependency tree (`net.devstudy` scope) contains only shared + contracts.
   - Evidence: `dependency-tree-net-devstudy-service.txt`
   - Expected/actual:
     - `net.devstudy:resume-shared`
     - `net.devstudy:resume-auth-contracts`
     - `net.devstudy:resume-profile-contracts`

4. Forbidden thick artifacts are absent in dependency tree.
   - Checked artifacts:
     - `net.devstudy:resume-web`
     - `net.devstudy:resume-search`
     - `net.devstudy:resume-auth`
     - `net.devstudy:resume-profile`
     - `net.devstudy:resume-notification`
   - Evidence: `forbidden-artifacts-in-dependency-tree.txt` (`NO_MATCHES`).

5. Imports from search domain module are absent.
   - Evidence: `imports-resume-search.txt` (`NO_MATCHES`).

6. Imports from non-contract internal profile/notification packages are absent.
   - Evidence: `imports-internal-profile-notification.txt` (`NO_MATCHES`).

7. Remaining profile/notification imports are contract API only.
   - Evidence:
     - `imports-profile-api.txt`
     - `imports-notification-api.txt`

8. `net.devstudy.resume.web.*` imports in this repo resolve to local source files (migrated into service), not external `resume-web` artifact.
   - Local package root exists in service sources:
     - `microservices/backend/services/auth-service/src/main/java/net/devstudy/resume/web/...`
   - Enforcer + dependency tree additionally confirm no external `resume-web` jar is used.

## Verdict

DoD check **passed** for AUTH-08-02: domain logic from `resume-platform-libs` is not consumed as external thick module dependencies/import sources in `resume-auth-service`.
