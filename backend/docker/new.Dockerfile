FROM amazoncorretto:21@sha256:edb6b3e12b360c67d9bd8ec8b4106ce9285603e0f436e58b74da1fa32da5a378 AS builder

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

FROM amazoncorretto:21-alpine@sha256:58c1d555f4ff3be0cfe90d3b4d1762bde080b57afbb71d48657b9d22748cad5b

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
