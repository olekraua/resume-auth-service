# Package Layout

Recommended package layers for new code:

- `api`: controllers and public DTOs
- `application`: use-cases and orchestration
- `domain`: entities and business rules
- `ports`: outbound interfaces
- `adapters`: DB, Kafka, and HTTP implementations
- `config`: boot wiring and module setup
