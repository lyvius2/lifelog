plugins {
    `java-library`
}

val commonmarkVersion = "0.24.0"
val mapstructVersion = "1.6.3"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-aop")
    implementation("org.aspectj:aspectjweaver")
    api("org.springframework.boot:spring-boot-starter-data-redis")
    api("org.springframework.boot:spring-boot-starter-jooq")
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
    api("org.mapstruct:mapstruct:$mapstructVersion")
    api("com.google.api-client:google-api-client:2.7.2")
    api("com.google.apis:google-api-services-drive:v3-rev20250220-2.0.0")
    implementation("org.commonmark:commonmark:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-gfm-tables:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-gfm-strikethrough:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-autolink:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-heading-anchor:$commonmarkVersion")
    implementation("org.commonmark:commonmark-ext-task-list-items:$commonmarkVersion")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    api("org.springframework.boot:spring-boot-starter-test")
    api("io.mockk:mockk:1.13.16")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-crypto")
}
