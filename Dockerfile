# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Firebase credentials handled via ENVIRONMENT VARIABLE (FIREBASE_SERVICE_ACCOUNT_BASE64)
# No need to copy the file anymore.
# We also need to make sure the app knows where to look if it's not on classpath root,
# but usually initializing from classpath works if it is in resources.
# However, `ClassPathResource` looks in the JAR.
# If the code uses `ClassPathResource`, it should find it inside app.jar!
# WAIT. The error said "class path resource ... cannot be opened".
# If it's in src/main/resources, Maven should package it into the JAR.
# If Maven packaged it, why is it not found?
# Maybe the file name case is wrong? Or it was excluded?
# Let's verify pom.xml to see if resources are filtered/excluded.
# But just to be safe, I'll copy it to the filesystem AND ensure it's in the JAR.
# Actually, if I copy it to /app/firebase-service-account.json, I might need to change the code to look for it there
# if ClassPathResource fails.
# BUT, `ClassPathResource` looks INSIDE the JAR.
# If it failed, it means `firebase-service-account.json` is NOT in the built JAR.
# So `mvn package` didn't put it there.
# I will check `pom.xml` in the next step.
# For now, I will add the COPY command just in case we need to switch to file system loading.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
