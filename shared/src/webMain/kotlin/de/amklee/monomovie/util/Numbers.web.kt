package de.amklee.monomovie.util

actual fun Float.toString(precision: Int): String = toFixed(this, precision)

@OptIn(ExperimentalWasmJsInterop::class)
private fun toFixed(number: Float, precision: Int): String = js("number.toFixed(precision)")
