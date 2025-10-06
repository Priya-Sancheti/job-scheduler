# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the final, slim image
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/job-scheduler-*.jar scheduler.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "scheduler.jar"]
