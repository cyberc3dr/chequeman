package io.chequeman.server.auth

import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64

@Component
class TokenUtil {

    private val random = SecureRandom()

    fun generateToken() : String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }
}