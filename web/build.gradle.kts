dependencies {
    implementation(project(":shared"))
    implementation(project(":user-service"))
    implementation(project(":content-service"))
    implementation(project(":blog-service"))
    implementation(project(":photo-archive-service"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect")
    implementation("org.springframework.boot:spring-boot-starter-security")
}
