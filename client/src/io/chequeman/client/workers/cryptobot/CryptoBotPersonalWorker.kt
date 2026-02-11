package io.chequeman.client.workers.cryptobot

import io.chequeman.client.workers.ChequeMessage
import io.chequeman.client.workers.ChequeWorker
import io.chequeman.client.workers.plainString
import io.chequeman.models.ChequeModel
import io.chequeman.models.CryptoBotPersonal

object CryptoBotPersonalWorker : ChequeWorker {
    override fun parse(raw: ChequeModel): ChequeMessage? {
        val cheque = raw.cheque as? CryptoBotPersonal ?: return null

        val chequeType = "Персональный чек"
        val amount = cheque.oneActivation.plainString
        val currency = cheque.currency
        val receiver = cheque.receiver
        val hashtags = "#CryptoBot #Персональный $$currency"

        val text = buildString {
            append("\uD83D\uDC8E **$chequeType** на **$amount $currency**\n")
            if(receiver != null) {
                append("\nПолучатель: @$receiver\n")
            }
            append("\n$hashtags")
        }

        val buttonText = "\uD83D\uDC8E Получить $amount $currency"
        val link = "http://t.me/send?start=${raw.ids.first()}"
        val source = raw.sources.first()

        return ChequeMessage(
            text = text,
            buttonText = buttonText,
            link = link,
            source = source
        )
    }
}
