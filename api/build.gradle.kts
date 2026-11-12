dependencies {
    implementation(project(":shared"))
    implementation(project(":user-service"))
    implementation(project(":content-service"))
    implementation(project(":blog-service"))
    implementation(project(":photo-archive-service"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // 요청·응답 DTO 의 JSON 이름을 런타임과 같은 매퍼(Jackson 3 + Kotlin 모듈)로 검증합니다.
    testImplementation("tools.jackson.module:jackson-module-kotlin")
}
