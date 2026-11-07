plugins {
    `java-library`
}

val commonmarkVersion = "0.24.0"
val mapstructVersion = "1.6.3"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.ai:spring-ai-openai:2.0.0-M3") {
        exclude(group = "net.javacrumbs.json-unit")
    }
    implementation("org.springframework.ai:spring-ai-starter-model-openai:2.0.0-M3") {
        exclude(group = "net.javacrumbs.json-unit")
    }
    api("org.springframework.boot:spring-boot-starter-data-redis")
    api("org.springframework.boot:spring-boot-starter-jooq")
    api("org.springframework:spring-aop")
    api("org.aspectj:aspectjweaver")
    // springdoc 2.x 는 Spring Boot 3 라인, 3.x 가 Boot 4 라인입니다.
    // 2.7.0 의 QuerydslPredicateOperationCustomizer 는 spring-data-commons 3 의
    // org.springframework.data.util.TypeInformation 을 참조하는데, Boot 4 가 쓰는
    // spring-data-commons 4 에는 그 클래스가 없습니다. 해당 빈이 실제로 생성되지는 않아
    // 평소 기동에는 드러나지 않지만, 빈 정의를 선행 조회하는 경로에서는 바로 깨집니다.
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
    api("org.mapstruct:mapstruct:$mapstructVersion")
    api("com.google.api-client:google-api-client:2.7.2")
    api("com.google.apis:google-api-services-drive:v3-rev20250220-2.0.0")
    api("com.github.codemonstur:embedded-redis:1.4.3")
    implementation("org.commonmark:commonmark:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-gfm-tables:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-gfm-strikethrough:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-autolink:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-heading-anchor:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-task-list-items:$commonmarkVersion")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    testImplementation("org.springframework.security:spring-security-crypto")
}
