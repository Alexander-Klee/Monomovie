package de.amklee.monomovie.util

import de.amklee.monomovie.R
import de.amklee.monomovie.components.Mode
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun R.sseJs(mode: Mode) = sseJs.replace($$"$mode$", mode.toString())

fun R.selectableJs(minSelection: Int) =
	selectableJs.replace($$"$minSelection$", minSelection.toString())

@OptIn(ExperimentalUuidApi::class)
fun R.rouletteSharedJs(shareId: Uuid) = rouletteSharedJs.replace($$"$shareId$", shareId.toString())
