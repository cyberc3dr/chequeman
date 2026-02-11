package io.chequeman.server.websocket

import io.chequeman.server.auth.UserRepository
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.stereotype.Component

@Component
class TokenChannelInterceptor(
    private val userRepository: UserRepository
): ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

        if(accessor != null && StompCommand.CONNECT == accessor.command) {
            val authHeader = accessor.getFirstNativeHeader("Authorization")

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                val token = authHeader.substring(7)

                val user = userRepository.findByToken(token)
                    ?: throw SecurityException("Token invalid")

                accessor.user = user

            } else {
                throw SecurityException("Missing or invalid Authorization header")
            }
        }

        return message
    }
}