package io.chequeman.http

import io.chequeman.database.DatabaseUtils
import io.chequeman.extensions.PrettyYaml
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory
import java.io.File
import java.util.logging.Logger

val logger = LoggerFactory.getLogger("HTTP Server")
val serverLoaded = CompletableDeferred<Unit>()

val postHooks = mutableListOf<() -> Unit>()

@Serializable
data class AppConfig(
    val port: Int = 56560,
    val host: String = "0.0.0.0",
    val database: DatabaseUtils.DatabaseConfig = DatabaseUtils.DatabaseConfig()
)

val config = File("config.yml").run {
    if(createNewFile()) {
        writeText(PrettyYaml().encodeToString(AppConfig()))
    }

    PrettyYaml().decodeFromString<AppConfig>(readText())
}

fun main() {
    embeddedServer(Netty, port = config.port, host = config.host, module = Application::module)
        .start(wait = true)
}

suspend fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    configureDatabase()
    configureAuthorization()

    configureRouting()

    monitor.subscribe(ApplicationStarted) {
        postHooks.forEach { it() }

        CommandManager.startCommandHandler()
    }
}