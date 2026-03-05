plugins {
    kotlin("kapt")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":shared"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")       // TODO : 향후 의존성 제거
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    kapt("org.mapstruct:mapstruct-processor:1.6.3")
}

kapt {
    arguments {
        arg("mapstruct.defaultComponentModel", "spring")
        arg("mapstruct.unmappedTargetPolicy", "IGNORE")
    }
}
