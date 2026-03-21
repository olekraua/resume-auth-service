# Dependency Inventory (Baseline -> Migrated)

## Scope
`resume-auth-service` module: `microservices/backend/services/auth-service`

## Baseline (before migration)

### Internal modules (net.devstudy)
- `resume-shared`
- `resume-auth-contracts`
- `resume-profile-contracts`

### Spring Boot starters (baseline)
- `spring-boot-starter-web`
- `spring-boot-starter-aop`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-data-redis`
- `spring-boot-starter-data-elasticsearch`
- `spring-boot-starter-security`
- `spring-boot-starter-oauth2-resource-server`
- `spring-boot-starter-oauth2-authorization-server`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `spring-boot-starter-cache`
- `spring-boot-starter-freemarker`
- `spring-boot-starter-mail`
- `spring-boot-starter-test` (test)

## Inventory after migration (what is реально needed)

### Internal modules (net.devstudy)
- `platform-shared-auth`
- `resume-auth-contracts`
- `resume-profile-contracts`

### Spring Boot starters (final)
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-data-redis`
- `spring-boot-starter-security`
- `spring-boot-starter-oauth2-resource-server`
- `spring-boot-starter-oauth2-authorization-server`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `spring-boot-starter-test` (test)

### Removed as non-required for current auth-service runtime
- `spring-boot-starter-aop`
- `spring-boot-starter-data-elasticsearch`
- `spring-boot-starter-cache`
- `spring-boot-starter-freemarker`
- `spring-boot-starter-mail`
- `caffeine`, `hibernate-jcache`, `ehcache`
- `commons-lang3`, `thumbnailator`, `jsoup`, `junidecode`, `libphonenumber` as direct deps
- `org.testcontainers:elasticsearch` (test)

## Notes
- `resume-shared` was removed from service POM and replaced with `platform-shared-auth`.
- `platform-shared-auth` contains only auth-relevant shared primitives used by this service:
  - `ApiErrorResponse`, `Constants`, `AbstractEntity`
  - form validators (`FieldMatch`, `PasswordsMatch`, `RestoreIdentifier`)
  - `DataBuilder` + transliteration
  - HTTP middleware customizer (`PlatformRestClientCustomizer` + related beans)
- Dependency trees captured in:
  - `target/dependency-tree-starters.txt` (baseline)
  - `target/dependency-tree-starters-after.txt` (final)
  - `target/dependency-tree-net-devstudy-after.txt` (final internal modules)
