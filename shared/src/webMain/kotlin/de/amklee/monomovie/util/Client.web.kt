package de.amklee.monomovie.util

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.*

actual typealias EngineConfig = JsClientEngineConfig
actual typealias EngineFactory = Js

actual fun HttpClientConfig<EngineConfig>.configureDefaults() {
}
