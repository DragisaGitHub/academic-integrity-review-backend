# README.md

# Academic Integrity Review Backend

Backend service for the Academic Integrity Review platform.

## Tech Stack

- Java 21
- Spring Boot
- Gradle
- MySQL
- Spring Data JPA
- Spring Security

## Project Structure

```
src/main/java
src/main/resources
src/test/java
```

## Running the application

```
./gradlew bootRun
```

## Build

```
./gradlew build
```

## Future integrations

- REST API for document analysis
- AI plagiarism detection integration
- Review workflow management
- Docker deployment



# .gitignore

# Gradle
.gradle
build/

# IntelliJ
.idea
*.iml
out/

# Logs
*.log

# OS
.DS_Store
Thumbs.db

# Env
.env
.env.*

# Java
*.class

# Node (if ever added)
node_modules/