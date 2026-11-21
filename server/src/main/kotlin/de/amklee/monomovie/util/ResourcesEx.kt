package de.amklee.monomovie.util

import de.amklee.monomovie.Environment
import de.amklee.monomovie.R
import kotlin.io.path.exists
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.reflect.KProperty
import kotlin.uuid.Uuid

val Resource.resourceFile
    get() = Path("src/main/resources/$path").takeIf {
        Environment.isDevelopment &&
            it.exists()
    }

fun Resource.read(): String = resourceFile?.readText() ?: resource

operator fun Resource.getValue(thisRef: Any?, property: KProperty<*>): String = read()

fun R.selectableJs(minSelection: Int) = selectableJs.read().replace($$"$minSelection$", minSelection.toString())

fun R.rouletteSharedJs(shareId: Uuid) = rouletteSharedJs.read().replace($$"$shareId$", shareId.toString())
