package de.amklee.monomovie.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed interface JsCall {
    @Serializable
    @SerialName("setupSse")
    data class SetupSse(val mode: SseEvent.Mode) : JsCall

    @Serializable
    @SerialName("setBookmarked")
    data class SetBookmarked(val movieId: String, val bookmark: Boolean) : JsCall

    @Serializable
    @SerialName("setWatched")
    data class SetWatched(val movieId: String, val watched: Boolean) : JsCall

    fun toJsString(): String = "window.monomovie(${Json.encodeToString(this)});"
}
