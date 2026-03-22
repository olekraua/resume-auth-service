# Repository Structure Standard: resume-auth-service

## Repository type
java-service

## Mandatory top-level directories
- docs/adr
- docs/architecture
- docs/runbooks
- docs/slo
- api/openapi
- api/asyncapi
- api/proto
- deploy/k8s/base
- deploy/k8s/overlays/dev
- deploy/k8s/overlays/prod
- scripts
- test/integration
- test/performance

## Package and module rules
- Keep domain boundaries strict: one service owns one domain write model.
- Keep transport code out of domain logic.
- Use idempotent write handlers and outbox publish points for every state mutation.
- Keep generated code in dedicated directories and never mix with hand-written domain logic.

## Review checklist for new files
- Is this file in the right domain or module folder?
- Does this change preserve API and event contract compatibility?
- Is the latency, CPU, and memory budget documented for this path?
- Is there a corresponding test where required?
