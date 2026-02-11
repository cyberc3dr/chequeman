package io.chequeman.server.workers

import io.chequeman.models.ChequeContainer
import io.chequeman.models.ChequeEntry
import io.chequeman.models.ChequeWriteRequest
import io.chequeman.models.SourceContainer
import io.chequeman.models.XRocketMC
import io.chequeman.models.XRocketPersonal

object XRocketBot : Bot {

    override val aliases = setOf("xrocket", "tonRocketBot")

    override fun handleRequest(request: ChequeWriteRequest): ChequeEntry {
        fun chequeType(link: String): String? {
            val regex = Regex("""[?&](?:startapp|start)=([a-zA-Z]+)_""")
            val match = regex.find(link)
            return match?.groups?.get(1)?.value?.lowercase()
        }

        val source = SourceContainer(request.sourceTitle, request.source)

        val type = chequeType(request.link)

        val cheque = when (type) {
            "mc", "mci" -> parseMultiCheque(request)
            "t" -> parsePersonalCheque(request)
            else -> null
        }

        return ChequeEntry(extractId(request.link), source, cheque)
    }

    private fun parseMultiCheque(
        chequeWriteRequest: ChequeWriteRequest
    ): XRocketMC {
        val message = chequeWriteRequest.message
        val markdownMessage = chequeWriteRequest.markdownMessage
        val inlineDescription = chequeWriteRequest.inlineDescription
        val buttonText = chequeWriteRequest.buttonText

        if (inlineDescription == null) throw IllegalArgumentException("Inline description cannot be null")

        val parts = inlineDescription.split(" · ")

        // Одна активация: первая часть (индекс 0)
        val oneActivationRegex = Regex("""Одна активация:\s*([\d.,]+)[  ]*([A-Z]+)?""")
        val oneActivationMatch = oneActivationRegex.find(parts.getOrNull(0) ?: "")
        val oneActivation = oneActivationMatch?.groups?.get(1)?.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        val currency = oneActivationMatch?.groups?.get(2)?.value ?: "USDT"

        // Сумма чека: вторая часть (индекс 1)
        val totalAmountRegex = Regex("""([\d.,]+)""")
        val totalAmount = parts.getOrNull(1)?.let {
            totalAmountRegex.find(it)?.groups?.get(1)?.value?.replace(",", ".")?.toDoubleOrNull()
        } ?: 0.0

        // Количество активаций: третья часть (индекс 2), ищем число
        val activationsRegex = Regex("""(\d+)""")
        val activations = parts.getOrNull(2)?.let {
            activationsRegex.find(it)?.groups?.get(1)?.value?.toIntOrNull()
        } ?: 1

        // Реферальный процент: четвертая часть (индекс 3), ищем число перед %
        val referralRegex = Regex("""(\d+)%""")
        val referralPercent = parts.getOrNull(3)?.let {
            referralRegex.find(it)?.groups?.get(1)?.value?.toIntOrNull()
        } ?: 0

        // Парсим картинку из markdownMessage (если есть)
        val pictureRegex = Regex("""^\[\u200d]\((.*?)\)""")
        val picture = pictureRegex.find(markdownMessage ?: "")?.groups?.get(1)?.value

        // Описание из markdownMessage (после 💬)
        val description = Regex("""💬\s*(.+)""", RegexOption.DOT_MATCHES_ALL)
            .find(message ?: "")?.groups?.get(1)?.value?.trim()

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
}