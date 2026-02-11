package io.chequeman.http

import io.chequeman.bots.Bot
import io.chequeman.database.DatabaseUtils
import io.ktor.server.application.*
import kotlin.io.path.Path

suspend fun Application.configureDatabase() {
    DatabaseUtils.initDatabase(Path("").toAbsolutePath().toFile(), config = config.database)
    Bot.loadAllDatabases()
}