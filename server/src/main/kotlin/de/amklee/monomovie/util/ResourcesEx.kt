package de.amklee.monomovie.util

import de.amklee.monomovie.R
import kotlin.uuid.Uuid

fun R.rouletteSharedJs(shareId: Uuid) = rouletteSharedJs.replace($$"$shareId$", shareId.toString())
