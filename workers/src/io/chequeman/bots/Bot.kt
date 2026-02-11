package io.chequeman.bots

import io.chequeman.database.ChequeEntity
import io.chequeman.database.ChequesTable
import io.chequeman.extensions.containsIgnoreCase
import io.chequeman.models.ChequeContainer
import io.chequeman.models.ChequeEntry
import io.chequeman.models.ChequeWriteRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

sealed interface Bot {

    val aliases: Set<String>

    suspend fun getCheque(link: String): ChequeEntry?

    suspend fun getCheques(): List<ChequeEntry>

    suspend fun handleWriteRequest(chequeWriteRequest: ChequeWriteRequest) : Boolean

    @OptIn(ExperimentalUuidApi::class)
    suspend fun upsertCheque(id: String, source: String, cheque: ChequeContainer?) {
        val uuid = newSuspendedTransaction {
            // Новый подход: если cheque == null, ищем по id, иначе по содержимому
            val entity = if (cheque == null) {
                ChequeEntity.all().firstOrNull { it.ids.contains(id) }
            } else {
                ChequeEntity.all().firstOrNull { it.cheque == cheque }
            }

            if (entity != null) {
                val updatedIds = if (!entity.ids.contains(id)) entity.ids + id else entity.ids
                val updatedSources = if (!entity.sources.contains(source)) entity.sources + source else entity.sources

                // Безопасное копирование данных из старого чека в новый (например, пароль)
                val mergedCheque = when {
                    cheque == null -> entity.cheque
                    entity.cheque == null -> cheque
                    else -> entity.cheque!!.safeCopyFrom(cheque)
                }

                entity.ids = updatedIds
                entity.sources = updatedSources
                entity.cheque = mergedCheque

                entity.id.value.toKotlinUuid()
            } else {
                val newEntity = ChequeEntity.new {
                    this.ids = listOf(id)
                    this.sources = listOf(source)
                    this.cheque = cheque
                }

                newEntity.id.value.toKotlinUuid()
            }
        }
    }

    companion object : CoroutineScope {

        override val coroutineContext = Dispatchers.IO

        val bots: List<Bot> = listOf(
            XRocketBot,
            CryptoBot
        )

        @OptIn(ExperimentalUuidApi::class)
        suspend fun getCheque(link: String): ChequeEntry? = newSuspendedTransaction {
            // Поиск по uuid
            val byUuid = runCatching { UUID.fromString(link) }.getOrNull()?.let { uuid ->
                ChequeEntity.findById(uuid)
            }

            if (byUuid != null) {
                return@newSuspendedTransaction ChequeEntry(
                    ids = byUuid.ids,
                    sources = byUuid.sources,
                    uuid = byUuid.id.value.toKotlinUuid(),
                    cheque = byUuid.cheque
                )
            }
            // Поиск по одному из id
            ChequeEntity.all().firstOrNull { it.ids.contains(link) }?.let {
                ChequeEntry(
                    ids = it.ids,
                    sources = it.sources,
                    uuid = it.id.value.toKotlinUuid(),
                    cheque = it.cheque
                )
            }
        }

        @OptIn(ExperimentalUuidApi::class)
        suspend fun getCheques(): List<ChequeEntry> = newSuspendedTransaction {
            ChequeEntity.all().map {
                ChequeEntry(
                    ids = it.ids,
                    sources = it.sources,
                    uuid = it.id.value.toKotlinUuid(),
                    cheque = it.cheque
                )
            }
        }

        suspend fun loadAllDatabases() = newSuspendedTransaction {
            SchemaUtils.create(ChequesTable)
        }

        suspend fun handleWriteRequest(chequeWriteRequest: ChequeWriteRequest): Boolean {
            val bot = bots.firstOrNull { it.aliases.containsIgnoreCase(chequeWriteRequest.bot) }
                ?: return false

            return bot.handleWriteRequest(chequeWriteRequest)
        }
    }

}