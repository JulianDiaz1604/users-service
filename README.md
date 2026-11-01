# users-service

Users, roles and JWT authentication — microservice of the **Plazoleta de comidas** challenge (PRAGMA PowerUp).

## User stories

- HU1 — Create owner
- HU5 — Authentication & JWT
- HU6 — Create employee
- HU8 — Create client

## Stack

Java 21 · Spring Boot 3.5.x · Gradle 8.10.x · PostgreSQL · MapStruct · Lombok · springdoc-openapi · JUnit 5 + Mockito.

## Architecture

Hexagonal (ports & adapters): `domain` / `application` / `infrastructure`. Full conventions in `CLAUDE.md`.

## Run

```bash
./gradlew clean build
./gradlew bootRun   # http://localhost:8081 - Swagger UI at /swagger-ui.html
```
