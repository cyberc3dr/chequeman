plugins {
    alias(libs.plugins.ktor)
}

application.mainClass = "io.chequeman.client.ChequeClient"

dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)

    implementation(project(":models"))
    implementation(project(":tcp-protocol"))

    implementation(libs.logback.classic)
}