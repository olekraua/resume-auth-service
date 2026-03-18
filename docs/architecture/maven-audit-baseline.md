# Maven Audit Baseline (E1-T1-S1)

Status: completed

## Target files

1. Root POM
   - Relative path: `pom.xml`
   - Absolute path: `/Users/oleksandrkravchenko/Desktop/resume-microservices-repos/resume-auth-service/pom.xml`
2. Service POM
   - Relative path: `microservices/backend/services/auth-service/pom.xml`
   - Absolute path: `/Users/oleksandrkravchenko/Desktop/resume-microservices-repos/resume-auth-service/microservices/backend/services/auth-service/pom.xml`

## Verification command

```bash
rg --files | rg '(^pom.xml$|microservices/backend/services/auth-service/pom.xml$)'
```
