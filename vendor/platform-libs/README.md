# resume-platform-libs

Shared platform libraries for resume microservices.

## Modules
- `platform-parent`
- `platform-bom`
- `platform-observability`
- `platform-http`
- `platform-security`
- `platform-test`
- `platform-shared-auth` (compatibility facade)
- `shared`
- `auth`
- `file`
- `profile`
- `notification`
- `media`
- `web`
- `search`
- `staticdata`
- `messaging`

## Build and publish locally
```bash
./mvnw -DskipTests install
```

Compatibility gate (for non-major releases):
```bash
REFERENCE_VERSION=<previous_release_version> ci/verify-platform-compatibility.sh
```

All service repositories consume these artifacts via Maven coordinates `net.devstudy:*:0.0.1-SNAPSHOT`.

## Architecture and Release Docs
- Package status map: `docs/architecture/package-status-map.md`
- Platform module boundaries: `docs/architecture/platform-modules.md`
- SemVer policy: `docs/release/semver-policy.md`
- Deprecation policy: `docs/release/deprecation-policy.md`
- Release process: `docs/release/release-process.md`

## Platform middleware (`platform-http` + `platform-observability`)
Platform middleware is now delivered via dedicated platform modules:
- Spring Boot auto-configuration for platform middleware (no manual component scan required)
- outbound `RestClient` middleware for timeout/retry/circuit breaker/trace/metrics
- inbound `Request-Id` propagation filter (`X-Request-Id`)
- inbound idempotency filter (`Idempotency-Key`) with replay from in-memory cache

Default property contract:
```yaml
app:
  platform:
    middleware:
      timeout:
        enabled: true
        connect: 2s
        read: 10s
      retry:
        enabled: true
        max-attempts: 3
        initial-delay: 100ms
        max-delay: 2s
        multiplier: 2.0
        jitter-enabled: true
        jitter-factor: 0.2
        retryable-status-codes: [408, 429, 500, 502, 503, 504]
        allowed-methods: [GET, HEAD, OPTIONS]
      circuit:
        enabled: true
        sliding-window-size: 20
        minimum-number-of-calls: 10
        failure-rate-threshold: 50.0
        permitted-calls-in-half-open-state: 5
        open-state-wait: 30s
        automatic-transition-from-open-to-half-open: true
        failure-status-codes: [500, 502, 503, 504]
        allowed-methods: [GET, HEAD, OPTIONS, POST, PUT, PATCH, DELETE]
      idempotency:
        enabled: true
        header-name: Idempotency-Key
        replay-header-name: X-Idempotency-Replay
        require-header: false
        ttl: 10m
        max-entries: 10000
        max-body-bytes: 65536
        include-query-string: true
        methods: [POST, PUT, PATCH]
      telemetry:
        enabled: true
        request-id-header: X-Request-Id
        request-id-mdc-key: requestId
        trace-id-header: X-Trace-Id
        propagate-request-id: true
        http-client-metric-name: resume.platform.http.client.requests
        idempotency-metric-name: resume.platform.idempotency.requests
        circuit-breaker-metric-name: resume.platform.circuitbreaker.calls
```
