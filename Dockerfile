FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /workspace
COPY . ./

RUN sed -i 's/\r$//' gradlew \
    && chmod +x gradlew \
    && ./gradlew clean bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-jammy

RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app app

WORKDIR /app
COPY --from=builder --chown=app:app /workspace/build/libs/*-SNAPSHOT.jar app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
