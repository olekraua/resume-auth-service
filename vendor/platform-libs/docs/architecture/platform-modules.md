# Platform Module Boundaries

## Modules

- `platform-http`
  - Responsibility: outbound/inbound HTTP middleware (`RestClient` customization, retry, circuit-breaker, idempotency)
  - Entry point: `PlatformMiddlewareAutoConfiguration`
  - No domain model imports (`auth/profile/search`)

- `platform-observability`
  - Responsibility: request-id and HTTP trace propagation for servlet stack
  - Entry point: `PlatformObservabilityAutoConfiguration`
  - Depends on `platform-http` configuration properties only

- `platform-security`
  - Responsibility: password encoder auto-config, transliteration components, validation annotations/validators
  - Entry points: `PasswordEncoderConfig`, `PlatformSharedComponentsAutoConfiguration`

- `platform-test`
  - Responsibility: platform infra tests and compatibility gates
  - Contains middleware behavioral tests migrated from `resume-shared`

## Wiring Rules

- Platform modules are connected via Spring Boot `AutoConfiguration.imports` only.
- No broad component scan is required for platform infrastructure.
- Domain modules (`auth/profile/search/...`) consume platform modules through explicit Maven dependencies.

