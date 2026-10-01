# --- Build stage ---
# Uses the official Maven image (includes mvn) rather than requiring a
# committed Maven Wrapper binary, since this repo doesn't ship one yet.
# Once you run `mvn -N wrapper:wrapper` locally, switch this back to
# eclipse-temurin + ./mvnw for a wrapper-pinned Maven version.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only the POM first so Docker caches the dependency layer
# independently of source changes - much faster rebuilds.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src src
RUN mvn package -DskipTests -B

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Run as a non-root user - standard production hardening.
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
