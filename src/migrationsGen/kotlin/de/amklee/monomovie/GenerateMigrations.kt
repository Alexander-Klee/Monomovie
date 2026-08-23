package de.amklee.monomovie

import de.amklee.monomovie.db.*
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.v1.migration.r2dbc.MigrationUtils
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

suspend fun main(args: Array<String>) {
    require(args.size == 1)
    R2dbcDatabase.mmvConnect(args[0])
    suspendTransaction(monomovieDb) {
        generateMigrationScript()
    }
}

@OptIn(ExperimentalDatabaseMigrationApi::class)
suspend fun generateMigrationScript() {
    val scriptDirectory = "src/main/resources/db/migration"
    Path(scriptDirectory).createDirectories()

    MigrationUtils.generateMigrationScript(
        Bookmarks,
        Watched,
        Migrations,
        scriptDirectory = scriptDirectory,
        scriptName = "V1__Initial",
    )
}
