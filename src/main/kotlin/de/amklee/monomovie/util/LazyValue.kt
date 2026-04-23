package de.amklee.monomovie.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LazyValue<T>(private val initializer: suspend () -> T) {
    private var _value: T? = null
    private val mutex = Mutex()

    suspend fun get(): T {
        _value?.let { return it }
        return mutex.withLock {
            if (_value == null) {
                _value = initializer()
            }
            _value!!
        }
    }
}
