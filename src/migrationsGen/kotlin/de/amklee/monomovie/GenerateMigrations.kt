package de.amklee.monomovie

import de.amklee.monomovie.db.*
import javax.swing.JOptionPane
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.v1.migration.r2dbc.MigrationUtils
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

suspend fun main(args: Array<String>) {
    require(args.size == 1)
    R2dbcDatabase.mmvConnect(args[0])
    monomovieDb.runMigrations()
    suspendTransaction(monomovieDb) {
        generateMigrationScript()
    }
}

fun showScriptNameDialog(): String {
    val pattern = Regex("db/migration/V(\\d+)__([a-zA-Z_]+)\\.sql")
    val previous = R.db.migration.index.values
        .map {
            val match = pattern.matchEntire(it.path)!!
            match.groups[1]!!.value.toLong() to match.groups[2]!!.value
        }.maxBy { it.first }

    return JOptionPane.showInputDialog(
        null,
        "Pick the name for the next migration\n(format: $pattern)",
        "Migration Name",
        JOptionPane.PLAIN_MESSAGE,
        null,
        null,
        "V${previous.first + 1}__${previous.second}",
    ) as String? ?: throw UnsupportedOperationException("No name was specified")
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
        scriptName = showScriptNameDialog(),
    )
}
