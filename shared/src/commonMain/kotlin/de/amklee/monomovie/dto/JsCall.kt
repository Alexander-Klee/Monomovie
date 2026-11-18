package de.amklee.monomovie.dto

import kotlinx.serialization.Serializable

@Serializable
sealed interface JsCall {
    @Serializable data class SetupSse(val mode: SseEvent.Mode) : JsCall
}
