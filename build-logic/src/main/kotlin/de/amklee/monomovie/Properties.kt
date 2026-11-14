package de.amklee.monomovie

import org.gradle.api.Project

fun Project.prop(name: String): String = property(name).toString()

fun Project.prop(name: String, default: String): String =
    if (hasProperty(name)) prop(name)
    else default

fun Project.nprop(name: String): String? =
    if (hasProperty(name)) prop(name)
    else null
