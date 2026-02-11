package io.chequeman.http

import io.chequeman.bots.Bot
import io.chequeman.extensions.containsIgnoreCase
import io.chequeman.models.ChequeWriteRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        get("api/cheques") {
            val cheques = Bot.getCheques()

            call.respond(cheques)
        }
        get("api/cheques/{chequeId}") {
            val chequeId = call.parameters["chequeId"] ?: return@get call.respondText("Cheque ID not specified", status = io.ktor.http.HttpStatusCode.BadRequest)

            val cheque = Bot.getCheque(chequeId) ?: return@get call.respondText(
                "Cheque not found",
                status = HttpStatusCode.NotFound
            )

            call.respond(cheque)
        }
        get("/api/{bot}/cheques") {
            val bot = call.parameters["bot"] ?: return@get call.respondText("Bot not specified", status = io.ktor.http.HttpStatusCode.BadRequest)

            val instance = Bot.bots.firstOrNull { it.aliases.containsIgnoreCase(bot) }
                ?: return@get call.respondText("Bot not found", status = HttpStatusCode.NotFound)

            val cheques = instance.getCheques()

            call.respond(cheques)
        }
        get("/api/{bot}/cheques/{chequeId}") {
            val bot = call.parameters["bot"] ?: return@get call.respondText("Bot not specified", status = io.ktor.http.HttpStatusCode.BadRequest)
            val chequeId = call.parameters["chequeId"] ?: return@get call.respondText("Cheque ID not specified", status = io.ktor.http.HttpStatusCode.BadRequest)

            val instance = Bot.bots.firstOrNull { it.aliases.containsIgnoreCase(bot) }
                ?: return@get call.respondText("Bot not found", status = HttpStatusCode.NotFound)

            val cheque = instance.getCheque(chequeId) ?: return@get call.respondText(
                "Cheque not found",
                status = HttpStatusCode.NotFound
            )

            call.respond(cheque)
        }

        authenticate("auth-bearer") {
            post("/api/writeChequeRaw") {
                val request = call.receive<ChequeWriteRequest>()

                logger.info("Received cheque write request: $request")

                if(Bot.handleWriteRequest(request)) {
                    call.respond(HttpStatusCode.Created)
                } else {
                    call.respond(HttpStatusCode.Conflict, "Cheque already exists or is invalid.")
                }
            }
        }
    }
}