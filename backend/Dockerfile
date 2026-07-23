# Multi-stage build for the ClaimLens Spring Boot backend (Java 21).

# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies first (uses the container's own maven, not the wrapper jar,
# which is gitignored).
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Then build the application.
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# Fonts for PDF generation. The claim-report PDF is rendered with openhtmltopdf/PDFBox, which needs
# real fonts on the system; the JRE image ships none, so without this the PDF comes out with missing
# glyphs (and PDFBox logs a font-cache warning on every render).
RUN apt-get update \
    && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

# Copy the built fat jar (exclude the plain jar Spring Boot also produces).
COPY --from=build /app/target/*.jar app.jar

# Run as a non-root user.
RUN useradd -r -u 1001 appuser
USER appuser

# The host injects PORT; the app reads it via server.port. MaxRAMPercentage keeps the JVM within a
# small container (e.g. Render free 512 MB) — the container-unaware default heap is far too small.
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-jar", "app.jar"]
