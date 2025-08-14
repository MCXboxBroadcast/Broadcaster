# Build stage
FROM eclipse-temurin:21-alpine AS builder

# Install required packages for building
RUN apk add --no-cache git

# Set working directory
WORKDIR /app

# Copy gradle wrapper and build files first (for better caching)
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY build-logic/ build-logic/

# Copy source code and build files
COPY core/ core/
COPY bootstrap/ bootstrap/

# Make gradlew executable
RUN chmod +x gradlew

# Build the application
RUN ./gradlew :bootstrap-standalone:shadowJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-alpine as runtime

# Set up permissions for /tmp
RUN chmod 777 -R /tmp && chmod o+t -R /tmp

# Create app user
RUN adduser -h /opt/app -H -D app

# Create directories and set ownership
RUN mkdir -p /opt/app/config && \
    chown -R app:app /opt/app

# Switch to app user
USER app:app

# Set working directory
WORKDIR /opt/app/config

# Copy the built JAR from builder stage
COPY --from=builder /app/bootstrap/standalone/build/libs/MCXboxBroadcastStandalone.jar /opt/app/MCXboxBroadcastStandalone.jar

# Run the application
CMD ["java", "-jar", "/opt/app/MCXboxBroadcastStandalone.jar"]
