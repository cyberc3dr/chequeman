package io.chequeman.client.websocket

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.stereotype.Component
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import java.util.concurrent.CountDownLatch

@Component
class WebSocketClient(
    private val handler: SessionHandler
) {

    @Value($$"${websocket.url}")
    lateinit var webSocketUrl: String

    @Value($$"${websocket.token}")
    lateinit var webSocketToken: String

    @PostConstruct
    fun connect() {
        println("Connecting to WebSocket server at $webSocketUrl")

        val client = StandardWebSocketClient()
        val stompClient = WebSocketStompClient(client)

        stompClient.messageConverter = MappingJackson2MessageConverter()

        val headers = StompHeaders().apply {
            set("Authorization", "Bearer $webSocketToken")
        }

        stompClient.connectAsync(
            webSocketUrl,
            null,
            headers,
            handler
        )
    }

}