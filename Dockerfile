# syntax=docker/dockerfile:1.7
#
# lifelog 멀티모듈 공용 Dockerfile (Eclipse Temurin 21).
# bootJar 가 활성화된 모듈(app, worker)을 MODULE 인자로 선택해 빌드합니다.
#
#   docker build --build-arg MODULE=app    --build-arg APP_PORT=8080 -t <user>/lifelog:0.0.1 .
#   docker build --build-arg MODULE=worker --build-arg APP_PORT=8081 -t <user>/lifelog-worker:0.0.1 .
#
# 빌드 컨텍스트는 항상 리포지토리 루트 (settings.gradle.kts 필요).

# ---------- 1. build ----------
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace
COPY . .

ARG MODULE=app
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :${MODULE}:bootJar \
        -x test -x check -x compileTestJava -x compileTestKotlin \
    && cp ${MODULE}/build/libs/*.jar /workspace/boot.jar

# ---------- 2. layer 분리 ----------
# Spring Boot 4 의 jarmode=tools 로 레이어를 쪼개면, 소스만 바뀐 재빌드에서
# 의존성 레이어(용량 대부분)를 Docker 캐시로 재사용하고 push 도 그만큼 빨라집니다.
# extract 는 대상 디렉터리가 비어 있어야 하므로 jar 와 분리된 경로로 추출합니다.
FROM builder AS extractor
WORKDIR /workspace
RUN java -Djarmode=tools -jar boot.jar extract --layers --launcher --destination /extract

# ---------- 3. runtime ----------
# 런타임은 21-jre-alpine (286MB). GoogleDriveHelper 가 ImageIO/Graphics2D 로
# 썸네일을 만들기 때문에 libfreetype/libfontconfig 가 필요하고, 이 이미지에는
# 포함되어 있습니다.
#
# 주의: amazoncorretto:21-alpine3.21 로 바꾸면 폰트가 없어 Graphics2D.drawString
# 에서 실패합니다. Corretto 는 슬림 JRE 태그를 제공하지 않아 사용 가능한
# 조합이 21-headful(612MB) 뿐이고, 최종 이미지가 약 325MB 커집니다.
#
# musl libc 기반이므로 JNI 네이티브 의존성에 주의하세요. 현재는 문제가 없지만,
# Kafka 의 compression.type 을 snappy/lz4 로 켜는 경우 검증이 필요합니다.
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# 변경 빈도가 낮은 레이어부터 복사. 사용자를 만들지 않고 숫자 UID 를 쓰는 이유는
# 베이스 이미지에 useradd 가 있는지에 의존하지 않기 위함입니다.
COPY --from=extractor --chown=1001:1001 /extract/dependencies/ ./
COPY --from=extractor --chown=1001:1001 /extract/spring-boot-loader/ ./
COPY --from=extractor --chown=1001:1001 /extract/snapshot-dependencies/ ./
COPY --from=extractor --chown=1001:1001 /extract/application/ ./

# GoogleDriveConfig.TOKENS_DIRECTORY_PATH = "tokens" (CWD 기준 상대 경로).
# 컨테이너 재시작 후에도 OAuth 토큰을 유지하려면 볼륨 마운트가 필요합니다.
RUN mkdir -p /app/tokens /app/log && chown -R 1001:1001 /app/tokens /app/log

USER 1001:1001

ARG APP_PORT=8080
EXPOSE ${APP_PORT}

ENV HOME=/app \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Seoul -Djava.awt.headless=true"

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
