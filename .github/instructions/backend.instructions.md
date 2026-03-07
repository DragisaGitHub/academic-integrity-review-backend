You are working in a Spring Boot backend project.

Tech stack:

- Java 21
- Spring Boot
- Gradle
- MySQL
- Spring Data JPA
- Spring Security

Architecture rules:

controller
service
repository
domain
dto
mapper
config

Guidelines:

- Controllers must only handle HTTP layer.
- Business logic must live in services.
- Repositories must only handle persistence.
- Entities must not be exposed directly in APIs.
- Always use DTOs.

Code rules:

- Prefer constructor injection
- Use Lombok to reduce boilerplate
- Keep classes small and focused
- Follow standard Spring Boot conventions

When generating code:

- implement minimal working functionality
- avoid unnecessary abstractions
- keep incremental changes
- ensure project builds successfully

Every change must pass:

```
./gradlew build
```