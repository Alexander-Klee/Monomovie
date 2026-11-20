package de.amklee.monomovie.util

actual fun Float.toString(precision: Int): String = "%.${precision}f".format(this)
