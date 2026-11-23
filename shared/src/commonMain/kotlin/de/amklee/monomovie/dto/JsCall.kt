package de.amklee.monomovie.dto

import kotlin.uuid.Uuid
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

    @Serializable
    @SerialName("scrollToTop")
    object InjectTopButton : JsCall

    @Serializable
    @SerialName("updateMovieSelectionCount")
    object UpdateMovieSelectionCount : JsCall

    @Serializable
    @SerialName("setupRouletteVoting")
    data class SetupRouletteVoting(val shareId: Uuid) : JsCall

    @Serializable
    @SerialName("setupInfiniteScroll")
    data class SetupSearchInfiniteScroll(val title: String) : JsCall

    fun toJsString(): String = "window.monomovie(${Json.encodeToString(this)});"
}
