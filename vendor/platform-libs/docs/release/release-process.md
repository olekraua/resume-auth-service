# Release Process

1. Update version in root `pom.xml`.
2. Update `CHANGELOG.md`:
- Move relevant entries from `Unreleased` to new version section with release date.
3. Run validation:
- `./mvnw -DskipTests install`
- `./mvnw verify`
4. Run compatibility gate for non-major release:
- `REFERENCE_VERSION=<previous_release> ci/verify-platform-compatibility.sh`
5. Tag release and publish artifacts.

