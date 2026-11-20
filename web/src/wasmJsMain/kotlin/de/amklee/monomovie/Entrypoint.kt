package de.amklee.monomovie

import de.amklee.monomovie.dto.JsCall
import kotlinx.serialization.json.Json

@OptIn(ExperimentalJsExport::class, ExperimentalWasmJsInterop::class)
@JsExport
fun entrypoint(call: JsAny) {
    Json.decodeFromString<JsCall>(toJson(call)).dispatch()
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun toJson(call: JsAny): String = js("JSON.stringify(call)")
