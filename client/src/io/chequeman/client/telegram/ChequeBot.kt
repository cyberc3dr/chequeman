package io.chequeman.client.telegram

import io.chequeman.client.Cheque
import io.chequeman.client.ChequeRepository
import io.chequeman.client.http.RestChequeClient
import io.chequeman.client.workers.ChequeMessage
import io.chequeman.client.workers.ChequeWorkers
import io.chequeman.models.ChequeUpdate
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.BotSession
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import java.util.*

@Component
class ChequeBot(
    @Value($$"${telegram.token}")
    private val token: String,

    @Value($$"${telegram.group}")
    private val group: String,

    private val restClient: RestChequeClient,
    private val workers: ChequeWorkers,
    private val chequeRepository: ChequeRepository
) : SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private val telegramClient = OkHttpTelegramClient(botToken)

    override fun getBotToken() = token

    override fun getUpdatesConsumer() = this

    override fun consume(update: Update) {
        // Nothing here
    }

    @Scheduled(cron = "0 0 0 * * 0")
    fun cleanTable() {
        chequeRepository.deleteAll()
    }

    fun editMessage(id: Int, newText: String) = telegramClient.executeAsync(
        EditMessageText.builder()
            .chatId(group)
            .messageId(id)
            .text(newText)
            .build()
    )

    fun edit(id: Int, chequeMessage: ChequeMessage) = telegramClient.executeAsync(
        EditMessageText.builder()
            .parseMode(null)
            .disableWebPagePreview(false)
            .linkPreviewOptions(LinkPreviewOptions.builder()
                .showAboveText(true)
                .build()
            )
            .chatId(group)
            .messageId(id)
            .markdown(chequeMessage.text)
            .replyMarkup(
                InlineKeyboardMarkup.builder()
                    .keyboard(listOf(
                        InlineKeyboardRow(
                            InlineKeyboardButton.builder()
                                .text(chequeMessage.buttonText)
                                .url(chequeMessage.link)
                                .build()
                        ),
                        InlineKeyboardRow(
                            InlineKeyboardButton.builder()
                                .text("\uD83D\uDD0E ${chequeMessage.source.title}")
                                .url(chequeMessage.source.link)
                                .build()
                        )
                    ))
                    .build()
            )
            .build()
    )

    fun sendToChannel(chequeMessage: ChequeMessage) = telegramClient.executeAsync(
        SendMessage.builder()
            .parseMode(null)
            .disableWebPagePreview(false)
            .linkPreviewOptions(LinkPreviewOptions.builder()
                .showAboveText(true)
                .build()
            )
            .chatId(group)
            .markdown(chequeMessage.text)
            .replyMarkup(
                InlineKeyboardMarkup.builder()
                    .keyboard(listOf(
                        InlineKeyboardRow(
                            InlineKeyboardButton.builder()
                                .text(chequeMessage.buttonText)
                                .url(chequeMessage.link)
                                .build()
                        ),
                        InlineKeyboardRow(
                            InlineKeyboardButton.builder()
                                .text("\uD83D\uDD0E ${chequeMessage.source.title}")
                                .url(chequeMessage.source.link)
                                .build()
                        )
                    ))
                    .build()
            )
            .build()
    )

    fun sendToChannel(message: String) = telegramClient.executeAsync(
        SendMessage(group, message).apply {
            parseMode = null
        }
    )

    fun handleChequeUpdate(chequeUpdate: ChequeUpdate) {
        val uuid = chequeUpdate.uuid

        val cheque = restClient.getCheque(uuid)
            ?.let {
                it.copy(
                    sources = it.sources
                        .filterNot { src -> src.link.contains(group, true) }
                )
            }
            ?: return

        val message = workers.parse(cheque)
            ?: return

        val id = chequeRepository.findByUuid(uuid)?.messageId

        if(id == null) {
            chequeRepository.save(
                Cheque(
                    uuid,
                    sendToChannel(message).get().messageId
                )
            )
        } else {
            edit(id, message)
        }
    }

    @AfterBotRegistration
    fun afterRegistration(session: BotSession) {
        sendToChannel("ChequeMan bot started!")
    }
}