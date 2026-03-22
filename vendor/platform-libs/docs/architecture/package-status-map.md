# Package Status Map: Infra vs Domain Leakage

Legend:
- `infra-clean` - platform infrastructure package, no domain logic (auth/profile/search)
- `domain` - service/domain package by bounded context
- `leakage-fixed` - package/class moved out of platform/shared layer
- `compat-facade` - compatibility module that re-exports new platform modules

## Platform Layer (`shared` + `platform-*`)

| Package Prefix | Module | Status | Notes |
|---|---|---|---|
| `net.devstudy.resume.shared.middleware.config` | `platform-http` | `infra-clean` | HTTP middleware properties + controlled auto-configuration |
| `net.devstudy.resume.shared.middleware.http` | `platform-http` | `infra-clean` | Retry/circuit/timeout/telemetry interceptors |
| `net.devstudy.resume.shared.middleware.web` | `platform-http`, `platform-observability` | `infra-clean` | Idempotency infra in `platform-http`, request-id filter in `platform-observability` |
| `net.devstudy.resume.shared.observability.config` | `platform-observability` | `infra-clean` | Explicit servlet observability auto-config |
| `net.devstudy.resume.shared.config` | `platform-security` | `infra-clean` | Password encoder auto-config |
| `net.devstudy.resume.shared.component` | `platform-security` | `infra-clean` | Transliteration contracts and implementations |
| `net.devstudy.resume.shared.validation` | `platform-security` | `infra-clean` | Validation annotations and validators |
| `net.devstudy.resume.shared.model` | `resume-shared` | `domain` | Shared domain primitives/entities |
| `net.devstudy.resume.shared.constants` | `resume-shared` | `domain` | Cross-domain constants (UI/media sizing) |
| `net.devstudy.resume.shared.dto` | `resume-shared` | `domain` | API DTOs used by domain modules |
| `net.devstudy.resume.shared.util` | `resume-shared` | `domain` | Generic helpers used by domain modules |

## Domain Leakage Fixes

| Old Location | New Location | Status |
|---|---|---|
| `net.devstudy.resume.shared.event.ProfileMediaCleanupRequestedEvent` | `net.devstudy.resume.profile.api.event.ProfileMediaCleanupRequestedEvent` | `leakage-fixed` |
| `net.devstudy.resume.shared.component.DataBuilder` (+ impl) | `net.devstudy.resume.auth.api.component.AuthDataBuilder` + `auth` impl, and `media` `CertificateNameBuilder` | `leakage-fixed` |

## Compatibility

| Artifact | Status | Notes |
|---|---|---|
| `platform-shared-auth` | `compat-facade` | Empty facade artifact with transitive deps on `resume-shared`, `platform-http`, `platform-observability`, `platform-security` |

