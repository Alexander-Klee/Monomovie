package de.amklee.monomovie

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.apply
import org.gradle.toolchains.foojay.FoojayToolchainsPlugin

abstract class DepsPlugin: Plugin<Settings> {
    override fun apply(target: Settings) {
        target.apply<FoojayToolchainsPlugin>()
        target.dependencyResolutionManagement {
            repositories.mavenCentral()
            versionCatalogs.create("ktorLibs").from(Spec.ktorVersionCatalog)
        }
    }
}