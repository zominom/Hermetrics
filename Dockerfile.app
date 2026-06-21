# syntax=docker/dockerfile:1
# Combined image: hermetrics API (Java) + frontend (static, served by nginx).
# Unrelated to the Flink job image (Dockerfile). Run both in one container;
# nginx serves the SPA and reverse-proxies /api/ to the local API process.

FROM maven:3.9-eclipse-temurin-21 AS api-build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM node:22-alpine AS ui-build
WORKDIR /ui
COPY ui/package.json ui/package-lock.json* ./
RUN npm ci
COPY ui/ .
RUN npm run build

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends nginx gettext-base \
    && rm -rf /var/lib/apt/lists/*

COPY --from=api-build /build/target/hermetrics-1.0-SNAPSHOT-api.jar /app/app.jar
COPY --from=ui-build /ui/dist /usr/share/nginx/html
COPY ui/default.conf.template /etc/nginx/templates/default.conf.template
COPY docker/app-entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENV LISTEN_PORT=80 \
    API_UPSTREAM=127.0.0.1:8080 \
    API_CONFIG=/etc/hermetrics/api-config.json
EXPOSE 80
ENTRYPOINT ["/entrypoint.sh"]
