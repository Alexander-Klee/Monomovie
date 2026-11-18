package de.amklee.monomovie.util

import de.amklee.monomovie.R
import de.amklee.monomovie.dto.JsCall
import de.amklee.monomovie.dto.SseEvent
import kotlin.uuid.Uuid
import kotlinx.serialization.json.Json

fun R.sseJs(mode: SseEvent.Mode) = JsCall.SetupSse(mode).toJs()

fun R.selectableJs(minSelection: Int) = selectableJs.replace($$"$minSelection$", minSelection.toString())

fun R.rouletteSharedJs(shareId: Uuid) = rouletteSharedJs.replace($$"$shareId$", shareId.toString())

fun JsCall.toJs(): String = "window.monomovie(${Json.encodeToString(this)});"
