package io.chequeman.server

import io.chequeman.server.ChequeServer.logger
import io.chequeman.server.database.TCPUser
import io.chequeman.server.database.TCPUsersTable
import io.ktor.utils.io.core.writeText
import korlibs.crypto.SecureRandom
import korlibs.encoding.base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.Scanner

object CommandHandler : CoroutineScope by ChequeServer.scope {

    fun String.generateToken() = Buffer().let {
        it.writeText(this)
        it.write(SecureRandom.nextBytes(64))
        it.readByteArray()
    }.base64

    fun start() = launch(Dispatchers.Main) {
        val scanner = Scanner(System.`in`)

        val command = scanner.nextLine().trim()

        while(scanner.hasNextLine()) {
            val label = command.split(" ").firstOrNull()?.lowercase() ?: continue
            val args = command.split(" ").drop(1)

            when (label) {
                "tcp" -> {
                    val subcommand = args.firstOrNull()?.lowercase()
                        ?: run {
                            logger.info("Usage: tcp <create|info|delete|reset> <username>")
                            return@launch
                        }

                    val username = args.getOrNull(1)
                        ?: run {
                            logger.info("Usage: tcp <create|info|delete|reset> <username>")
                            return@launch
                        }

                    when (subcommand) {
                        "create" -> newSuspendedTransaction {
                            val user = TCPUser.find(TCPUsersTable.username eq username)
                                .firstOrNull()

                            if (user != null) {
                                logger.info("User with username '$username' already exists.")
                                return@newSuspendedTransaction
                            }

                            val token = username.generateToken()

                            TCPUser.new {
                                this.username = username
                                this.token = token
                            }

                            logger.info("TCP user created with username '$username' and token '$token'.")
                        }

                        "delete" -> newSuspendedTransaction {
                            val user = TCPUser.find(TCPUsersTable.username eq username)
                                .firstOrNull()

                            if (user == null) {
                                logger.info("User with username '$username' does not exist.")
                                return@newSuspendedTransaction
                            }

                            user.delete()
                            println("TCP user with username '$username' deleted.")
                        }

                        "info" -> newSuspendedTransaction {
                            val user = TCPUser.find(TCPUsersTable.username eq username)
                                .firstOrNull()

                            if (user == null) {
                                logger.info("User with username '$username' does not exist.")
                            } else {
                                logger.info("TCP user info:")
                                logger.info("Username: ${user.username}")
                                logger.info("Token: ${user.token}")
                            }
                        }

                        "reset" -> newSuspendedTransaction {
                            val user = TCPUser.find(TCPUsersTable.username eq username)
                                .firstOrNull()

                            if (user == null) {
                                logger.info("User with username '$username' does not exist.")
                                return@newSuspendedTransaction
                            }

                            val newToken = username.generateToken()
                            user.token = newToken

                            logger.info("TCP user token reset for username '$username'. New token: $newToken")
                        }

                        else -> {
                            logger.info("Unknown subcommand: $subcommand")
                        }
                    }
                }

                else -> {}
            }
        }
    }
}