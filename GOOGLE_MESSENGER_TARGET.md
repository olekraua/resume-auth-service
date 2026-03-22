# Google Messenger Target: resume-auth-service

## Service role
Identity, token issuance, session lifecycle, account recovery, and trust hooks.

## Recommended stack
- Primary runtime: Go 1.24 target runtime (keep Java 21 during controlled migration).
- Why: optimize throughput per CPU core and reduce steady-state memory while preserving reliability.

## Data ownership
PostgreSQL for accounts and sessions, Redis for token/session cache.

## APIs and protocols
OIDC/OAuth2.1 at edge; gRPC/HTTP internal.

## Event contracts
auth.login.succeeded, auth.login.failed, auth.token.revoked.

## Reliability target
- SLO: Token issuance success >= 99.99%.
- Latency budget: Issue token p99 <= 150 ms.

## Resource efficiency target
Bound crypto worker pools, cache JWKS and key metadata.

## Phase-1 implementation checkpoint
Introduce stateless token service path and idempotent session writes.
