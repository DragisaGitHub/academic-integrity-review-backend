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

RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/* \
  && groupadd --system spring \
  && useradd --system --gid spring --create-home --home-dir /app spring

COPY --from=build /tmp/app.jar /app/app.jar

RUN mkdir -p /app/uploads \
  && chown -R spring:spring /app

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=75.0"

USER spring:spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl --fail --silent http://127.0.0.1:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
