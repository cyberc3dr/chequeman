package io.chequeman.http

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.bearer
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

object HttpUsersTable : UUIDTable("http_users") {
    val token = varchar("token", 255)
    val username = varchar("username", 255)
}

class HttpUser(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<HttpUser>(HttpUsersTable)

    var token by HttpUsersTable.token
    var username by HttpUsersTable.username
}

suspend fun Application.configureAuthorization() {
    newSuspendedTransaction(Dispatchers.IO) {
        SchemaUtils.create(HttpUsersTable)
    }

    install(Authentication) {
        bearer("auth-bearer") {
            realm = "Data writes access"
            authenticate { credential ->
                val user = newSuspendedTransaction(Dispatchers.IO) {
                    HttpUser.find(HttpUsersTable.token eq credential.token)
                        .firstOrNull()
                } ?: return@authenticate null

                return@authenticate UserIdPrincipal(user.username)
            }
        }
    }
}