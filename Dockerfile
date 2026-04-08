# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy Maven wrapper and metadata first to maximize layer cache reuse.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Pre-fetch dependencies into Maven local repository cache.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp dependency:go-offline

# Copy source and build the Spring Boot executable jar.
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Run as non-root for better container security.
RUN groupadd --system spring && useradd --system --gid spring --create-home spring

COPY --from=build /workspace/target/*.jar /app/app.jar
RUN chown spring:spring /app/app.jar

USER spring

# JVM options recognized automatically by the Java runtime.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport -Dfile.encoding=UTF-8"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
