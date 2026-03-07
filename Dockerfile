# Stage 1: Build the Spring Boot jar using Gradle (JDK 21)
FROM gradle:8.10.2-jdk21 AS build

WORKDIR /home/gradle/project

# Copy build scripts first to leverage Docker layer caching
COPY --chown=gradle:gradle gradlew gradlew.bat build.gradle settings.gradle ./
COPY --chown=gradle:gradle gradle ./gradle

RUN chmod +x ./gradlew

# Copy sources
COPY --chown=gradle:gradle src ./src

# Build executable jar
RUN ./gradlew bootJar --no-daemon \
  && JAR_PATH=$(ls -1 build/libs/*.jar | grep -v "plain" | head -n 1) \
  && test -n "$JAR_PATH" \
  && cp "$JAR_PATH" /tmp/app.jar


# Stage 2: Run the application (JRE 21)
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /tmp/app.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
