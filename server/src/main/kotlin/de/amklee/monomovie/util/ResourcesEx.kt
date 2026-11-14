package de.amklee.monomovie.util

import de.amklee.monomovie.R
import de.amklee.monomovie.dto.SseEvent
import kotlin.uuid.Uuid

fun R.sseJs(mode: SseEvent.Mode) = "window.monomovie.setupSse('${mode.name}');"

fun R.selectableJs(minSelection: Int) = selectableJs.replace($$"$minSelection$", minSelection.toString())

fun R.rouletteSharedJs(shareId: Uuid) = rouletteSharedJs.replace($$"$shareId$", shareId.toString())
