package io.chequeman.server.http

import io.chequeman.models.ChequeUpdate
import io.chequeman.models.ChequeWriteRequest
import io.chequeman.server.workers.BotWorker
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/write")
class ChequeWriteController(
    private val template: SimpMessagingTemplate,
    private val botWorker: BotWorker
) {

    @PostMapping("/")
    fun write(@RequestBody request: ChequeWriteRequest) : ResponseEntity<String> {
        val response = botWorker.handleRequest(request)
            ?: return ResponseEntity.badRequest().body("Unknown bot")

        template.convertAndSend("/topic/cheques", ChequeUpdate(
            uuid = response.id!!
        ))

        return ResponseEntity.ok("Processed cheque")
    }
}