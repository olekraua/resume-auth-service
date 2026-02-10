# auth-service

Standalone repository for the auth-service microservice.

## Local build

```bash
./mvnw -pl microservices/backend/services/auth-service -am -Dmaven.test.skip=true package
```

## Local run

```bash
./mvnw -pl microservices/backend/services/auth-service -am spring-boot:run
```

## Included modules

- shared
- staticdata
- profile
- notification
- auth
- media
- web
- search
- microservices/backend/services/auth-service

