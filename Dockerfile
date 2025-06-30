# Build stage
FROM gradle:jdk21-jammy AS build
WORKDIR /app
COPY . .
RUN gradle clean build --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/monomovie-1.0-SNAPSHOT-all.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]