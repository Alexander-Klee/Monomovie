package de.amklee.monomovie

import de.amklee.monomovie.util.warn
import io.ktor.util.PlatformUtils

object Environment {
    private val log = System.getLogger("MMV/Environment")

    val isDevelopment: Boolean get() = PlatformUtils.IS_DEVELOPMENT_MODE

    val hostname = System.getenv("MMV_HOSTNAME") ?: run {
        if (isDevelopment) {
            log.warn { "MMV_HOSTNAME not set, defaulting to http://localhost:8080 for development environment" }
        } else {
            throw IllegalStateException("MMV_HOSTNAME environment variable must be set in production environment")
        }
        "http://localhost:8080"
    }

    val jellyfinHost = System.getenv("MMV_JELLYFIN_HOST")
    val jellyfinToken = System.getenv("MMV_JELLYFIN_TOKEN")

    init {
        if (jellyfinHost.isNullOrBlank() || jellyfinToken.isNullOrBlank()) {
            log.warn { "Jellyfin credentials not set, Jellyfin integration will be disabled" }
        }
    }
}
