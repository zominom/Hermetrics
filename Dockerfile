FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM flink:2.1.0-java21
# Flink Application Mode: the job jar lives in usrlib and is launched by the
# base image's docker-entrypoint.sh as `standalone-job` (JobManager) or
# `taskmanager`. There is deliberately no CMD here — the role is chosen at run.
COPY --from=build /build/target/hermetrics-1.0-SNAPSHOT.jar /opt/flink/usrlib/hermetrics.jar
