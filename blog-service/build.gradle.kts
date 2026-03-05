plugins {
    kotlin("kapt")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":user-service"))                                // TODO : 향후 의존성 제거
    implementation("org.springframework.boot:spring-boot-starter-webmvc")   // TODO : 향후 의존성 제거
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.apache.commons:commons-lang3:3.20.0")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")                       // TODO : 향후 의존성 제거
    kapt("org.mapstruct:mapstruct-processor:1.6.3")
}

kapt {
    arguments {
        arg("mapstruct.defaultComponentModel", "spring")
        arg("mapstruct.unmappedTargetPolicy", "IGNORE")
    }
}
