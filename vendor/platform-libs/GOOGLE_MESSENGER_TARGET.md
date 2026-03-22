# Google Messenger Target: resume-platform-libs

## Service role
Shared security, observability, transport, and SDK libraries.

## Recommended stack
- Primary runtime: Java shared libs + generated SDKs for Go/TS/Swift/Kotlin.
- Why: optimize throughput per CPU core and reduce steady-state memory while preserving reliability.

## Data ownership
No business data ownership.

## APIs and protocols
Common interceptors/middleware for HTTP, gRPC, Kafka.

## Event contracts
Shared telemetry schema versions.

## Reliability target
- SLO: Library quality gate: zero critical CVEs and backward-compatible releases.
- Latency budget: N/A (library), but enforce low allocation and low overhead helpers.

## Resource efficiency target
Benchmark core helpers in CI; prevent regression by thresholds.

## Phase-1 implementation checkpoint
Publish versioned SDK matrix from contracts pipeline.
