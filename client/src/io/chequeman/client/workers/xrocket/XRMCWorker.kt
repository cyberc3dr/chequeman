package io.chequeman.client.workers.xrocket

import io.chequeman.client.workers.ChequeMessage
import io.chequeman.client.workers.ChequeWorker
import io.chequeman.client.workers.plainString
import io.chequeman.models.ChequeContainer
import io.chequeman.models.ChequeModel
import io.chequeman.models.XRocketMC

object XRMCWorker : ChequeWorker {

    override fun parse(raw: ChequeModel): ChequeMessage? {
        val cheque = raw.cheque as? XRocketMC ?: return null

        val chequeType = "Мультичек"

        val icons = buildString {
            if (cheque.password != null) append("🔒 ")
            if (cheque.isPremium) append("⭐ ")
            if (cheque.groups.isNotEmpty()) append("👥 ")
        }.trim()

        val description = cheque.description?.takeIf { it.isNotBlank() }
        val amount = cheque.oneActivation.plainString
        val currency = cheque.currency
        val total = cheque.totalAmount.plainString
        val activations = cheque.activations
        val picture = cheque.picture
        val referralPercent = cheque.referralPercent

        val password = cheque.password
        val groups = cheque.groups

        val hashtags = buildList {
            add("#XRocket")
            add("#Мультичек")
            add("$$currency")
            add(if (cheque.isPremium) "#Premium" else "#БезPremium")
            add(if (password != null) "#Пароль" else "#БезПароля")
            add(if (groups.isNotEmpty()) "#Группы" else "#БезГрупп")
        }.joinToString(" ")

        val text = buildString {
            if(picture != null) {
                append("[\u200B]($picture)")
            }
            append("\uD83D\uDE80 **$chequeType** на **$total $currency** $icons\n")

            append("\nОдин чек: **$amount $currency**\n")
            if(referralPercent > 0) {
                append("Реферальный процент: **$referralPercent%**\n")
            }
            append("Активаций: **$activations**\n")

            if (description != null) {
                append("\n$description\n")
            }

            if(password != null) {
                append("\n🔑 **Пароль:** `$password`\n")
            }

            if(groups.isNotEmpty()) {
                append("\n👥 **Ограничен подписчиками:**\n")
                groups.forEach { group ->
                    append("✅ · $group\n")
                }
            }

            append("\n$hashtags")
        }

        val buttonText = "\uD83D\uDE80 Получить $amount $currency"
        val id = raw.ids.first()

        val link = if(cheque.old) {
            "https://t.me/xrocket?start=$id"
        } else {
            "https://t.me/xrocket/app?startapp=$id"
        }
        val source = raw.sources.first() // Здесь должен быть линк на источник

        return ChequeMessage(
            text = text,
            buttonText = buttonText,
            link = link,
            source = source
        )
    }


}