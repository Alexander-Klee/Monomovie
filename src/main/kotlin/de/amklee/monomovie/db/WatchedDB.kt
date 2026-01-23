package de.amklee.monomovie.db

import de.amklee.monomovie.CachedMovies
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object WatchedDB {
    val eventFlow = MutableSharedFlow<Event>()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    data class WatchedItem(val item: CachedMovies.Movie, val watchedAt: LocalDateTime)

    private var watchedDB: WatchedDB1 = openWatchedDb()

    private fun save() = synchronized(this) {
        watchedDB.save()
    }

    operator fun contains(id: String): Boolean = watchedDB.watched.any { it.id == id }

    suspend fun setWatch(id: String) {
        if (contains(id)) {
            // maybe increment watch count or something
            return
        }
        watchedDB = watchedDB.copy(
            watched = watchedDB.watched + listOf(
            WatchedItem1(id, Instant.now().epochSecond)
        ))
        save()
        eventFlow.emit(Event.Added(id))
    }

    suspend fun deleteWatch(id: String) {
        watchedDB = watchedDB.copy(
            watched = watchedDB.watched.filter { it.id != id }
        )
        save()
        eventFlow.emit(Event.Removed(id))
    }

    suspend fun getWatched(): List<WatchedItem> = watchedDB.watched
        .sortedByDescending { it.watchedAt }
        .mapNotNull {
            WatchedItem(
                CachedMovies.get(it.id) ?: return@mapNotNull null,
                LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(it.watchedAt),
                    ZoneId.of("Europe/Berlin")
                )
            )
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

    private fun WatchedDB1.save(path: Path = Path("watched.json")) {
        val jsonString = json.encodeToString(this)
        path.writeText(jsonString)
    }

    @Serializable
    private open class Versioned(val version: Int)

    @Serializable
    private data class WatchedDB1(
        val watched: List<WatchedItem1>
    ) : Versioned(1)

    @Serializable
    private data class WatchedItem1(
        val id: String,
        val watchedAt: Long
    )

    private fun WatchedDB1.migrate(): WatchedDB1 = this
}
