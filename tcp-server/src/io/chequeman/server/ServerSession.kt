package io.chequeman.server

import io.chequeman.protocol.*
import io.chequeman.server.database.TCPUser
import io.chequeman.server.database.TCPUsersTable
import io.chequeman.utils.decryptAes
import io.chequeman.utils.encrypt
import io.chequeman.utils.encryptAes
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import korlibs.crypto.SecureRandom
import korlibs.encoding.base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.security.PublicKey

class ServerSession(val socket: Socket) : CoroutineScope by ChequeServer.scope {

    private lateinit var rsaKey: PublicKey
    private val aesKey = SecureRandom.nextBytes(32)

    private val readChannel = socket.openReadChannel()
    private val writeChannel = socket.openWriteChannel(autoFlush = true)

    var loggedIn = false
    var authorized = false

    init {
        launch {
            while (true) try {
                handlePacket(readPacket())
            } catch (e: Throwable) {
                e.printStackTrace()
                break
            }
        }
    }

    suspend fun handlePacket(packet: ServerBoundPacket) {
        // Authorization

        when (packet) {
            is HandshakeC2S -> {
                if(loggedIn) return

                ChequeServer.logger.info("Received handshake from client: ${packet.rsaKey}")

                rsaKey = packet.rsaKey

                sendPacket(
                    HandshakeS2C(
                        aesKey = aesKey.base64
                    )
                )

                loggedIn = true

                return
            }

            is LoginC2S -> {
                if(!loggedIn || authorized) return

                ChequeServer.logger.info("Received login request from client: ${packet.clientName}")

                newSuspendedTransaction {
                    val user = TCPUser.find(TCPUsersTable.token eq packet.token)
                        .firstOrNull()

                    if(user == null) {
                        sendPacket(
                            LoginFailureS2C(
                                "Login failed: Invalid token"
                            )
                        )

                        socket.close()
                    } else {
                        this@ServerSession.authorized = true

                        sendPacket(
                            LoginSuccessS2C(
                                "Logged in as ${user.username} (ID: ${user.id})"
                            )
                        )
                    }
                }

                return
            }

            else -> {}
        }

        // Post auth

        if(!authorized) return
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sendPacket(packet: ClientBoundPacket) {
        var bytes = Cbor.encodeToByteArray<ClientBoundPacket>(packet)

        if (loggedIn) {
            bytes = bytes.encryptAes(aesKey)
        } else {
            bytes = rsaKey.encrypt(bytes)
        }

        writeChannel.writeInt(bytes.size)
        writeChannel.writeFully(bytes)
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun readPacket() : ServerBoundPacket {
        val dataLength = readChannel.readInt()

        var bytes = readChannel.readByteArray(dataLength)

        if(loggedIn) {
            bytes = bytes.decryptAes(aesKey)
        }

        return Cbor.decodeFromByteArray(bytes)
    }

}