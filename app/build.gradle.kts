tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
}

tasks.named<Jar>("jar") {
    enabled = false
}

dependencies {
    implementation(project(":web"))
    implementation(project(":api"))
    implementation(project(":user-service"))
    implementation(project(":content-service"))
    implementation(project(":blog-service"))
    implementation(project(":photo-archive-service"))
    implementation(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter")

    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j:8.2.0")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
}
