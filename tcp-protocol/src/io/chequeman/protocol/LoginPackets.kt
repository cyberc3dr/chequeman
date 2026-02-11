@file:UseSerializers(PublicKeySerializer::class)

package io.chequeman.protocol

import io.chequeman.utils.PublicKeySerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.security.PublicKey

// Serverbound packets

@Serializable
@SerialName("handshake_c2s")
data class HandshakeC2S (
    val rsaKey: PublicKey
) : ServerBoundPacket

@Serializable
@SerialName("login_c2s")
data class LoginC2S (
    val clientName: String,
    val token: String
) : ServerBoundPacket

// Clientbound packets

@Serializable
@SerialName("handshake_s2c")
data class HandshakeS2C (
    val aesKey: String
) : ClientBoundPacket

@Serializable
@SerialName("login_success_s2c")
data class LoginSuccessS2C (
    val message: String,
) : ClientBoundPacket

@Serializable
@SerialName("login_failure_s2c")
data class LoginFailureS2C (
    val message: String
) : ClientBoundPacket


