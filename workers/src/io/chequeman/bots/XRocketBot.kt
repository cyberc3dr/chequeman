@file:OptIn(ExperimentalUuidApi::class)

package io.chequeman.bots

import io.chequeman.extensions.decimal
import io.chequeman.models.ChequeWriteRequest
import io.chequeman.models.XRocketMC
import io.chequeman.models.XRocketPersonal
import kotlin.uuid.ExperimentalUuidApi

object XRocketBot : Bot {

    override val aliases = setOf("xrocket", "tonRocketBot")

    override suspend fun getCheque(link: String) = Bot.getCheque(link)
        ?.takeIf { it.cheque is XRocketMC || it.cheque is XRocketPersonal }

    override suspend fun getCheques() = Bot.getCheques()
        .filter { it.cheque is XRocketMC || it.cheque is XRocketPersonal }

    private fun parseMultiCheque(
        chequeWriteRequest: ChequeWriteRequest
    ): XRocketMC {
        val markdownMessage = chequeWriteRequest.markdownMessage
        val inlineTitle = chequeWriteRequest.inlineTitle
        val inlineDescription = chequeWriteRequest.inlineDescription
        val buttonText = chequeWriteRequest.buttonText

        if (inlineTitle == null || inlineDescription == null) throw IllegalArgumentException("Inline description cannot be null")

        // Новая структура: inlineTitle = "Rocket-чек на 10 USDT"
        // inlineDescription = "1 USDT за активацию · 10 активаций · 5% рефералка"
        val sumRegex = Regex("""Rocket-чек на ([\d.,]+)[  ]*([A-Z]+)""")
        val sumMatch = sumRegex.find(inlineTitle)
        val totalAmount = sumMatch?.groups?.get(1)?.value?.decimal ?: 0.0
        val currency = sumMatch?.groups?.get(2)?.value ?: "USDT"

        // inlineDescription: "1 USDT за активацию · 10 активаций · 5% рефералка"
        val parts = inlineDescription.split(" · ")
        val oneActivationRegex = Regex("""([\d.,]+)[  ]*([A-Z]+)?""")
        val activationsRegex = Regex("""(\d+)\s*активац""")
        val referralRegex = Regex("""(\d+)%\s*реферал""")

        val oneActivation = oneActivationRegex.find(parts.getOrNull(0) ?: "")?.groups?.get(1)?.value?.decimal ?: 0.0
        val activations = activationsRegex.find(parts.getOrNull(1) ?: "")?.groups?.get(1)?.value?.toIntOrNull() ?: 1
        val referralPercent = referralRegex.find(parts.getOrNull(2) ?: "")?.groups?.get(1)?.value?.toIntOrNull() ?: 0

        // Парсим картинку из markdownMessage (если есть)
        val pictureRegex = Regex("""^\[\u200d]\((.*?)\)""")
        val picture = pictureRegex.find(markdownMessage ?: "")?.groups?.get(1)?.value

        // Описание из markdownMessage (после 💬)
        val description = Regex("""💬\s*(.+)""", RegexOption.DOT_MATCHES_ALL)
            .find(markdownMessage ?: "")?.groups?.get(1)?.value?.trim()
            ?.removePrefix("__")?.removeSuffix("__")

        val isPremium = buttonText?.contains("🌟") == true

        val forwardMessage = chequeWriteRequest.forwardMessage
        val forwardMarkdownMessage = chequeWriteRequest.forwardMarkdownMessage

        var password: String? = null
        var groups: List<String> = emptyList()
        var awardsPaid: Int? = null

        if(forwardMessage != null && forwardMarkdownMessage != null) {
            // --- Парсим пароль ---
            // Ищем "Пароль: ..." или "**Пароль:** ..."
            val passwordRegex = Regex("""(?:\*\*Пароль:\*\*|Пароль:)\s*([^\n*]+)""")
            password = passwordRegex.find(forwardMessage)?.groups?.get(1)?.value
                ?: passwordRegex.find(forwardMarkdownMessage)?.groups?.get(1)?.value
            password = password?.trim()

            // --- Парсим группы ---
            // Ищем блок после "Ограничен подписчиками:" до пустой строки или конца блока
            val groupsRegex = Regex("""Ограничен подписчиками:\s*\n((?:[^\n]+\n?)+)""")
            val groupsBlock = groupsRegex.find(forwardMessage)?.groups?.get(1)?.value
                ?: groupsRegex.find(forwardMarkdownMessage)?.groups?.get(1)?.value
            if (groupsBlock != null) {
                groups = groupsBlock.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && it.startsWith("✅ · ") }
                    .map { it.removePrefix("✅ · ") }
            }

            // --- Парсим выплачено наград ---
            // Ищем "Выплачено наград: ..." или "Выплачено наград: **...**"
            val awardsPaidRegex = Regex("""Выплачено наград:\s*\*{0,2}(\d+)""")
            awardsPaid = awardsPaidRegex.find(forwardMessage)?.groups?.get(1)?.value?.toIntOrNull()
                ?: awardsPaidRegex.find(forwardMarkdownMessage)?.groups?.get(1)?.value?.toIntOrNull()
        }

        // Определяем old по ссылке
        val old = chequeWriteRequest.link.contains("start=")

        return XRocketMC(
            oneActivation = oneActivation,
            currency = currency,
            picture = picture,
            description = description,
            totalAmount = totalAmount,
            activations = activations,
            referralPercent = referralPercent,
            isPremium = isPremium,
            password = password,
            groups = groups,
            awardsPaid = awardsPaid,
            old = old
        )
    }

    private fun parsePersonalCheque(
        chequeWriteRequest: ChequeWriteRequest
    ): XRocketPersonal {
        val inlineTitle = chequeWriteRequest.inlineTitle ?: throw IllegalArgumentException("Inline title cannot be null")
        // Новая структура: inlineTitle = "Чек на 5 USDT"
        val sumRegex = Regex("""Чек на ([\d.,]+)[  ]*([A-Z]+)""")
        val sumMatch = sumRegex.find(inlineTitle)
        val oneActivation = sumMatch?.groups?.get(1)?.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        val currency = sumMatch?.groups?.get(2)?.value ?: "USDT"

        // Определяем old по ссылке
        val old = chequeWriteRequest.link.contains("start=")

        return XRocketPersonal(
            oneActivation = oneActivation,
            currency = currency,
            old = old
        )
    }

    private fun extractId(link: String): String {
        // Извлекаем id из ссылки (например, startapp=mc_xxx или start=t_xxx)
        val regex = Regex("""[?&](?:startapp|start)=([a-zA-Z]+_[\w\d]+)""")
        return regex.find(link)?.groups?.get(1)?.value ?: link
    }

    override suspend fun handleWriteRequest(chequeWriteRequest: ChequeWriteRequest): Boolean {
        fun chequeType(link: String): String? {
            val regex = Regex("""[?&](?:startapp|start)=([a-zA-Z]+)_""")
            val match = regex.find(link)
            return match?.groups?.get(1)?.value?.lowercase()
        }

        val type = chequeType(chequeWriteRequest.link)

        return when (type) {
            "mc", "mci" -> {
                val cheque = parseMultiCheque(chequeWriteRequest)
                upsertCheque(extractId(chequeWriteRequest.link), chequeWriteRequest.source, cheque)
                true
            }
            "t" -> {
                val cheque = parsePersonalCheque(chequeWriteRequest)
                upsertCheque(extractId(chequeWriteRequest.link), chequeWriteRequest.source, cheque)
                true
            }
            else -> false
        }
    }
}