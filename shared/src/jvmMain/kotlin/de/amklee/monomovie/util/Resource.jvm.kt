package de.amklee.monomovie.util

import java.io.FileNotFoundException

actual data class Resource actual constructor(actual val path: String) {
    val resource: String by lazy {
        Resource::class.java.getResource("/$path")?.readText()
            ?: throw FileNotFoundException("$path not found in resources")
    }
}
