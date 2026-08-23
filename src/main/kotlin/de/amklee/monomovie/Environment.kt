package de.amklee.monomovie

import de.amklee.monomovie.util.error
import de.amklee.monomovie.util.warn
import io.ktor.util.PlatformUtils

object Environment {
	private val log = System.getLogger("MMV/Environment")

	val isDevelopment: Boolean get() = PlatformUtils.IS_DEVELOPMENT_MODE

	val hostname =
		System.getenv("MMV_HOSTNAME") ?: run {
			if (isDevelopment) {
				log.warn {
					"MMV_HOSTNAME not set, defaulting to http://localhost:8080 for development environment"
				}
			} else {
				val msg = "MMV_HOSTNAME environment variable must be set in production environment"
				log.error { msg } // might not get logged otherwise
				throw IllegalStateException(msg)
			}
			"http://localhost:8080"
		}

	val jellyfinHost = System.getenv("MMV_JELLYFIN_HOST")?.ifBlank { null }
	val jellyfinToken = System.getenv("MMV_JELLYFIN_TOKEN")?.ifBlank { null }

	init {
		if (jellyfinHost.isNullOrBlank() || jellyfinToken.isNullOrBlank()) {
			log.warn { "Jellyfin credentials not set, Jellyfin integration will be disabled" }
		}
	}
}
