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
