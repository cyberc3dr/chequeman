package io.chequeman.client.workers.cryptobot

import io.chequeman.client.workers.ChequeMessage
import io.chequeman.client.workers.ChequeWorker
import io.chequeman.client.workers.plainString
import io.chequeman.models.ChequeModel
import io.chequeman.models.CryptoBotMC

object CryptoBotMCWorker : ChequeWorker {
    override fun parse(raw: ChequeModel): ChequeMessage? {
        val cheque = raw.cheque as? CryptoBotMC ?: return null

        val chequeType = "Мультичек"

        val icons = buildString {
            if (cheque.passwordProtected) append("🔒 ")
            if (cheque.isPremium) append("⭐ ")
            if (cheque.requireNewUser) append("🆕 ")
        }.trim()

        val description = cheque.description?.takeIf { it.isNotBlank() }
        val amount = cheque.oneActivation.plainString
        val currency = cheque.currency
        val total = cheque.totalAmount.plainString
        val activations = cheque.activations
        val picture = cheque.picture

        val activated = cheque.activated

        val hashtags = buildList {
            add("#CryptoBot")
            add("#Мультичек")
            add("$$currency")
            add(if (cheque.isPremium) "#Premium" else "#БезPremium")
            add(if (cheque.passwordProtected) "#Пароль" else "#БезПароля")
            add(if (cheque.requireNewUser) "#ТолькоНовые" else "#ДляВсех")
        }.joinToString(" ")

        val text = buildString {
            if(picture != null) {
                append("[\u200B]($picture)")
            }
            append("\uD83D\uDC8E **$chequeType** на **$total $currency** $icons\n")
            append("\nОдин чек: **$amount $currency**\n")
            append("Активаций: **$activations**\n")

            if(activated != null) {
                append("Активировано: **$activated**\n")
            }

            if (description != null) {
                append("\n$description\n")
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

