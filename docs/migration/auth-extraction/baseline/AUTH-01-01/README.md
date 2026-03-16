# AUTH-01-01 Baseline: dependency + imports snapshot

Purpose: lock the pre-migration state for `resume-auth-service` before auth domain extraction.

## Artifacts

- `metadata.txt` - generation timestamp + git branch/commit + command used.
- `dependency-tree-net-devstudy.txt` - Maven dependency tree for `net.devstudy` artifacts.
- `imports-all-net-devstudy.txt` - all source imports from `net.devstudy.resume.*`.
- `imports-resume-web.txt` - imports from `net.devstudy.resume.web.*`.
- `imports-resume-search.txt` - imports from `net.devstudy.resume.search.*`.
- `imports-platform-libs-domain-shared.txt` - imports from packages currently provided by `resume-platform-libs` domain/shared modules (`auth|profile|staticdata|media|notification|shared`).

## Key findings (baseline)

1. Dependency tree contains direct reliance on:
- `net.devstudy:resume-web`
- `net.devstudy:resume-search`

2. Through `resume-web`, service currently receives transitive platform modules:
- `resume-profile`, `resume-staticdata`, `resume-auth`, `resume-media`, `resume-notification`, `resume-shared`

3. Current source imports:
- `resume-web` imports: `7`
- `resume-search` imports: `0` (see note in `imports-resume-search.txt`)
- platform-libs domain/shared imports (`auth|...|shared`): `4`
- total `net.devstudy.resume.*` imports in main sources: `12`

## Regeneration command

```bash
./mvnw -pl microservices/backend/services/auth-service -am -DskipTests dependency:tree -Dincludes=net.devstudy
```

Imports reports were generated from:
`microservices/backend/services/auth-service/src/main/java`
