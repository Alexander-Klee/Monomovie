package de.amklee.monomovie.db

import org.jetbrains.exposed.v1.core.dao.id.IdTable

object Watched : IdTable<String>("watched") {
    override val id = varchar("id", 255).references(Bookmarks.id).entityId()
    override val primaryKey = PrimaryKey(id)
    val watchedAt = long("watched_at")
}
