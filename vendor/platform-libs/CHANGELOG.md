# Changelog

All notable changes to this project are documented in this file.

The format is based on Keep a Changelog, and this project follows Semantic Versioning.

## [Unreleased]
### Added
- Introduced platform module split: `platform-observability`, `platform-http`, `platform-security`, `platform-test`.
- Added explicit AutoConfiguration wiring via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Added package status map and module boundaries documentation.
- Added compatibility gate script: `ci/verify-platform-compatibility.sh`.

### Changed
- Moved infrastructure middleware from `resume-shared` into dedicated `platform-*` modules.
- Moved shared validation/transliteration/password infra into `platform-security`.
- Refactored `platform-shared-auth` into compatibility facade artifact.

### Removed
- Removed domain leakage from platform/shared layer (`DataBuilder`, `ProfileMediaCleanupRequestedEvent`).

## [0.0.1] - 2026-03-21
### Added
- Initial baseline release.
