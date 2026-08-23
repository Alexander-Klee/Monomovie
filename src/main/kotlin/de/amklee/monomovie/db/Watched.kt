package de.amklee.monomovie.db

import de.amklee.monomovie.service.CachedMovies
import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable

object Watched : IdTable<String>("watched") {
    override val id = varchar("id", 255).references(Bookmarks.id).entityId()
    override val primaryKey = PrimaryKey(id)
    val watchedAt = long("watched_at")

    data class Item(val item: CachedMovies.Movie, val watchedAt: Instant) {
        companion object {
            suspend operator fun invoke(row: ResultRow): Item = Item(
                item = row[id].value.let { id ->
                    CachedMovies.get(id) ?: throw IllegalStateException("Movie with id $id not found in cache")
                },
                watchedAt = Instant.fromEpochSeconds(row[watchedAt]),
            )
        }
    }
}
