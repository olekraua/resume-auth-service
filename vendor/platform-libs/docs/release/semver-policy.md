# Semantic Versioning Policy

Version format: `MAJOR.MINOR.PATCH`

## Rules

- `MAJOR`: increment when public API is changed incompatibly.
- `MINOR`: increment when backward-compatible functionality is added.
- `PATCH`: increment when only backward-compatible fixes are made.

## Public API Scope

This repository treats the following artifacts as versioned public platform APIs:

- `resume-shared`
- `platform-http`
- `platform-observability`
- `platform-security`
- `platform-shared-auth` (compatibility facade)

Public API includes public/protected Java types and Spring auto-configuration contracts.

## Release Contract

- Any binary/source incompatible change in the public API requires `MAJOR` bump.
- `MINOR` and `PATCH` releases must pass compatibility gate against previous release major.
- Compatibility gate command:
  - `REFERENCE_VERSION=<previous_release> ci/verify-platform-compatibility.sh`

