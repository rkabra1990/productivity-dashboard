# Build stage
FROM maven:3.8.6-openjdk-17-slim AS build
WORKDIR /app

# Copy only the files needed for downloading dependencies
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source files
COPY src ./src

# Build the application
RUN mvn package -DskipTests

# Run stage
FROM openjdk:17-slim
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx512m -Xms512m"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
