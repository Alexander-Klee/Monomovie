package de.amklee.monomovie

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.java.*
import java.net.http.HttpClient.Version as HttpClientVersion

actual typealias EngineConfig = JavaHttpConfig
actual typealias EngineFactory = Java

actual fun HttpClientConfig<EngineConfig>.configureDefaults() {
    engine {
        protocolVersion = HttpClientVersion.HTTP_2
    }
}
