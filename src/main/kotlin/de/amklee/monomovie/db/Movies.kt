package de.amklee.monomovie.db

import de.amklee.monomovie.service.remote.MediaEntry
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.json.json

object Movies : IdTable<String>("movies") {
    override val id = varchar("id", 255).entityId()
    override val primaryKey = PrimaryKey(id)
    val mediaEntry = json("media_entry", Json, MediaEntry.serializer()).nullable()
    val cacheDate = long("cache_date")
}
