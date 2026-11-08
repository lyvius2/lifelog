plugins {
    kotlin("kapt")
}

dependencies {
    implementation(project(":shared"))
    // jOOQ 와 JDBC 는 shared 가 api 로 노출합니다 (spring-boot-starter-jooq).
    implementation("org.springframework.boot:spring-boot-starter-security")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")
    testImplementation("jakarta.servlet:jakarta.servlet-api:6.1.0")
    kapt("org.mapstruct:mapstruct-processor:1.6.3")
}

kapt {
    arguments {
        arg("mapstruct.defaultComponentModel", "spring")
        arg("mapstruct.unmappedTargetPolicy", "IGNORE")
    }
}
