package de.amklee.monomovie

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.*

actual typealias EngineConfig = JsClientEngineConfig
actual typealias EngineFactory = Js

actual fun HttpClientConfig<EngineConfig>.configureDefaults() {
}
