package de.amklee.monomovie.db

import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object Bookmarks : IdTable<String>("bookmarks") {
    override val id = varchar("id", 255).references(Movies.id).entityId()
    override val primaryKey = PrimaryKey(id)
    val bookmarkedAt = timestamp("bookmarked_at")
    val isBookmarked = bool("is_bookmarked").default(true)
    val colour = varchar("colour", 63).nullable().default(null)

    data class Item(val id: String, val bookmarkedAt: Instant, val isBookmarked: Boolean = true, val colour: String? = null) {
        companion object {
            suspend operator fun invoke(row: ResultRow) = Item(
                id = row[id].value,
                bookmarkedAt = row[bookmarkedAt],
                isBookmarked = row[isBookmarked],
                colour = row[colour],
            )
        }
    }
}
