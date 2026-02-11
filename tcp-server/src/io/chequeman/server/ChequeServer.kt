package io.chequeman.server

import io.chequeman.bots.Bot
import io.chequeman.database.DatabaseUtils
import io.chequeman.extensions.PrettyYaml
import io.chequeman.server.database.TCPUsersTable
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.io.path.Path

object ChequeServer {

    private val connections = mutableListOf<ServerSession>()

    lateinit var scope : CoroutineScope
    val logger: Logger = LoggerFactory.getLogger("ChequeServer")

    @Serializable
    data class ServerConfig(
        val port: Int = 58818,
        val database: DatabaseUtils.DatabaseConfig = DatabaseUtils.DatabaseConfig()
    )

    private val config = File("server.yml").run {
        if (createNewFile()) {
            writeText(PrettyYaml().encodeToString(ServerConfig()))
        }

        PrettyYaml().decodeFromString<ServerConfig>(readText())
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking(Dispatchers.IO) {
        scope = this

        val selectorManager = ActorSelectorManager(coroutineContext)

        configureDatabases()

        logger.info("Initializing TCP server...")

        val serverSocket = aSocket(selectorManager).tcp().bind(port = config.port)

        logger.info("Cheque server started on port ${config.port}")

        newSuspendedTransaction {
            SchemaUtils.create(TCPUsersTable)
        }

        CommandHandler.start()

        while (true) {
            connections += ServerSession(
                serverSocket.accept()
            )
        }
    }

    private suspend fun configureDatabases() {
        DatabaseUtils.initDatabase(Path("").toAbsolutePath().toFile(), config = config.database)
        Bot.loadAllDatabases()
    }
}