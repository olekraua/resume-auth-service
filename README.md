# resume-auth-service

Single-service repository for `microservices/backend/services/auth-service`.

## Build
`./mvnw -pl microservices/backend/services/auth-service -am -DskipTests package`

## Run
`./mvnw -pl microservices/backend/services/auth-service -am spring-boot:run`

## Shared libraries
This service depends on artifacts from `resume-platform-libs`.

Install/update shared libraries before building service repos:
`cd ../resume-platform-libs && ./mvnw -DskipTests install`
