package de.amklee.monomovie.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LazyValue<T>(private val initializer: suspend () -> T) {
	private var value: T? = null
	private val mutex = Mutex()

	suspend fun get(): T {
		value?.let { return it }
		return mutex.withLock {
			if (value == null) {
				value = initializer()
			}
			value!!
		}
	}
}
