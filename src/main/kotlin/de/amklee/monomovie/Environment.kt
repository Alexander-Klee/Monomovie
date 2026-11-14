package de.amklee.monomovie

import de.amklee.monomovie.util.error
import de.amklee.monomovie.util.warn
import io.ktor.util.PlatformUtils
import java.nio.file.Path
import java.time.ZoneId
import kotlin.io.path.Path

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

    val timezone = ZoneId.of(System.getenv("TZ") ?: "Europe/Berlin")

    val jellyfinHost = System.getenv("MMV_JELLYFIN_HOST")?.ifBlank { null }
    val jellyfinToken = System.getenv("MMV_JELLYFIN_TOKEN")?.ifBlank { null }

    val ssl = run {
        val storeFile = System.getenv("MMV_SSL_STOREFILE")?.ifBlank { null }
        val keyAlias = System.getenv("MMV_SSL_KEYALIAS")?.ifBlank { null }
        val keyStorePassword = System.getenv("MMV_SSL_KEYSTORE_PASSWORD")?.ifBlank { null }
        val privateKeyPassword = System.getenv("MMV_SSL_PRIVATEKEY_PASSWORD")?.ifBlank { null }
        if (storeFile != null) {
            if (keyAlias == null || keyStorePassword == null || privateKeyPassword == null) {
                log.warn { "SSL key alias, key store password or private key password not set, SSL will be disabled" }
                null
            } else {
                SSL(Path(storeFile), keyAlias, keyStorePassword, privateKeyPassword)
            }
        } else {
            null
        }
    }

    data class SSL(val storeFile: Path, val keyAlias: String, val keyStorePassword: String, val privateKeyPassword: String)

    init {
        if (jellyfinHost.isNullOrBlank() || jellyfinToken.isNullOrBlank()) {
            log.warn { "Jellyfin credentials not set, Jellyfin integration will be disabled" }
        }
    }
}
