plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.spring") version "2.2.21" apply false
    kotlin("kapt") version "2.2.21" apply false
    id("org.springframework.boot") version "4.0.2" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("plugin.jpa") version "2.2.21" apply false
    // Spring Boot 4.0 의 네이티브 이미지는 GraalVM 25 이상을 요구하고,
    // Native Build Tools 는 0.11.x 라인을 씁니다.
    id("org.graalvm.buildtools.native") version "0.11.5" apply false
}

// 평소 빌드는 Java 21 로 유지하고, 네이티브 빌드만 GraalVM 25 툴체인으로 돌립니다.
//   ./gradlew :app:nativeCompile -PjavaToolchain=25
// 21 로 고정해 두면 native-image 가 25 를 요구해 빌드가 서지 않습니다.
val javaToolchainVersion = (findProperty("javaToolchain") as String? ?: "21").toInt()

group = "com.walter"
version = "0.0.1-SNAPSHOT"
description = "lifelog"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    group = rootProject.group
    version = rootProject.version

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaToolchainVersion))
        }
    }

    // 툴체인(컴파일을 수행하는 JDK)만 바뀌고 바이트코드 타깃은 항상 21 입니다.
    // Kotlin 의 jvmTarget 이 21 로 고정되어 있어, Java 쪽도 release=21 로 묶지 않으면
    // Gradle 9 가 "Inconsistent JVM-target compatibility" 로 빌드를 거부합니다.
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(21)
    }

    repositories {
        mavenCentral()
    }

    tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
        enabled = false
    }

    tasks.named<Jar>("jar") {
        enabled = true
    }

    dependencies {
        "implementation"("org.jetbrains.kotlin:kotlin-reflect")
        "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
        "testImplementation"("org.jetbrains.kotlin:kotlin-test-junit5")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("io.mockk:mockk:1.13.16")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
