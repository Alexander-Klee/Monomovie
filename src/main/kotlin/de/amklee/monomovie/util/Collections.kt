package de.amklee.monomovie.util

fun <T, K, V> Sequence<T>.associateColliding(transform: (T) -> Pair<K, V>): Map<K, V> {
    val map = mutableMapOf<K, V>()
    for (element in this) {
        val (key, value) = transform(element)
        require(key !in map) { "Duplicate key: $key" }
        map[key] = value
    }
    return map
}
