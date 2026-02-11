@file:OptIn(ExperimentalUuidApi::class)

package io.chequeman.protocol

import io.chequeman.models.ChequeEntry
import io.chequeman.models.ChequeWriteRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// Serverbound packets

@Serializable
@SerialName("cheque_upload_c2s")
data class ChequeUploadC2S(
    val chequeWriteRequest: ChequeWriteRequest
) : ServerBoundPacket

@Serializable
@SerialName("cheque_request_c2s")
data class ChequeRequestC2S(
    val searchTerm: String
) : ServerBoundPacket

// Clientbound packets

@Serializable
@SerialName("cheque_update_s2c")
data class ChequeUpdateS2C(
    val uuid: Uuid
) : ClientBoundPacket

@Serializable
@SerialName("cheque_search_success_s2c")
data class ChequeSearchSuccessS2C(
    val cheque: ChequeEntry
) : ClientBoundPacket

@Serializable
@SerialName("cheque_search_failure_s2c")
data class ChequeSearchFailureS2C(
    val message: String
) : ClientBoundPacket