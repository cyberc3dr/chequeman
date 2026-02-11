package io.chequeman.database

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database
import java.io.File
import java.io.IOException

object DatabaseUtils {

    @Serializable
    data class DatabaseConfig(
        val type: Type = Type.H2,
        val file: String = "database.db",
        val address: String = "127.0.0.1",
        val database: String = "database",
        val username: String = "admin",
        val password: String = "pass",
        val tablePrefix: String = "test_"
    )

    /**
     * Префикс для всех таблиц
     */
    var tablePrefix: String = ""

    /**
     * Инициализация базы данных
     *
     * @param config конфигурация
     *
     * @throws RuntimeException если произошла ошибка при инициализации базы данных
     */
    fun initDatabase(folder: File, config: DatabaseConfig) {
        val type = config.type

        var connectionString = "jdbc:${type.dbname}:"

        tablePrefix = config.tablePrefix

        when (type) {
            Type.H2, Type.SQLITE -> {
                val file = File(folder, config.file)

                try {
                    connectionString += file.canonicalPath
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }

                Database.connect(connectionString, type.className)
            }

            Type.MARIADB, Type.POSTGRESQL -> {
                connectionString += "//${config.address}/${config.database}"

                Database.connect(connectionString, type.className, config.username, config.password)
            }
        }
    }

    /**
     * Тип базы данных
     *
     * @param dbname название базы данных
     * @param className полное имя класса драйвера
     */
    @Serializable
    enum class Type(val dbname: String, val className: String) {
        /**
         * MariaDB (MySQL)
         */
        @SerialName("mariadb")
        MARIADB("mariadb", "org.mariadb.jdbc.Driver"),

        /**
         * PostgreSQL (Postgres)
         */
        @SerialName("postgresql")
        POSTGRESQL("postgresql", "org.postgresql.Driver"),

        /**
         * SQLite (встроенная база данных)
         */
        @SerialName("sqlite")
        SQLITE("sqlite", "org.sqlite.JDBC"),

        /**
         * H2 (встроенная база данных)
         */
        @SerialName("h2")
        H2("h2", "org.h2.Driver");

    }
}