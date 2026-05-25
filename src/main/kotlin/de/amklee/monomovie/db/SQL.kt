package de.amklee.monomovie.db

import de.amklee.monomovie.util.Resources
import io.ktor.server.application.*
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
import java.security.MessageDigest

lateinit var monomovieDb: R2dbcDatabase

fun R2dbcDatabase.Companion.mmvConnect() {
    monomovieDb = R2dbcDatabase.connect(
        url = "r2dbc:h2:file:///./monomovie;DB_CLOSE_DELAY=0",
        user = "monomovie",
        password = ""
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
    val index by Resources.Resource("db/migration/index.json")
    val migrations: Map<Long, String> = json.decodeFromString(index)
    for ((id_, name_) in migrations.entries.sortedBy { it.key }) {
        val content_ = Resources.Resource("db/migration/$name_").toString()
        val hash_ = content_.md5()

        val existing = Migrations[id_]
        if (existing == null) {
            suspendTransaction {
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

private fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(toByteArray())
    return digest.toHexString()
}

suspend operator fun <T : Any> IdTable<T>.get(k: T) = suspendTransaction(monomovieDb) { selectAll().where { this@get.id eq k }.singleOrNull() }
