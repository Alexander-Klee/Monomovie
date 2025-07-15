package de.amklee.monomovie.db

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object BookmarksDB {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private var bookmarksDB: BookmarksDB3 = openBookmarksDb()

    private fun save() = synchronized(this) {
        bookmarksDB.save()
    }

    operator fun contains(id: String): Boolean = bookmarksDB.bookmarks.any { it.id == id }
    fun isBookmarked(id: String): Boolean = bookmarksDB.bookmarks.any { it.id == id && it.isBookmarked }

    fun addBookmark(id: String) {
        if (contains(id)) {
            markBookmark(id)
            return
        }
        bookmarksDB = bookmarksDB
            .copy(bookmarks = bookmarksDB.bookmarks + listOf(
                BookmarkItem3(id, Instant.now().epochSecond, true)
            ))
        save()
    }

    fun markBookmark(id: String) {
        bookmarksDB = bookmarksDB.copy(
            bookmarks = bookmarksDB.bookmarks
                .map {
                    if (it.id == id) it.copy(isBookmarked = true)
                    else it
                }
        )
        save()
    }

    fun removeBookmark(id: String) {
        bookmarksDB = bookmarksDB.copy(
            bookmarks = bookmarksDB.bookmarks
                .map {
                    if (it.id == id) it.copy(isBookmarked = false)
                    else it
                }
        )
        save()
    }

    fun deleteBookmark(id: String) {
        bookmarksDB = bookmarksDB.copy(bookmarks = bookmarksDB.bookmarks.filterNot { it.id == id })
        save()
    }

    fun getBookmarks(): List<String> = bookmarksDB.bookmarks
        .filter { it.isBookmarked }
        .sortedByDescending { it.bookmarkedAt }
        .map { it.id }

    fun getAllBookmarks(): List<Pair<String, Boolean>> = bookmarksDB.bookmarks
        .sortedByDescending { it.bookmarkedAt }
        .sortedByDescending { it.isBookmarked }
        .map { it.id to it.isBookmarked }

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
        val version = json.decodeFromString<Versioned>(string).version
        return when (version) {
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
    private data class BookmarksDB1(
        val bookmarks: List<String>
    ) : Versioned(1)

    @Serializable
    private data class BookmarksDB2(
        val bookmarks: List<BookmarkItem2>
    ) : Versioned(2)

    @Serializable
    private data class BookmarksDB3(
        val bookmarks: List<BookmarkItem3>
    ) : Versioned(3)

    @Serializable
    private data class BookmarkItem3(
        val id: String,
        val bookmarkedAt: Long,
        val isBookmarked: Boolean
    )
    @Serializable
    private data class BookmarkItem2(
        val id: String,
        val bookmarkedAt: Long
    )

    private fun BookmarksDB1.migrate(): BookmarksDB3 {
        return BookmarksDB2(bookmarks = bookmarks.map { BookmarkItem2(it, Instant.now().epochSecond) }).migrate()
    }
    private fun BookmarksDB2.migrate(): BookmarksDB3 {
        return BookmarksDB3(bookmarks = bookmarks.map { BookmarkItem3(it.id, it.bookmarkedAt, true) }).migrate()
    }
    private fun BookmarksDB3.migrate(): BookmarksDB3 = this
}
