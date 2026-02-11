import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.kotlin)
    alias(libs.plugins.spring.jpa)
    alias(libs.plugins.graalvm)
}

dependencies {
    implementation(project(":models"))

    implementation(libs.spring.websocket)
    implementation(libs.spring.jpa)

    implementation(libs.telegram.client)
    implementation(libs.telegram.spring)

    implementation(libs.commonmark)

    implementation(libs.kotlin.reflect)

    implementation(libs.h2)
    implementation(libs.postgresql)
}

kotlin.compilerOptions {
    freeCompilerArgs.addAll(
        "-Xjsr305=strict",
        "-Xannotation-default-target=param-property"
    )
}