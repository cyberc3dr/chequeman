dependencies {
    api(libs.exposed.core)
    api(libs.exposed.jdbc)
    api(libs.exposed.dao)
    api(libs.exposed.json)
    api(libs.h2)
    api(libs.postgres)

    api(project(":models"))
}