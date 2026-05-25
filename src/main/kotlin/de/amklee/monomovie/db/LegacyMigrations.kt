package de.amklee.monomovie.db

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.r2dbc.insert
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

suspend fun performLegacyMigration() {
    for ((id_, bookmarkedAt_, isBookmarked_, colour_) in openBookmarksDb().bookmarks) {
        Bookmarks.insert {
            it[id] = id_
            it[bookmarkedAt] = bookmarkedAt_
            it[isBookmarked] = isBookmarked_
            it[colour] = colour_
        }
    }
    for ((id_, watchedAt_) in openWatchedDb().watched) {
        Watched.insert {
            it[id] = id_
            it[watchedAt] = watchedAt_
        }
    }
}

private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
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

private fun openWatchedDb(path: Path = Path("watched.json")): WatchedDB1 {
    if (!path.exists()) {
        return WatchedDB1(emptyList())
    }
    val string = path.readText().trim()
    return when (val version = json.decodeFromString<Versioned>(string).version) {
        1 -> json.decodeFromString<WatchedDB1>(string).migrate()
        else -> throw IllegalStateException("Unsupported WatchedDB version: $version")
    }
}

private fun BookmarksDB1.migrate(): BookmarksDB3 {
    return BookmarksDB2(bookmarks = bookmarks.map { BookmarkItem2(it, Instant.now().epochSecond) }).migrate()
}
private fun BookmarksDB2.migrate(): BookmarksDB3 {
    return BookmarksDB3(bookmarks = bookmarks.map { BookmarkItem3(it.id, it.bookmarkedAt, true) }).migrate()
}
private fun BookmarksDB3.migrate(): BookmarksDB3 = this

private fun WatchedDB1.migrate(): WatchedDB1 = this

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
private data class BookmarkItem2(
    val id: String,
    val bookmarkedAt: Long
)

@Serializable
private data class BookmarkItem3(
    val id: String,
    val bookmarkedAt: Long,
    val isBookmarked: Boolean,
    val colour: String? = null
)

@Serializable
private data class WatchedDB1(
    val watched: List<WatchedItem1>
) : Versioned(1)

@Serializable
private data class WatchedItem1(
    val id: String,
    val watchedAt: Long
)
