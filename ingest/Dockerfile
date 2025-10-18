FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY ingest ingest
RUN chmod +x gradlew

RUN ./gradlew :ingest:bootJar --no-daemon --no-build-cache

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /workspace/ingest/build/libs/ingest-*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
