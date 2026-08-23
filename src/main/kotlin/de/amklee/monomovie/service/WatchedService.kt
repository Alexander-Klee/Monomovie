package de.amklee.monomovie.service

import de.amklee.monomovie.db.Watched
import de.amklee.monomovie.db.get
import de.amklee.monomovie.db.monomovieDb
import java.time.Instant
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

object WatchedService {
    val eventFlow = MutableSharedFlow<Event>()

    suspend operator fun contains(id: String): Boolean = Watched[id] != null

    suspend fun setWatch(id: String) = suspendTransaction(monomovieDb) {
        if (contains(id)) {
            // maybe increment watch count or something
            return@suspendTransaction
        }
        Watched.insert {
            it[Watched.id] = id
            it[watchedAt] = Instant.now().epochSecond
        }
        eventFlow.emit(Event.Added(id))
    }

    suspend fun deleteWatch(id: String) = suspendTransaction(monomovieDb) {
        val modifiedCount = Watched.deleteWhere { Watched.id eq id }
        if (modifiedCount > 0) eventFlow.emit(Event.Removed(id))
    }

    suspend fun getWatched(): List<Watched.Item> = suspendTransaction(monomovieDb) {
        Watched.selectAll()
            .orderBy(Watched.watchedAt, SortOrder.DESC)
            .mapNotNull { Watched.Item(it) }
            .toList()
    }
}
