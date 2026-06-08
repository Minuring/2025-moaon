FROM amazoncorretto:21 AS builder

WORKDIR /app

COPY ../gradlew .
COPY ../gradle ./gradle
COPY ../build.gradle .
COPY ../settings.gradle .

RUN chmod +x ./gradlew

RUN --mount=type=cache,target=/root/.gradle,id=gradle-home,sharing=locked \
    ./gradlew --no-daemon dependencies

COPY ../src ./src

RUN --mount=type=cache,target=/root/.gradle,id=gradle-home,sharing=locked \
    ./gradlew --no-daemon bootJar

FROM amazoncorretto:21-alpine AS app

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

FROM amazoncorretto:21-alpine AS reindex

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar", "--reindex", "--spring.main.web-application-type=none"]
