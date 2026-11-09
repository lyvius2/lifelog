plugins {
    id("org.graalvm.buildtools.native")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
}

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
    runtimeOnly("com.mysql:mysql-connector-j:8.2.0")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("io.opentelemetry:opentelemetry-exporter-otlp")
    testImplementation(project(":blog-service"))
    testImplementation(project(":photo-archive-service"))
    testImplementation("org.springframework.boot:spring-boot-starter-jooq")
    testAndDevelopmentOnly("com.h2database:h2")
}

configurations.runtimeClasspath {
    exclude(group = "io.netty", module = "netty-codec-native-quic")
}

tasks.matching { it.name == "processAot" }.configureEach {
    (this as JavaExec).args("--spring.profiles.active=${findProperty("aotProfiles") ?: "live"}")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("lifelog")

            buildArgs.addAll(
                "--initialize-at-build-time=java.beans.Introspector$1",
                "--no-fallback",
                // 네이티브 이미지는 기본적으로 빌드 시점 기본 문자셋 주변만 포함합니다.
                // MySQL 드라이버가 커넥션 문자셋을 고를 때 빠진 문자셋이 있으면
                // 서버에 알린 문자셋과 실제로 보내는 바이트가 어긋납니다.
                "-H:+AddAllCharsets",
                "-H:+ReportExceptionStackTraces",
                "-J-Xmx10g",
            )

            if (findProperty("nativeRuntimeClassLoading") == "true") {
                buildArgs.addAll(
                    "-H:+UnlockExperimentalVMOptions",
                    "-H:+RuntimeClassLoading",
                )
            }
        }
    }
}
