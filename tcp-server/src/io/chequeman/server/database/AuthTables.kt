package io.chequeman.server.database

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.UUID

object TCPUsersTable : UUIDTable("tcp_users") {
    val token = varchar("token", 255)
    val username = varchar("username", 255)
}

class TCPUser(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<TCPUser>(TCPUsersTable)

    var token by TCPUsersTable.token
    var username by TCPUsersTable.username
}