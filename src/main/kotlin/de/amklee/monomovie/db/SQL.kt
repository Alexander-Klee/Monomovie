@file:Suppress("ktlint:standard:property-naming")

package de.amklee.monomovie.db

import de.amklee.monomovie.R
import io.ktor.server.application.*
import java.security.MessageDigest
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

lateinit var monomovieDb: R2dbcDatabase

fun R2dbcDatabase.Companion.mmvConnect(path: String = "./monomovie") {
    monomovieDb = R2dbcDatabase.connect(
        url = "r2dbc:h2:file:///$path;DB_CLOSE_DELAY=0",
        user = "monomovie",
        password = "",
    )
}

object Migrations : LongIdTable("migrations") {
    val name = varchar("name", 255)
    val hash = varchar("hash", 255)
}

private val json = Json { prettyPrint = true }

@Suppress("UnusedReceiverParameter")
suspend fun Application.configureDatabases() {
    R2dbcDatabase.mmvConnect()

    suspendTransaction(monomovieDb) { SchemaUtils.create(Migrations) }

    run {
        val id_ = 0L
        val name_ = "V0__Legacy.sql"
        val hash_ = ""

        val existing = Migrations[id_]
        if (existing == null) {
            suspendTransaction(monomovieDb) {
                performLegacyMigration()
                Migrations.insert {
                    it[id] = id_
                    it[name] = name_
                    it[hash] = hash_
                }
            }
        }
    }

    val pattern = Regex("db/migration/V(\\d+)__[a-zA-Z_]+\\.sql")
    val sortedMigrations = R.db.migration.index.values
        .map {
            val id = pattern.matchEntire(it.path)!!.groups[1]!!.value.toLong()
            id to it
        }.sortedBy { it.first }

    for ((id_, resource_) in sortedMigrations) {
        val name_ = resource_.path.requireAndTrimStart("db/migration/")
        val content_ = resource_.toString()
        val hash_ = content_.md5()

        val existing = Migrations[id_]
        if (existing == null) {
            suspendTransaction(monomovieDb) {
                exec(content_)
                Migrations.insert {
                    it[id] = id_
                    it[name] = name_
                    it[hash] = hash_
                }
            }
        } else {
            require(existing[Migrations.name] == name_) { "Multiple migrations with different names for ID $id_" }
            require(existing[Migrations.hash] == hash_) { "Applied transaction for name $name_ does not correspond to hash $hash_" }
        }
    }
}

private fun String.requireAndTrimStart(start: String): String {
    require(startsWith(start))
    return substring(start.length)
}

private fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(toByteArray())
    return digest.toHexString()
}

suspend operator fun <T : Any> IdTable<T>.get(k: T) = suspendTransaction(monomovieDb) {
    selectAll().where { this@get.id eq k }.singleOrNull()
}
