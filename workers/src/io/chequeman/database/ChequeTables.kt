package io.chequeman.database

import io.chequeman.models.ChequeContainer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.json.jsonb
import java.util.UUID

object ChequesTable : UUIDTable("cheques") {
    val ids = array<String>("ids", 255)
        .default(emptyList())
    val sources = array<String>("sources", 255)
        .default(emptyList())
    val cheque = jsonb("cheque", Json, ChequeContainer.serializer())
        .nullable()
}

class ChequeEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ChequeEntity>(ChequesTable)

    var ids by ChequesTable.ids
    var sources by ChequesTable.sources
    var cheque by ChequesTable.cheque
}