# AUTH-08-03 Release Runbook (Rollout / Rollback)

Scope: `resume-auth-service` auth-domain extraction release.
Goal: safe rollout with fast rollback path in case of regression.

## 1. Pre-release checks (must be green)

1. CI required checks on release commit:
   - `Build and Verify`
   - `Integration Tests`
   - `Contract Consumer Checks`
2. Local verification (optional but recommended):
   - `./mvnw -B -ntp verify`
   - `./mvnw -B -ntp -Pintegration verify`
   - `./mvnw -B -ntp -Dtest='*ConsumerContractTest' test`
3. Confirm banned dependency guardrail is active:
   - `./mvnw -B -ntp -DskipTests validate` (enforcer must pass)

## 2. Rollout steps

1. Deploy to staging environment.
2. Run smoke checks on staging:
   - REST: `register`, `login`, `logout`, `logout-all`, `me`, `csrf`, `restore access` request/status/confirm.
   - OIDC: `/.well-known/openid-configuration`, `/oauth2/token`, `/oauth2/jwks`.
   - Internal flow: auth -> profile lookup and outbox write (`RESTORE_ACCESS_MAIL`).
3. Monitor for 15-30 minutes:
   - 5xx rate,
   - auth error rate (401/403/429 spikes),
   - latency p95 for auth APIs,
   - outbox queue growth and send failures.
4. If staging is stable, deploy to production using phased traffic:
   - 10% -> 50% -> 100%,
   - hold 10-15 minutes between phases with same smoke + metrics checks.

## 3. Rollback triggers

Start rollback immediately if at least one condition is true:

1. Critical auth endpoints fail smoke checks.
2. OIDC discovery/token/jwks smoke fails.
3. Sustained 5xx increase above baseline for 10+ minutes.
4. Outbox errors grow and `RESTORE_ACCESS_MAIL` delivery path is broken.
5. Contract drift/regression detected by consumer checks in hotfix pipeline.

## 4. Rollback procedure

1. Roll back deployment to previous stable release tag/image of `resume-auth-service`.
2. Restore previous runtime config/secrets if they were changed in this release.
3. Re-run smoke checks:
   - `login`, `register`, `restore`, `logout`, `me`, `csrf`,
   - OIDC discovery/token/jwks.
4. Verify recovery criteria:
   - error rate returns to baseline,
   - no new auth outbox failures,
   - profile internal flow works again.
5. Open incident follow-up:
   - capture failed step, logs, metrics, request samples,
   - create fix-forward ticket before next rollout.

## 5. Exit criteria for successful rollout

1. All smoke scenarios pass on production.
2. No sustained error/latency regression vs baseline.
3. Outbox processing is stable, no stuck NEW/ERROR growth.
4. No contract compatibility alerts for auth/profile surfaces.
