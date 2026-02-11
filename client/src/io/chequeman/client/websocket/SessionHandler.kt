package io.chequeman.client.websocket

import io.chequeman.client.telegram.ChequeBot
import io.chequeman.models.ChequeUpdate
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandler
import org.springframework.stereotype.Component

@Component
class SessionHandler(
    private val bot: ChequeBot
) : StompSessionHandler {

    override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
        println("Connected to WebSocket server")

        session.subscribe("/topic/cheques", this)
    }

    override fun handleException(session: StompSession, command: StompCommand?, headers: StompHeaders, payload: ByteArray, exception: Throwable) {
        println("Exception: ${exception.message}")
    }

    override fun handleTransportError(session: StompSession, exception: Throwable) {
        println("Transport error: ${exception.message}")
    }

    override fun getPayloadType(headers: StompHeaders) = ChequeUpdate::class.java

    override fun handleFrame(headers: StompHeaders, payload: Any?) {
        bot.handleChequeUpdate(payload as ChequeUpdate)
    }
}