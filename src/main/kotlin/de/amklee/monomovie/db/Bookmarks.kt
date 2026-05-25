package de.amklee.monomovie.db

import org.jetbrains.exposed.v1.core.dao.id.IdTable

object Bookmarks : IdTable<String>("bookmarks") {
    override val id = varchar("id", 255).entityId()
    override val primaryKey = PrimaryKey(id)
    val bookmarkedAt = long("bookmarked_at")
    val isBookmarked = bool("is_bookmarked").default(true)
    val colour = varchar("colour", 63).nullable()
}
