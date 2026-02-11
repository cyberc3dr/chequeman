package io.chequeman.protocol

import kotlinx.serialization.Serializable

@Serializable
sealed interface Packet

@Serializable
sealed interface ClientBoundPacket : Packet

@Serializable
sealed interface ServerBoundPacket : Packet