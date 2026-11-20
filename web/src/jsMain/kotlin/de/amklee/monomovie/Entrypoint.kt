package de.amklee.monomovie

import de.amklee.monomovie.dto.JsCall
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromDynamic

@OptIn(ExperimentalJsExport::class, ExperimentalSerializationApi::class)
@JsExport
fun entrypoint(call: dynamic) {
    Json.decodeFromDynamic<JsCall>(call).dispatch()
}
