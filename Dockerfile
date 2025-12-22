# Build stage
FROM gradle:jdk25-alpine AS build
WORKDIR /app
COPY . .
RUN gradle clean :runtime --no-daemon

# Runtime stage
FROM alpine:latest
WORKDIR /data
COPY --from=build /app/build/image /app
ENTRYPOINT ["/app/bin/monomovie"]