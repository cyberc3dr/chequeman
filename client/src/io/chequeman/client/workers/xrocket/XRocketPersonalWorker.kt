package io.chequeman.client.workers.xrocket

import io.chequeman.client.workers.ChequeMessage
import io.chequeman.client.workers.ChequeWorker
import io.chequeman.client.workers.plainString
import io.chequeman.models.ChequeModel
import io.chequeman.models.XRocketPersonal

object XRocketPersonalWorker : ChequeWorker {

    override fun parse(raw: ChequeModel): ChequeMessage? {
        val cheque = raw.cheque as? XRocketPersonal ?: return null

        val chequeType = "Персональный чек"

        val amount = cheque.oneActivation.plainString
        val currency = cheque.currency

        val hashtags = buildList {
            add("#XRocket")
            add("#Персональный")
            add("$$currency")
        }.joinToString(" ")

        val text = buildString {
            append("\uD83D\uDE80 **$chequeType** на **$amount $currency**\n")
            append("\n$hashtags")
        }

        val buttonText = "\uD83D\uDE80 Получить $amount $currency"
        val id = raw.ids.first()

        val link = if(cheque.old) {
            "https://t.me/xrocket?start=$id"
        } else {
            "https://t.me/xrocket/app?startapp=$id"
        }
        val source = raw.sources.first()

        return ChequeMessage(
            text = text,
            buttonText = buttonText,
            link = link,
            source = source
        )
    }
}

