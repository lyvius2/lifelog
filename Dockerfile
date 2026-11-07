# syntax=docker/dockerfile:1.7
#
# lifelog 경량 컨테이너 이미지.
#
#   docker build -t <user>/lifelog:0.0.1 .
#
# 빌드 컨텍스트는 항상 리포지토리 루트입니다 (settings.gradle.kts 필요).
#
# 경량화 전략 — 큰 것부터:
#   1. jlink 로 애플리케이션이 쓰는 모듈만 담은 런타임 생성.
#      temurin:21-jre-alpine(286MB) 대신 alpine(8MB) + 커스텀 런타임을 씁니다.
#   2. 개발 전용 의존성(h2, embedded-redis)과 쓰이지 않는 플랫폼 네이티브
#      (netty-codec-native-quic 5종)를 bootJar 에서 제외 — app/build.gradle.kts 참고.
#   3. jarmode=tools 로 레이어 분리 — 소스만 바뀐 재빌드에서 의존성 레이어를 캐시 재사용.

# =============================================================================
# 1. build — bootJar 생성
# =============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace
COPY . .

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :app:bootJar \
        -x test -x check -x compileTestJava -x compileTestKotlin \
    && cp app/build/libs/app-0.0.1-SNAPSHOT.jar /workspace/boot.jar


# =============================================================================
# 2. extract — Spring Boot 레이어 분리
# =============================================================================
# extract 는 대상 디렉터리가 비어 있어야 하므로 jar 와 분리된 경로로 추출합니다.
FROM builder AS extractor

WORKDIR /workspace
RUN java -Djarmode=tools -jar boot.jar extract --layers --launcher --destination /extract


# =============================================================================
# 3. jre — jlink 커스텀 런타임
# =============================================================================
# 모듈을 직접 나열합니다. jdeps 자동 탐지는 리플렉션·서비스로더로만 닿는 모듈을
# 놓치기 때문에, 아래처럼 근거를 적어 고정하는 편이 재현성과 디버깅에 낫습니다.
# 모듈을 빼면 런타임에 NoClassDefFoundError 나 Provider not found 로 나타납니다.
FROM eclipse-temurin:21-jdk-alpine AS jre

# 모듈 목록은 한 줄로 둡니다. 역슬래시로 줄을 이으면 들여쓰기 공백이 모듈명에
# 그대로 붙어 "Module         java.security.sasl not found" 로 실패합니다.
# 각 모듈의 선정 근거는 바로 아래 주석에 있습니다.
RUN jlink \
      --add-modules java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.transaction.xa,java.xml,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.jfr,jdk.localedata,jdk.management,jdk.naming.dns,jdk.net,jdk.unsupported,jdk.zipfs \
      --include-locales=en,ko \
      --strip-debug \
      --no-header-files \
      --no-man-pages \
      --compress=zip-6 \
      --output /javaruntime

# 모듈 선정 근거
#   java.compiler       javax.lang.model — Spring Data JPA 의 repository-aot-processor 가
#                       기동 중 참조합니다. 빼면 BeanCreationException:
#                       javax/lang/model/SourceVersion 으로 기동이 실패합니다.
#   java.desktop        ImageIO / Graphics2D — GoogleDriveHelper 의 썸네일 생성.
#                       java.beans 도 여기 있어 Spring 이 항상 필요로 합니다.
#   java.instrument     Spring 의 LoadTimeWeaver, OpenTelemetry 에이전트
#   java.logging        JUL → SLF4J 브리지 (Tomcat 이 JUL 사용)
#   java.management     Actuator / Micrometer JVM 메트릭
#   jdk.management        └ 확장 OS·프로세스 메트릭
#   java.naming         Tomcat JNDI, JDBC DriverManager 경로
#   java.net.http       java.net.http.HttpClient — spring-ai RestClient
#   java.rmi            JMX 커넥터가 참조
#   java.scripting      SpEL 주변·일부 템플릿 엔진의 ScriptEngine 탐색
#   java.security.jgss  GSSAPI — MySQL / MongoDB 인증 경로가 참조
#   java.security.sasl  MongoDB SCRAM-SHA 인증에 필수
#   java.sql            JDBC
#   java.transaction.xa DataSource 의 XA 인터페이스
#   java.xml            logback XML 설정 파싱
#   jdk.crypto.ec       TLS ECDHE 암호군 — HTTPS, rediss://, mongodb+srv 에 필수
#   jdk.crypto.cryptoki PKCS#11 — JCA 공급자 탐색 시 참조
#   jdk.localedata      한국어 로케일 (en,ko 만 포함해 용량 절감)
#   jdk.naming.dns      DNS SRV/TXT 조회. MongoDB Atlas 의 mongodb+srv:// URI 를
#                       드라이버가 JNDI DNS 로 풀기 때문에 필요합니다. 로컬에서
#                       mongodb:// 로 테스트하면 드러나지 않고 운영에서만 터집니다.
#   jdk.net             jdk.net.ExtendedSocketOptions — Lettuce 가 TCP keepalive 를
#                       설정할 때 참조합니다. 빼면 Redis 연결이 전부 실패해
#                       캐시를 쓰는 페이지가 모두 500 이 됩니다 (기동은 성공).
#   jdk.unsupported     sun.misc.Unsafe — Netty, Hibernate, Kotlin 이 사용
#   jdk.zipfs           중첩 jar 를 ZipFileSystem 으로 여는 경로
#   jdk.jfr             운영 중 프로파일링이 필요할 때를 위한 보험 (용량 영향 작음)


