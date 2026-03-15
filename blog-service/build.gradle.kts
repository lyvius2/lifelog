plugins {
    kotlin("kapt")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":shared"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.apache.commons:commons-lang3:3.20.0")
    kapt("org.mapstruct:mapstruct-processor:1.6.3")
}

kapt {
    arguments {
        arg("mapstruct.defaultComponentModel", "spring")
        arg("mapstruct.unmappedTargetPolicy", "IGNORE")
    }
}
