# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies first (for layer caching)
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-slim
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/app.jar /app/app.jar

# Render sets the PORT environment variable automatically.
# The application already reads it via server.port=${PORT:8081} in application.properties
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "/app/app.jar"]