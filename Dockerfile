# Use a lightweight JDK 21 image
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Copy the built JAR file (assuming Maven build runs before Docker build)
# We will configure GitHub Actions to run 'mvn package' first
COPY target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Run the application with "prod" profile
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
