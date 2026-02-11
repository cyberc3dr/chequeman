plugins {
    alias(libs.plugins.ktor)
}

application.mainClass = "io.chequeman.server.ChequeServer"

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)

    implementation(libs.logback.classic)

    api(project(":tcp-protocol"))
    api(project(":workers"))
}