# ADR-0001: Auth Domain Ownership by `resume-auth-service`

- Status: Accepted
- Date: 2026-03-16
- Owners: Identity and Access team
- Scope: `resume-auth-service` migration wave (AUTH-01+)

## Context

Current state:

1. `resume-auth-service` depends on platform domain artifacts (`resume-web`, `resume-search`, transitive `resume-auth`, `resume-profile`, `resume-notification`).
2. Core auth business logic is implemented outside the service owner repository (`resume-platform-libs/auth` and auth-related pieces in `resume-platform-libs/web`).
3. This blocks independent release of auth-service and creates cross-domain coupling.

Program target:

- auth business logic must be owned and released by `resume-auth-service`.
- `resume-platform-libs` must contain only platform/shared technical primitives.
- cross-service integration must go through contracts and shared middleware only.

## Decision

`resume-auth-service` is the single owner of auth domain logic.

Ownership rule:

1. All auth business logic classes (`services`, `repositories`, `entities`, `security domain behavior`, `auth outbox logic`, `auth controllers`) must live in `resume-auth-service` codebase.
2. `resume-platform-libs` must not host new auth domain logic.
3. API and event boundaries must be consumed via `resume-contracts` modules.
4. `resume-auth-service` keeps only platform-safe shared dependencies (for example `resume-shared`) and contract artifacts; thick domain dependencies are forbidden.

## Migration Contract (Repository-level)

### `resume-auth-service`

Must:

1. Host auth domain code and tests.
2. Enforce banned dependencies in Maven (`resume-web`, `resume-search`, `resume-auth`, `resume-profile`, `resume-notification`).
3. Provide required CI checks: build, tests, integration, contract-consumer checks.

### `resume-platform-libs`

Must:

1. Stop accepting new auth business logic.
2. Keep only reusable platform primitives and middleware.

### `resume-contracts`

Must:

1. Remain the single source of truth for auth/profile API and auth event contracts.
2. Gate compatibility with lint + breaking checks.

## Consequences

Positive:

1. Auth-service can be released independently.
2. Domain ownership becomes explicit and enforceable.
3. Coupling to unrelated domains decreases.

Trade-offs:

1. Short-term duplication during migration period.
2. More code and tests inside service repository.
3. Temporary CI failures until dependencies are fully removed.

## Risks and Mitigations

1. Risk: behavior drift for auth REST/OIDC endpoints.
- Mitigation: endpoint parity tests, OIDC smoke, contract checks against `resume-auth-contracts`.

2. Risk: breakage in auth -> profile internal integration.
- Mitigation: consumer tests against `resume-profile-contracts` internal endpoints and integration tests with test doubles/containerized profile path.

3. Risk: auth outbox event drift (`RESTORE_ACCESS_MAIL`) impacting downstream consumers.
- Mitigation: async contract validation against auth AsyncAPI and integration verification with relay/consumer path.

4. Risk: accidental re-introduction of banned dependencies.
- Mitigation: Maven enforcer bannedDependencies + architecture tests in CI.

5. Risk: rollout regression in production auth flow.
- Mitigation: phased rollout with health and OIDC smoke gates before full traffic.

## Rollback Approach

Rollback objective: restore previously stable auth behavior without cross-repo emergency refactors.

1. Code rollback:
- Revert `resume-auth-service` to last green release tag/commit and redeploy previous image.

2. Config rollback:
- Restore previous runtime configuration values (OIDC/session/security toggles, profile internal endpoint settings, secrets).

3. Contract rollback policy:
- If new contract versions were introduced, keep producer payload backward-compatible or pin consumer to previous compatible version during rollback window.

4. Database safety rule:
- Migration changes in this program must be backward-compatible first (additive or tolerant) before cutover.
- If a non-backward-compatible migration is ever required, it must ship with a dedicated rollback script and explicit operational runbook before release.

5. Exit criteria after rollback:
- auth-service health endpoints green,
- OIDC discovery/token/jwks smoke green,
- login/register/restore critical paths green,
- no contract-consumer regression alerts.

## Verification and Evidence

Baseline artifacts for pre-migration state:

- `docs/migration/auth-extraction/baseline/AUTH-01-01/*`

Guardrail enforcement:

- parent Maven build defines banned dependencies rule for auth-service repository.
