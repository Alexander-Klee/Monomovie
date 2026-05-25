package de.amklee.monomovie

import de.amklee.monomovie.db.Bookmarks
import de.amklee.monomovie.db.Migrations
import de.amklee.monomovie.db.Watched
import de.amklee.monomovie.db.mmvConnect
import de.amklee.monomovie.db.monomovieDb
import de.amklee.monomovie.util.associateColliding
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.v1.migration.r2dbc.MigrationUtils
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.nio.file.Path
import kotlin.io.path.*

suspend fun main() {
    R2dbcDatabase.mmvConnect()
    suspendTransaction(monomovieDb) {
        generateMigrationScript()
    }
}

@OptIn(ExperimentalDatabaseMigrationApi::class)
suspend fun generateMigrationScript() {
    val scriptDirectory = "src/main/resources/db/migration"
    Path(scriptDirectory).createDirectories()

    MigrationUtils.generateMigrationScript(
        Bookmarks, Watched, Migrations,
        scriptDirectory = scriptDirectory,
        scriptName = "V1__Initial",
    )

    recreateIndex(Path(scriptDirectory))
}

@OptIn(ExperimentalSerializationApi::class)
fun recreateIndex(dir: Path) {
    val pattern = Regex("V(\\d+)__[a-zA-Z_]+\\.sql")
    val migrations = dir.useDirectoryEntries { it
        .filter { it.name != "index.json" }
        .associateColliding {
            require(it.isRegularFile()) { "Expected a regular file, but got: ${it.name}" }
            require(it.extension == "sql") { "Expected a .sql file, but got: ${it.name}" }
            val id = pattern.matchEntire(it.name)!!.groups[1]!!.value.toLong()
            id to it.name
        }
    }

    require(migrations.keys == (1..migrations.keys.max()).toSet()) { "Migrations not contiguous" }

    (dir/"index.json").outputStream().use { Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }.encodeToStream(migrations, it) }
}
