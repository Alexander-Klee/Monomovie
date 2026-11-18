package de.amklee.monomovie.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed interface JsCall {
    @Serializable data class SetupSse(val mode: SseEvent.Mode) : JsCall

    fun toJsString(): String = "window.monomovie(${Json.encodeToString(this)});"
}