# =============================================================================
# 4. runtime
# =============================================================================
FROM alpine:3.21 AS runtime

# 추가 패키지를 설치하지 않습니다.
#
# GoogleDriveHelper.resizeImage 는 Graphics2D.drawImage 로만 축소하고 글자를 그리지
# 않습니다. 컨테이너 안에서 JPEG/PNG 디코딩 → 리사이즈 → 재인코딩을 직접 실행해
# freetype / fontconfig / 글꼴 / libstdc++ 없이도 전 과정이 통과함을 확인했습니다.
# 네 패키지를 빼서 약 17MB 를 줄였습니다.
#
# 주의: 썸네일에 워터마크 등 Graphics2D.drawString 을 쓰게 되면 글꼴 탐색이 실패합니다.
#       그때는 아래 한 줄을 되살리십시오.
#   RUN apk add --no-cache freetype fontconfig ttf-dejavu

ENV JAVA_HOME=/opt/java
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=jre /javaruntime ${JAVA_HOME}

WORKDIR /app

# 변경 빈도가 낮은 레이어부터 복사합니다. 사용자를 만들지 않고 숫자 UID 를 쓰는 이유는
# 베이스 이미지에 adduser 가 있는지에 의존하지 않기 위함입니다.
COPY --from=extractor --chown=1001:1001 /extract/dependencies/ ./
COPY --from=extractor --chown=1001:1001 /extract/spring-boot-loader/ ./
COPY --from=extractor --chown=1001:1001 /extract/snapshot-dependencies/ ./
COPY --from=extractor --chown=1001:1001 /extract/application/ ./

# /app/config : 런타임에 마운트하는 자격증명 자리입니다.
#     -v $(pwd)/secrets:/app/config:ro
#   아래 GOOGLE_CREDENTIALLOCATION 이 이 경로를 가리킵니다.
#   credential.json / service-account.json 은 이미지에 넣지 않습니다(.dockerignore).
# /app/tokens : GoogleDriveConfig.TOKENS_DIRECTORY_PATH = "tokens" (CWD 기준 상대 경로).
#   컨테이너를 재생성해도 OAuth 토큰을 유지하려면 볼륨으로 빼야 합니다.
RUN mkdir -p /app/config /app/tokens /app/log \
    && chown -R 1001:1001 /app/config /app/tokens /app/log

USER 1001:1001

EXPOSE 8080

# 이 이미지는 live 프로파일 전용입니다. 기본값을 박아 두지 않으면 default 프로파일로
# 떨어져 H2 드라이버를 찾다가 "Cannot load driver class: org.h2.Driver" 로 죽습니다
# (h2 는 개발 전용이라 bootJar 에 없습니다). 필요하면 실행 시 덮어쓸 수 있습니다.
ENV SPRING_PROFILES_ACTIVE=live \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Seoul -Djava.awt.headless=true" \
    GOOGLE_CREDENTIALLOCATION=file:/app/config/credential.json \
    HOME=/app

# busybox wget 을 쓰므로 curl 을 설치하지 않습니다.
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
