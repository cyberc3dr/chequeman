package io.chequeman.client.telegram

import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Link
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.parser.Parser
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.MessageEntity

data class TelegramMessage(
    val text: String,
    val entities: List<MessageEntity>
)

fun SendMessage.SendMessageBuilder<*, *>.message(message: TelegramMessage) = apply {
    text(message.text)
    entities(message.entities)
}

fun SendMessage.SendMessageBuilder<*, *>.markdown(markdown: String) = message(markdown.toTelegram())

fun EditMessageText.EditMessageTextBuilder<*, *>.message(message: TelegramMessage) = apply {
    text(message.text)
    entities(message.entities)
}

fun EditMessageText.EditMessageTextBuilder<*, *>.markdown(markdown: String) = message(markdown.toTelegram())

@Suppress("AssignedValueIsNeverRead")
fun String.toTelegram() : TelegramMessage {
    val parser = Parser.builder().build()
    val document = parser.parse(this)

    val textBuilder = StringBuilder()
    val entities = mutableListOf<MessageEntity>()

    var offset = 0

    document.accept(object : AbstractVisitor() {
        override fun visit(node: Text) {
            val literal = node.literal
            textBuilder.append(literal)
            offset += literal.length
        }

        override fun visit(node: StrongEmphasis) {
            val startOffset = offset
            visitChildren(node)
            val length = offset - startOffset
            if (length > 0) {
                entities.add(MessageEntity.builder()
                    .type("bold")
                    .offset(startOffset)
                    .length(length)
                    .build()
                )
            }
        }

        override fun visit(node: Emphasis) {
            val startOffset = offset
            visitChildren(node)
            val length = offset - startOffset
            if (length > 0) {
                entities.add(MessageEntity.builder()
                    .type("italic")
                    .offset(startOffset)
                    .length(length)
                    .build()
                )
            }
        }

        override fun visit(node: Code) {
            val literal = node.literal
            textBuilder.append(literal)
            entities.add(MessageEntity.builder()
                .type("code")
                .offset(offset)
                .length(literal.length)
                .build()
            )
            offset += literal.length
        }

        override fun visit(node: Link) {
            val startOffset = offset
            val url = node.destination
            visitChildren(node)
            val length = offset - startOffset
            if (length > 0) {
                entities.add(MessageEntity.builder()
                    .type("text_link")
                    .offset(startOffset)
                    .length(length)
                    .url(url)
                    .build())
            }
        }

        override fun visit(node: SoftLineBreak) {
            textBuilder.append("\n")
            offset += 1
        }

        override fun visit(node: HardLineBreak) {
            textBuilder.append("\n")
            offset += 1
        }

        override fun visit(node: Paragraph) {
            // Process paragraph children (text, emphasis, etc.)
            visitChildren(node)
            // Add a newline after the paragraph if it's not the last node
            if (node.next != null) {
                textBuilder.append("\n\n")
                offset += 2
            }
        }
    })

    return TelegramMessage(textBuilder.toString(), entities)
}