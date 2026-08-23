package de.amklee.monomovie.db

import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object BookmarksDB {
    val eventFlow = MutableSharedFlow<Event>()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    data class BookmarkItem(val id: String, val bookmarkedAt: Long, val isBookmarked: Boolean = true, val colour: String? = null)

    private var bookmarksDB: BookmarksDB3 = openBookmarksDb()

    private fun save() = synchronized(this) {
        bookmarksDB.save()
    }

    operator fun contains(id: String): Boolean = bookmarksDB.bookmarks.any { it.id == id }

    fun isBookmarked(id: String): Boolean = bookmarksDB.bookmarks.any {
        it.id == id && it.isBookmarked
    }

    suspend fun addBookmark(id: String) {
        if (contains(id)) {
            markBookmark(id)
            return
        }
        bookmarksDB = bookmarksDB
            .copy(
                bookmarks = bookmarksDB.bookmarks +
                    listOf(
                        BookmarkItem3(id, Instant.now().epochSecond, true),
                    ),
            )
        save()
        eventFlow.emit(Event.Added(id))
    }

    private suspend fun markBookmark(id: String) {
        bookmarksDB = bookmarksDB.copy(
            bookmarks = bookmarksDB.bookmarks
                .map {
                    if (it.id == id) {
                        it.copy(isBookmarked = true)
                    } else {
                        it
                    }
                },
        )
        save()
        eventFlow.emit(Event.Added(id))
    }

    suspend fun removeBookmark(id: String) {
        bookmarksDB = bookmarksDB.copy(
            bookmarks = bookmarksDB.bookmarks
                .map {
                    if (it.id == id) {
                        it.copy(isBookmarked = false)
                    } else {
                        it
                    }
                },
        )
        save()
        eventFlow.emit(Event.Removed(id))
    }

    suspend fun deleteBookmark(id: String) {
        bookmarksDB = bookmarksDB.copy(bookmarks = bookmarksDB.bookmarks.filterNot { it.id == id })
        save()
        eventFlow.emit(Event.Removed(id))
    }

    fun getBookmarks(): List<BookmarkItem> = bookmarksDB.bookmarks
        .filter { it.isBookmarked }
        .sortedByDescending { it.bookmarkedAt }
        .map { BookmarkItem(it.id, it.bookmarkedAt, it.isBookmarked, it.colour) }

    suspend fun setColour(id: String, colour: String?) {
        bookmarksDB = bookmarksDB.copy(
            bookmarks = bookmarksDB.bookmarks
                .map {
                    if (it.id == id) {
                        it.copy(colour = colour)
                    } else {
                        it
                    }
                },
        )
        save()
    }

    private fun openBookmarksDb(path: Path = Path("bookmarks.json")): BookmarksDB3 {
        if (!path.exists()) {
            return BookmarksDB3(emptyList())
        }
        val string = path.readText().trim()
        if (string[0] == '[') {
            // legacy DB1 format
            val db1 = json.decodeFromString<List<String>>(string)
            return BookmarksDB1(db1).migrate()
        }
        return when (val version = json.decodeFromString<Versioned>(string).version) {
            1 -> json.decodeFromString<BookmarksDB1>(string).migrate()
            2 -> json.decodeFromString<BookmarksDB2>(string).migrate()
            3 -> json.decodeFromString<BookmarksDB3>(string).migrate()
            else -> throw IllegalStateException("Unsupported BookmarksDB version: $version")
        }
    }

    private fun BookmarksDB3.save(path: Path = Path("bookmarks.json")) {
        val jsonString = json.encodeToString(this)
        path.writeText(jsonString)
    }

    @Serializable
    private open class Versioned(val version: Int)

    @Serializable
    private data class BookmarksDB1(val bookmarks: List<String>) : Versioned(1)

    @Serializable
    private data class BookmarksDB2(val bookmarks: List<BookmarkItem2>) : Versioned(2)

    @Serializable
    private data class BookmarksDB3(val bookmarks: List<BookmarkItem3>) : Versioned(3)

    @Serializable
    private data class BookmarkItem2(val id: String, val bookmarkedAt: Long)

    @Serializable
    private data class BookmarkItem3(val id: String, val bookmarkedAt: Long, val isBookmarked: Boolean, val colour: String? = null)

    private fun BookmarksDB1.migrate(): BookmarksDB3 = BookmarksDB2(
        bookmarks = bookmarks.map {
            BookmarkItem2(it, Instant.now().epochSecond)
        },
    ).migrate()

    private fun BookmarksDB2.migrate(): BookmarksDB3 = BookmarksDB3(
        bookmarks = bookmarks.map {
            BookmarkItem3(it.id, it.bookmarkedAt, true)
        },
    ).migrate()

    private fun BookmarksDB3.migrate(): BookmarksDB3 = this
}
