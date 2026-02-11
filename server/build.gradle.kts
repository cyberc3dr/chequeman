plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.kotlin)
    alias(libs.plugins.spring.jpa)
    alias(libs.plugins.graalvm)
}

dependencies {
    implementation(project(":models"))

    implementation(libs.spring.web)
    implementation(libs.spring.actuator)
    implementation(libs.spring.websocket)
    implementation(libs.spring.jpa)
    implementation(libs.spring.security)
    implementation(libs.spring.shell)

    implementation(libs.kotlin.reflect)

    implementation(libs.h2)
    implementation(libs.postgresql)
}

kotlin.compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
}