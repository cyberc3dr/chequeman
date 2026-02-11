package io.chequeman.client

import io.chequeman.extensions.PrettyYaml
import io.chequeman.protocol.*
import io.chequeman.utils.decrypt
import io.chequeman.utils.decryptAes
import io.chequeman.utils.encryptAes
import io.chequeman.utils.generateKeyPair
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import korlibs.encoding.fromBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.*
import kotlinx.serialization.cbor.Cbor
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.uuid.ExperimentalUuidApi

object ChequeClient {

    private val logger = LoggerFactory.getLogger("ChequeClient")

    private val selectorManager = ActorSelectorManager(Dispatchers.IO)
    private lateinit var socket: Socket

    private val readChannel by lazy { socket.openReadChannel() }
    private val writeChannel by lazy { socket.openWriteChannel(autoFlush = true) }

    private val rsaKeyPair = generateKeyPair()
    private lateinit var aesKey: ByteArray

    private var loggedIn = false
    private var authorized = false

    @Serializable
    data class ClientConfig(
        val hostname: String = "127.0.0.1",
        val port: Int = 58818,
        val token: String = "<INPUT_YOUR_TOKEN_HERE>"
    )

    private val config = File("client.yml").run {
        if (createNewFile()) {
            writeText(PrettyYaml().encodeToString(ClientConfig()))
        }

        PrettyYaml().decodeFromString<ClientConfig>(readText())
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        socket = aSocket(selectorManager).tcp().connect(config.hostname, config.port)

        sendPacket(HandshakeC2S(
            rsaKey = rsaKeyPair.public
        ))

        while (true) {
            try {
                handlePacket(readPacket())
            } catch (e: Throwable) {
                e.printStackTrace()
                break
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun handlePacket(packet: ClientBoundPacket) = when (packet) {
        is HandshakeS2C -> {
            logger.info("Received handshake from server, AES key: ${packet.aesKey}")

            loggedIn = true

            aesKey = packet.aesKey.fromBase64()

            sendPacket(
                LoginC2S(
                    clientName = "ChequeClient",
                    token = config.token
                )
            )
        }

        is LoginSuccessS2C -> {
            logger.info("Login successful: ${packet.message}")

            authorized = true
        }

        is LoginFailureS2C -> {
            logger.info("Login failed: ${packet.message}")

            socket.close()
        }

        is ChequeUpdateS2C -> {
            logger.info("Received cheque update: ${packet.uuid}")
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun sendPacket(packet: ServerBoundPacket) {
        var bytes = Cbor.encodeToByteArray<ServerBoundPacket>(packet)

        if(loggedIn) {
            bytes = bytes.encryptAes(aesKey)
        }

        writeChannel.writeInt(bytes.size)
        writeChannel.writeFully(bytes)
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun readPacket(): ClientBoundPacket {
        val dataLength = readChannel.readInt()

        var bytes = readChannel.readByteArray(dataLength)

        bytes = if(loggedIn) {
            bytes.decryptAes(aesKey)
        } else {
            rsaKeyPair.private.decrypt(bytes)
        }

        return Cbor.decodeFromByteArray(bytes)
    }
}