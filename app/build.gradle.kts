tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
}

tasks.named<Jar>("jar") {
    enabled = false
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
