# Build stage
FROM gradle:jdk21-jammy AS build
WORKDIR /app
COPY . .
RUN gradle clean build --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /data
COPY --from=build /app/build/libs/app.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]