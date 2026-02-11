package io.chequeman.utils

import korlibs.encoding.base64
import korlibs.encoding.fromBase64
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.security.PublicKey

object PublicKeySerializer : KSerializer<PublicKey> {

    override val descriptor = PrimitiveSerialDescriptor("PublicKey", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PublicKey) {
        encoder.encodeString(value.encoded.base64)
    }

    override fun deserialize(decoder: Decoder): PublicKey {
        return decoder.decodeString().fromBase64().toPublicKey()
    }
}