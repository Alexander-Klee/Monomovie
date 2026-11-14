package de.amklee.monomovie.util

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NIHCache<T : Any>(private val maxAge: Duration, private val fetch: suspend () -> T) {
    private var cache: T? = null
    private var lastAccessed = Instant.fromEpochSeconds(0)
    private val mutex = Mutex()

    suspend fun get(): T {
        if (Clock.System.now() - lastAccessed < maxAge) {
            return cache!!
        }
        return mutex.withLock {
            if (Clock.System.now() - lastAccessed < maxAge) {
                return cache!!
            }
            val newValue = fetch()
            cache = newValue
            lastAccessed = Clock.System.now()
            newValue
        }
    }
}
