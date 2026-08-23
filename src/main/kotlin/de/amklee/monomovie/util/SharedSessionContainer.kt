package de.amklee.monomovie.util

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.schedule
import kotlin.time.Duration

private val LOG = System.getLogger("SharedSessionContainer")

class SharedSessionContainer<K, V>(private val sessionTimeout: Duration, private val construct: (K) -> V) {
	private val timer = Timer("SessionCleanupTimer", true)
	private val sessions: MutableMap<K, SessionEntry<V>> = ConcurrentHashMap()

	fun preheat(key: K, construct: () -> V = { construct(key) }, update: (V) -> Unit = {}): V = sessions
		.compute(key) { _, oldSession ->
			val value =
				oldSession?.let {
					it.timerTask.cancel()
					update(it.value)
					it.value
				} ?: construct()
			SessionEntry(
				value = value,
				references = oldSession?.references ?: 0,
				timerTask = cleanupTask(key),
			)
		}!!
		.value

	fun reheat(key: K) {
		sessions.computeIfPresent(key) { _, session ->
			session.timerTask.cancel()
			session.copy(timerTask = cleanupTask(key))
		}
	}

	fun <R> withValue(key: K, construct: () -> V = { construct(key) }, update: (V) -> Unit = {}, action: (V) -> R): R {
		val value = obtain(key, construct, update)
		try {
			return action(value)
		} finally {
			release(key, value)
		}
	}

	suspend fun <R> withValueSuspend(key: K, construct: () -> V = { construct(key) }, update: (V) -> Unit = {}, action: suspend (V) -> R): R {
		val value = obtain(key, construct, update)
		try {
			return action(value)
		} finally {
			release(key, value)
		}
	}

	private fun obtain(key: K, construct: () -> V, update: (V) -> Unit): V = sessions
		.compute(key) { _, oldSession ->
			val value =
				oldSession?.let {
					it.timerTask.cancel()
					update(it.value)
					it.value
				} ?: construct()
			SessionEntry(
				value = value,
				references = oldSession?.references?.plus(1) ?: 1,
				timerTask = cleanupTask(key),
			)
		}!!
		.value

	private fun release(key: K, value: V) {
		sessions.computeIfPresent(key) { _, session ->
			if (session.value != value) {
				LOG.warn {
					"Session value for key $key has changed during action execution, skipping cleanup"
				}
				session
			} else {
				session.copy(references = session.references - 1)
			}
		}
	}

	private fun cleanupTask(key: K) = timer.schedule(sessionTimeout.inWholeMilliseconds + 100) {
		cleanup(key)
	}

	private fun cleanup(key: K) {
		sessions.computeIfPresent(key) { _, session ->
			if (session.references > 0) {
				session.copy(timerTask = cleanupTask(key))
			} else {
				null
			}
		}
	}

	private data class SessionEntry<V>(val value: V, val references: Int, val timerTask: TimerTask)
}
