# Build stage
FROM gradle:jdk25-alpine AS build
WORKDIR /app
COPY . .
RUN gradle clean :runtime --no-daemon -PisProduction=true

# Runtime stage
FROM alpine:latest
EXPOSE 8080
ENV MMV_HOSTNAME="http://localhost:8080"
WORKDIR /data
COPY --from=build /app/build/image /app
ENTRYPOINT ["/app/bin/monomovie"]
