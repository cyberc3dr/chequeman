package io.chequeman.server.workers

import io.chequeman.extensions.containsIgnoreCase
import io.chequeman.models.ChequeWriteRequest
import io.chequeman.server.Cheque
import io.chequeman.server.ChequeRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class BotWorker(
    private val chequeRepository: ChequeRepository,
) {

    private val bots = setOf(
        CryptoBot, XRocketBot
    )

    @Scheduled(cron = "0 0 0 * * 0")
    fun cleanTable() {
        chequeRepository.deleteAll()
    }

    fun getCheques() : List<Cheque> {
        return chequeRepository.findAll()
    }

    fun getCheques(botAlias: String) : List<Cheque>? {
        val bot = bots.firstOrNull { it.aliases.containsIgnoreCase(botAlias) }
            ?: return null

        return chequeRepository.findAll()
            .filter { it.bot.equals(bot.aliases.first(), ignoreCase = true) }
    }

    fun handleRequest(request: ChequeWriteRequest) : Cheque? {
        val bot = bots.firstOrNull { it.aliases.containsIgnoreCase(request.bot) }
            ?: return null

        val chequeEntry = bot.handleRequest(request)
        val cheque = chequeEntry.cheque

        val existing = if(chequeEntry.cheque == null) {
            chequeRepository.findAll()
                .firstOrNull { chequeEntry.id in it.ids }
        } else {
            chequeRepository.findAll()
                .firstOrNull { it.cheque == chequeEntry.cheque }
        }

        if(existing != null) {
            val mergedCheque = when {
                cheque == null -> existing.cheque
                existing.cheque == null -> cheque
                else -> existing.cheque.safeCopyFrom(cheque)
            }

            return chequeRepository.save(
                existing.copy(
                    ids = (existing.ids + chequeEntry.id).distinct(),
                    sources = (existing.sources + chequeEntry.source).distinct(),
                    cheque = mergedCheque
                )
            )
        } else {
            return chequeRepository.save(Cheque(
                bot = request.bot,
                ids = listOf(chequeEntry.id),
                sources = listOf(chequeEntry.source),
                cheque = cheque
            ))
        }
    }
}