# Stage 1: Build Spring Boot app
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline

COPY src/ src/

RUN ./mvnw clean package -Dmaven.test.skip=true

# ==================================================
# OpenTelemetry Java Agent
# ==================================================
FROM alpine:3.22 AS otel-agent

ARG OTEL_JAVA_AGENT_VERSION=2.31.1

RUN apk add --no-cache curl \
	&& curl -fsSL \
	   "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_JAVA_AGENT_VERSION}/opentelemetry-javaagent.jar" \
	   -o /opentelemetry-javaagent.jar

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
COPY --from=otel-agent /opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
