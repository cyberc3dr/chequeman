package io.chequeman.server.http

import io.chequeman.server.Cheque
import io.chequeman.server.workers.BotWorker
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/read")
class ChequeReadController(
    private val botWorker: BotWorker
) {

    @GetMapping("/")
    fun index() = "Hello world!"

    @GetMapping("/cheques")
    fun readAll() : ResponseEntity<List<Cheque>> {
        val cheques = botWorker.getCheques()
        return ResponseEntity.ok(cheques)
    }

    @GetMapping("/cheques/{chequeId}")
    fun read(@PathVariable chequeId: String) : ResponseEntity<Cheque> {
        val cheques = botWorker.getCheques()
        val cheque = cheques.firstOrNull { it.ids.contains(chequeId) || it.id.toString() == chequeId }
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(cheque)
    }

    @GetMapping("/{bot}/cheques")
    fun readBotAll(@PathVariable bot: String) : ResponseEntity<List<Cheque>> {
        val cheques = botWorker.getCheques(bot)
            ?: return ResponseEntity.badRequest().body(emptyList())

        return ResponseEntity.ok(cheques)
    }

    @GetMapping("/{bot}/cheques/{chequeId}")
    fun readBot(
        @PathVariable bot: String,
        @PathVariable chequeId: String
    ) : ResponseEntity<Cheque> {
        val cheques = botWorker.getCheques(bot)
            ?: return ResponseEntity.badRequest().build()

        val cheque = cheques.firstOrNull { it.ids.contains(chequeId) || it.id.toString() == chequeId }
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(cheque)
    }
}