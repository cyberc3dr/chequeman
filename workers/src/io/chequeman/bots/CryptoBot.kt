@file:OptIn(ExperimentalUuidApi::class)

package io.chequeman.bots

import io.chequeman.models.*
import kotlin.uuid.ExperimentalUuidApi

object CryptoBot : Bot {

    override val aliases = setOf("CryptoBot", "send")

    override suspend fun getCheque(link: String) = Bot.getCheque(link)
        ?.takeIf { it.cheque is CryptoBotMC || it.cheque is CryptoBotPersonal }

    override suspend fun getCheques() = Bot.getCheques()
        .filter { it.cheque is CryptoBotMC || it.cheque is CryptoBotPersonal }

    private fun extractPicture(markdownMessage: String): String? =
        Regex("""\[​?]\((https?://[^\s)]+)\)""").find(markdownMessage)?.groups?.get(1)?.value

    private fun extractDescription(markdownMessage: String, isForward: Boolean = false): String? {
        val raw = Regex("""💬\s*(.+)""", RegexOption.DOT_MATCHES_ALL)
            .find(markdownMessage)?.groups?.get(1)?.value?.trim()
            ?.removePrefix("__")?.removeSuffix("__")
            ?: return null

        return if (isForward) {
            // Для forward: удаляем последние два отдельно стоящие абзаца и всё
            val paragraphs = raw.split(Regex("""\n\s*\n""")).map { it.trim() }
            if (paragraphs.size > 2) {
                paragraphs.dropLast(3).joinToString("\n\n").trim()
            } else {
                ""
            }
        } else raw
    }

    private fun extractId(link: String): String {
        // Извлекаем id из ссылки (например, startapp=mc_xxx или start=t_xxx)
        val regex = Regex("""[?&](?:startapp|start)=([a-zA-Z]+_[\w\d]+)""")
        return regex.find(link)?.groups?.get(1)?.value ?: link
    }

    override suspend fun handleWriteRequest(chequeWriteRequest: ChequeWriteRequest): Boolean {
        val link = chequeWriteRequest.link
        val message = chequeWriteRequest.message
        val markdownMessage = chequeWriteRequest.markdownMessage
        val forwardMessage = chequeWriteRequest.forwardMessage
        val forwardMarkdownMessage = chequeWriteRequest.forwardMarkdownMessage
        val source = chequeWriteRequest.source

        val id = extractId(link)

        if (forwardMessage != null && forwardMarkdownMessage != null) {
            val picture = extractPicture(forwardMessage)
            val description = extractDescription(forwardMessage, true)

            val isMulti = isMultiCheque(forwardMessage, forwardMarkdownMessage)

            val cheque = when {
                isMulti -> parseForwardMultiCheque(forwardMessage, forwardMarkdownMessage, picture, description)
                else -> parseForwardPersonalCheque(forwardMessage, forwardMarkdownMessage)
            }

            upsertCheque(id, source, cheque)

            return true
        }

        if(message != null && markdownMessage != null) {
            val picture = extractPicture(markdownMessage)
            val description = extractDescription(markdownMessage)

            val isMulti = isMultiCheque(message, markdownMessage)

            val cheque = when {
                isMulti -> parseNonForwardMultiCheque(message, picture, description)
                else -> parseNonForwardPersonalCheque(message)
            }

            upsertCheque(id, source, cheque)

            return true
        }

        upsertCheque(id, source, null)
        return true
    }


    private fun isMultiCheque(message: String, markdownMessage: String): Boolean =
        Regex("""Мультичек|multi-?use""", RegexOption.IGNORE_CASE).containsMatchIn(message)
            || Regex("""Мультичек|multi-?use""", RegexOption.IGNORE_CASE).containsMatchIn(markdownMessage)

