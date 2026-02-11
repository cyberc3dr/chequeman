package io.chequeman.http

import io.chequeman.event.Event
import io.chequeman.event.EventManager
import io.ktor.utils.io.core.*
import korlibs.crypto.SecureRandom
import korlibs.encoding.base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

data class CommandExecutionEvent(
    val label: String,
    val args: List<String>
) : Event

object CommandManager : CoroutineScope {
    override val coroutineContext = Dispatchers.IO

    suspend fun emitCommand(command: CommandExecutionEvent) {
        // EventManager.callEvent(command)
    }

    private suspend fun handleCommand(execution: CommandExecutionEvent) = when (execution.label) {
        "help" -> {
            logger.info("Available commands: help, http, end")
        }
        "end" -> {
            exitProcess(0)
        }
        "http" -> {
            val subcommand = execution.args.firstOrNull()?.lowercase() ?:
                throw IllegalArgumentException("Usage: http <create|info|delete|reset> <username>")

            val username = execution.args.getOrNull(1) ?:
                throw IllegalArgumentException("Usage: http <create|info|delete|reset> <username>")

            when (subcommand) {
                "create" -> newSuspendedTransaction {
                    val user = HttpUser.find(HttpUsersTable.username eq username)
                        .firstOrNull()

                    if (user != null) {
                        logger.info("User with username '$username' already exists.")
                        return@newSuspendedTransaction
                    }

                    val token = username.generateToken()

                    HttpUser.new {
                        this.username = username
                        this.token = token
                    }

                    logger.info("HTTP user created with username '$username' and token '$token'.")
                }
                "delete" -> newSuspendedTransaction {
                    val user = HttpUser.find(HttpUsersTable.username eq username)
                        .firstOrNull()

                    if (user == null) {
                        logger.info("User with username '$username' does not exist.")
                        return@newSuspendedTransaction
                    }

                    user.delete()
                    logger.info("HTTP user with username '$username' deleted.")
                }
                "info" -> newSuspendedTransaction {
                    val user = HttpUser.find(HttpUsersTable.username eq username)
                        .firstOrNull()

                    if (user == null) {
                        logger.info("User with username '$username' does not exist.")
                    } else {
                        logger.info("HTTP user info:")
                        logger.info("Username: ${user.username}")
                        logger.info("Token: ${user.token}")
                    }
                }
                "reset" -> newSuspendedTransaction {
                    val user = HttpUser.find(HttpUsersTable.username eq username)
                        .firstOrNull()

                    if (user == null) {
                        logger.info("User with username '$username' does not exist.")
                        return@newSuspendedTransaction
                    }

                    val newToken = username.generateToken()
                    user.token = newToken

                    logger.info("HTTP user token reset for username '$username'. New token: $newToken")
                }
                else -> {
                    logger.info("Unknown subcommand: $subcommand")
                }
            }
        }
        else -> emitCommand(execution)
    }

    fun startCommandHandler() {
        val scanner = Scanner(System.`in`)

        while(scanner.hasNextLine()) {
            val command = scanner.nextLine().trim()

            val label = command.split(" ").firstOrNull()?.lowercase() ?: continue
            val args = command.split(" ").drop(1)

            launch {
                runCatching {
                    handleCommand(CommandExecutionEvent(label, args))
                }.onFailure {
                    logger.info(it.message)
                }
            }
        }
    }
}

fun String.generateToken() = Buffer().let {
    it.writeText(this)
    it.write(SecureRandom.nextBytes(64))
    it.readByteArray()
}.base64