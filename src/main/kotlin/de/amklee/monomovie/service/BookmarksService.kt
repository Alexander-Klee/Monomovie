package de.amklee.monomovie.service

import de.amklee.monomovie.db.Bookmarks
import de.amklee.monomovie.db.get
import de.amklee.monomovie.db.monomovieDb
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import org.jetbrains.exposed.v1.r2dbc.upsert
import java.time.Instant

object BookmarksService {
    val eventFlow = MutableSharedFlow<Event>()

    suspend operator fun contains(id: String): Boolean = Bookmarks[id] != null
    suspend fun isBookmarked(id: String): Boolean = Bookmarks[id]?.let { it[Bookmarks.isBookmarked] } ?: false

    suspend fun addBookmark(id: String) = suspendTransaction(monomovieDb) {
        Bookmarks.upsert(onUpdate = {
            it[Bookmarks.isBookmarked] = true
        }) {
            it[Bookmarks.id] = id
            it[bookmarkedAt] = Instant.now().epochSecond
            it[isBookmarked] = true
        }
        eventFlow.emit(Event.Added(id))
    }

    suspend fun removeBookmark(id: String) = suspendTransaction(monomovieDb) {
        val modifiedCount = Bookmarks.update(where = { Bookmarks.id eq id }) {
            it[isBookmarked] = false
        }
        if (modifiedCount > 0) eventFlow.emit(Event.Removed(id))
    }

    suspend fun deleteBookmark(id: String) = suspendTransaction(monomovieDb) {
        val modifiedCount = Bookmarks.deleteWhere { Bookmarks.id eq id }
        if (modifiedCount > 0) eventFlow.emit(Event.Removed(id))
    }

    suspend fun getBookmarks(): List<Bookmarks.Item> = suspendTransaction(monomovieDb) {
        Bookmarks.selectAll()
            .where { Bookmarks.isBookmarked eq true }
            .orderBy(Bookmarks.bookmarkedAt, SortOrder.DESC)
            .map { Bookmarks.Item(it) }
            .toList()
    }

    suspend fun setColour(id: String, colour: String?) = suspendTransaction(monomovieDb) {
        Bookmarks.update(where = { Bookmarks.id eq id }) {
            it[Bookmarks.colour] = colour
        }
    }
}
