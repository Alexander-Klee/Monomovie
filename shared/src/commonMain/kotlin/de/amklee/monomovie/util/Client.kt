package de.amklee.monomovie.util

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect class EngineConfig : HttpClientEngineConfig
expect object EngineFactory : HttpClientEngineFactory<EngineConfig> {
    override fun create(block: EngineConfig.() -> Unit): HttpClientEngine
}

expect fun HttpClientConfig<EngineConfig>.configureDefaults()

@Suppress("FunctionName")
fun MmvHttpClient(block: HttpClientConfig<EngineConfig>.() -> Unit = {}) = HttpClient(EngineFactory) {
    configureDefaults()
    engine {
        pipelining = true
    }
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            },
        )
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 10000 // Set a timeout for requests
        socketTimeoutMillis = 10000 // Set a timeout for sockets
        connectTimeoutMillis = 10000 // Set a timeout for connections
    }
    block()
}