    private fun parseForwardMultiCheque(
        message: String,
        markdownMessage: String,
        picture: String?,
        description: String?
    ): CryptoBotMC {
        val sumRegex = Regex("""Общая сумма[:\s]*([0-9.,]+)\s*([A-Z]+)""")
        val oneActivationRegex = Regex("""Сумма одного чека[:\s]*[🪙]*\s*([0-9.,]+)\s*([A-Z]+)""")
        val activationsRegex = Regex("""Количество активаций[:\s]*([0-9]+)""")
        val activatedRegex = Regex("""Выполнено:\s*([0-9]+)""")
        val passwordRegex = Regex("""парол[ья]""", RegexOption.IGNORE_CASE)
        val premiumRegex = Regex("""Premium""", RegexOption.IGNORE_CASE)
        val newUserRegex = Regex("""только новые пользователи|only new users""", RegexOption.IGNORE_CASE)

        val sumMatch = sumRegex.find(message)
        val oneActivationMatch = oneActivationRegex.find(message)
        val activationsMatch = activationsRegex.find(message)
        val activatedMatch = activatedRegex.find(message)

        val totalAmount = sumMatch?.groups?.get(1)?.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        val currency = sumMatch?.groups?.get(2)?.value ?: "TON"
        val oneActivation = oneActivationMatch?.groups?.get(1)?.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        val activations = activationsMatch?.groups?.get(1)?.value?.toIntOrNull() ?: 1
        val activated = activatedMatch?.groups?.get(1)?.value?.toIntOrNull() ?: 0

        val isPremium = premiumRegex.containsMatchIn(message) || premiumRegex.containsMatchIn(markdownMessage)
        val passwordProtected = passwordRegex.containsMatchIn(message) || passwordRegex.containsMatchIn(markdownMessage)
        val requireNewUser = newUserRegex.containsMatchIn(message) || newUserRegex.containsMatchIn(markdownMessage)

        return CryptoBotMC(
            currency = currency,
            oneActivation = oneActivation,
            picture = picture,
            description = description,
            totalAmount = totalAmount,
            activations = activations,
            activated = activated,
            passwordProtected = passwordProtected,
            isPremium = isPremium,
            requireNewUser = requireNewUser
        )
    }

    private fun parseForwardPersonalCheque(
        message: String,
        markdownMessage: String
    ): CryptoBotPersonal {
        val sumRegex = Regex("""Сумма[:\s]*[🪙]*\s*([0-9.,]+)\s*([A-Z]+)""")
        val receiverRegex = Regex("""Только\s+@([A-Za-z0-9_]+)\s+может активировать этот чек""")
        val sumMatch = sumRegex.find(message)
        val currency = sumMatch?.groups?.get(2)?.value ?: "TON"
        val oneActivation = sumMatch?.groups?.get(1)?.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        val receiver = receiverRegex.find(message)?.groups?.get(1)?.value ?: ""
        val hasPassword = Regex("""парол[ья]""", RegexOption.IGNORE_CASE).containsMatchIn(message)
            || Regex("""парол[ья]""", RegexOption.IGNORE_CASE).containsMatchIn(markdownMessage)

        return CryptoBotPersonal(
            oneActivation = oneActivation,
            currency = currency,
            receiver = receiver,
            hasPassword = hasPassword
        )
    }

    private fun parseNonForwardMultiCheque(
        message: String,
        picture: String?,
        description: String?
    ): CryptoBotMC {
        val sumRegex = Regex("""на [🪙]*\s*([0-9.,]+)\s*([A-Z]+)""")
        val oneActivationRegex = Regex("""Сумма одного чека[:\s]*[🪙]*\s*([0-9.,]+)\s*([A-Z]+)""")
        val activationsRegex = Regex("""Количество активаций[:\s]*([0-9]+)""")
        val sumMatch = sumRegex.find(message)
        val oneActivationMatch = oneActivationRegex.find(message)
        val activationsMatch = activationsRegex.find(message)

        val totalAmount = sumMatch?.groups?.get(1)?.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        val currency = sumMatch?.groups?.get(2)?.value ?: "TON"
        val oneActivation = oneActivationMatch?.groups?.get(1)?.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        val activations = activationsMatch?.groups?.get(1)?.value?.toIntOrNull() ?: 1

        return CryptoBotMC(
            currency = currency,
            oneActivation = oneActivation,
            picture = picture,
            description = description,
            totalAmount = totalAmount,
            activations = activations,
        )
    }

    private fun parseNonForwardPersonalCheque(
        message: String
    ): CryptoBotPersonal {
        val sumRegex = Regex("""на [🪙]*\s*([0-9.,]+)\s*([A-Z]+)""")
        val receiverRegex = Regex("""для\s+@?([A-Za-z0-9_]+)""")
        val sumMatch = sumRegex.find(message)
        val currency = sumMatch?.groups?.get(2)?.value ?: "TON"
        val oneActivation = sumMatch?.groups?.get(1)?.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        val receiver = receiverRegex.find(message)?.groups?.get(1)?.value ?: ""

        return CryptoBotPersonal(
            oneActivation = oneActivation,
            currency = currency,
            receiver = receiver,
        )
    }
}