# Deprecation Policy

## Standard Lifecycle

1. **Deprecate**
- Mark API with `@Deprecated` and Javadoc replacement target.
- Add entry in `CHANGELOG.md` under `Deprecated` (or `Changed` with migration note).

2. **Support Window**
- Keep deprecated API for at least one `MINOR` release cycle.
- Do not remove deprecated API in `PATCH` releases.

3. **Removal**
- Remove only in next `MAJOR` release.
- Include migration instructions in release notes.

## Compatibility Guardrail

- If deprecated API is removed without major bump, compatibility gate must fail.
- CI integration must run `ci/verify-platform-compatibility.sh` for non-major releases.

