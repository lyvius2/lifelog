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

    // 개발·테스트에서만 쓰는 런타임 의존성입니다. bootJar 에는 포함되지 않습니다.
    //   h2            : default 프로파일의 인메모리 RDB (운영은 MySQL)
    //   embedded-redis: EmbeddedRedisConfig, @Profile("!live")
    // 둘을 빼면 bootJar 에서 약 14MB 가 줄어듭니다.
    // Hibernate 가 네이티브 이미지용으로 따로 내는 보조 아티팩트입니다(8KB).
    // GraalVMStaticFeature 가 persister / naming strategy / id generator 등의
    // 리플렉션을 등록합니다. 네이티브 빌드에만 쓰이지만 Feature 로 동작하려면
    // 런타임 클래스패스에 있어야 합니다.
    implementation("org.hibernate.orm:hibernate-graalvm:7.2.1.Final")

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
                // Groovy 가 생성하는 groovyjarjarasm.asm 클래스는 빌드 시점 초기화가
                // 필요합니다. 런타임 초기화로 남겨 두면 초기화 충돌로 실패합니다.
                "--initialize-at-build-time=groovyjarjarasm.asm",

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
            // AWT 는 GraalVM 25 가 기본 지원하므로 --initialize-at-run-time 을 주지
            // 않습니다. 필요한 것은 리플렉션 메타데이터이며
            // src/main/resources/META-INF/native-image/com.walter/lifelog-awt/ 에 있습니다.
        }
    }
}

// Groovy 5 는 GraalVM 의 치환(substitution)과 맞지 않습니다
// (v7.IndyInterface 에 invalidateSwitchPoints() 가 없음).
// Groovy 는 thymeleaf-layout-dialect 때문에만 들어오고, 그 라이브러리가 실제로 요청하는
// 버전도 4.0.x 입니다. Boot 4 BOM 이 5.0.3 으로 올리는 것을 네이티브 빌드에서만 되돌립니다.
// JVM 빌드의 의존성 해석은 건드리지 않습니다.
if (findProperty("javaToolchain") == "25") {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.apache.groovy") {
                useVersion("4.0.28")
                because("Groovy 5 는 GraalVM 네이티브 이미지 빌드에서 실패합니다")
            }
        }
    }
}
