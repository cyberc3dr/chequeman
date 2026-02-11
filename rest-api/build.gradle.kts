plugins {
    alias(libs.plugins.ktor)
}

application.mainClass = "io.chequeman.http.ApplicationKt"

dependencies {
    api(libs.ktor.server.core)
    api(libs.ktor.server.netty)

    implementation(libs.ktor.server.auth)

    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)

    implementation(libs.logback.classic)

    api(project(":workers"))
}