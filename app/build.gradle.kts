plugins {
    id("org.graalvm.buildtools.native")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
}

// 네이티브 빌드가 app-...-plain.jar 를 클래스패스로 요구합니다
// (Spring Boot 의 native 연동이 runtimeClasspath 기준으로 경로를 잡습니다).
// 끄면 "Main entry point class ... neither found on classpath" 로 실패합니다.
// JVM 배포에 쓰는 산출물은 bootJar 쪽(app-0.0.1-SNAPSHOT.jar)입니다.
tasks.named<Jar>("jar") {
    enabled = true
}

dependencies {
    implementation(project(":web"))
    implementation(project(":api"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("com.github.loki4j:loki-logback-appender:1.5.2")
    implementation("com.github.ulisesbocchio:jasypt-spring-boot-starter:3.0.5")
    runtimeOnly("com.mysql:mysql-connector-j:8.2.0")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // JooqRepositoryWriteTest 가 리포지토리를 직접 주입받습니다. app 은 이 모듈들을
    // web / api 를 거쳐 런타임에만 받으므로 테스트 컴파일에는 보이지 않습니다.
    // 쓰기 경로를 실제 DB(H2 + schema.sql) 에 대고 검증할 곳이 app 뿐이라
    // (스키마와 DataSource 가 여기 있습니다) 테스트 범위로만 의존을 추가합니다.
    testImplementation(project(":blog-service"))
    testImplementation(project(":photo-archive-service"))
    testImplementation("org.springframework.boot:spring-boot-starter-jooq")

    // 개발·테스트에서만 쓰는 런타임 의존성입니다. bootJar 에는 포함되지 않습니다.
    //   h2            : default 프로파일의 인메모리 RDB (운영은 MySQL)
    //   embedded-redis: EmbeddedRedisConfig, @Profile("!live")
    // 둘을 빼면 bootJar 에서 약 14MB 가 줄어듭니다.
    testAndDevelopmentOnly("com.h2database:h2")
    testAndDevelopmentOnly("com.github.codemonstur:embedded-redis:1.4.3")
}

// netty-codec-native-quic 은 플랫폼별 분류 아티팩트 5종(linux/osx/windows × arch)이
// 모두 해석되어 약 11MB 를 차지합니다. HTTP/3 경로에서만 쓰이는데, 이 경로는
// spring-ai → spring-boot-starter-webclient → reactor-netty-http 로 들어온 것이고
// OpenAiChatService 는 blocking .call() 만 사용합니다(.stream() 없음).
// Reactor Netty 의 HTTP/3 는 명시적으로 켜야 동작하므로 제외해도 쓰이지 않습니다.
configurations.runtimeClasspath {
    exclude(group = "io.netty", module = "netty-codec-native-quic")
}

// =============================================================================
// AOT 처리 시 활성 프로파일
// =============================================================================
// @Conditional 평가 결과는 AOT 시점에 확정되어 이미지에 박힙니다. 런타임에
// 환경변수로 바꿀 수 있는 것은 "값" 뿐이고, "빈의 존재 여부" 는 바꿀 수 없습니다.
//
// 프로파일을 지정하지 않으면 AOT 가 default 로 돌아 실제 배포 프로파일과 다른
// 판정을 내립니다. 실측 사례:
//   application.yml      management.tracing.export.enabled: false
//   application-live.yml                                  : true
// default 로 AOT 를 돌리면 TracingExporterConfig
// (@ConditionalOnProperty havingValue = "true") 가 이미지에서 빠지는데,
// 컨테이너는 SPRING_PROFILES_ACTIVE=live 로 뜨므로 속성은 true 인데 빈이 없는
// 상태가 됩니다. 트레이싱이 오류 없이 조용히 동작하지 않습니다.
//
// 반대로 프로파일별 "값"(접속 정보, 풀 크기 등)은 런타임 환경변수로 정상
// 적용됩니다. AOT 를 default 로 처리한 산출물을 live 로 띄워 MySQL 에 붙고
// 화면이 뜨는 것까지 확인했습니다. 즉 이미지를 프로파일마다 따로 만들 필요는
// 없고, AOT 판정이 배포 프로파일과 일치하게만 맞춰 주면 됩니다.
tasks.matching { it.name == "processAot" }.configureEach {
    (this as JavaExec).args("--spring.profiles.active=${findProperty("aotProfiles") ?: "live"}")
}

// =============================================================================
// GraalVM 네이티브 이미지
// =============================================================================
// 빌드:
//   ./gradlew :app:nativeCompile -PjavaToolchain=25     (GraalVM 25 필요)
//   docker build -f Dockerfile.native -t lifelog:native .
//
// 네이티브 빌드에서만 적용되는 설정이라 JVM 빌드에는 영향이 없습니다.
graalvmNative {
    binaries {
        named("main") {
            imageName.set("lifelog")

            buildArgs.addAll(
                // java.beans.Introspector 의 static 초기화가 빌드 시점에 돌면서
                // SharedSecrets.javaBeansAccess(빌드 시점 초기화 필드)에 Introspector$1
                // 인스턴스를 남깁니다. 그런데 Introspector$1 자체는 런타임 초기화 대상이라
                // "An object of type 'java.beans.Introspector$1' was found in the image heap"
                // 으로 분석이 중단됩니다. 둘의 초기화 시점을 맞춰 줍니다.
                // (Introspector$1 은 접근 shim 으로 상태가 없어 빌드 시점 초기화가 안전합니다)
                "--initialize-at-build-time=java.beans.Introspector$1",

                // 폴백(JVM 을 끼워 넣은 반쪽 이미지) 을 금지합니다. 이게 없으면 분석이
                // 실패해도 빌드가 "성공" 하고, 실행 시점에 JVM 이 필요해집니다.
                "--no-fallback",

                // 실패 원인을 추적할 수 있도록 스택트레이스를 남깁니다.
                "-H:+ReportExceptionStackTraces",

                // native-image 빌더 JVM 의 힙 상한. 컨테이너 메모리를 넘기면
                // OOM-kill 로 조용히 죽습니다.
                "-J-Xmx10g",
            )

            // -H:+RuntimeClassLoading 은 런타임 클래스 정의를 허용하지만 실험적이고
            // open world 를 함의합니다(closed-world 최적화 포기). JPA 를 걷어낸 뒤로는
            // 런타임에 클래스를 만드는 라이브러리가 없어 쓸 일이 없습니다.
            //
            // 2026-11-08 실측: 23분 39초 실행 후 빌더가 힙 9,777MB 를 모두 소진하고
            // watchdog 에 의해 중단됐습니다(exit status 30). open world 분석의
            // 메모리 요구가 closed world 보다 훨씬 큽니다. -J-Xmx 를 더 올려
            // 완주시킬 수는 있겠지만, 산출물이 실험적 구성이라 운영에 쓸 수 없고
            // 빌드 시간도 이미 closed world(13분 37초)의 두 배입니다.
            //
            // 기본값은 끄고, 다시 측정할 때만 켭니다.
            //   ./gradlew :app:nativeCompile -PjavaToolchain=25 -PnativeRuntimeClassLoading=true
            if (findProperty("nativeRuntimeClassLoading") == "true") {
                buildArgs.addAll(
                    "-H:+UnlockExperimentalVMOptions",
                    "-H:+RuntimeClassLoading",
                )
            }
            // AWT 는 GraalVM 25 가 기본 지원하므로 --initialize-at-run-time 을 주지
            // 않습니다. 필요한 것은 리플렉션 메타데이터이며
            // src/main/resources/META-INF/native-image/com.walter/lifelog-awt/ 에 있습니다.
        }
    }
}
