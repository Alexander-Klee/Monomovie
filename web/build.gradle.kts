import de.amklee.monomovie.*
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    `local-properties`
    org.jetbrains.kotlin.multiplatform
    ktlint
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.plugin.js.plain.objects)
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
    kotlinxHtml()
}

kotlin {
    js {
        browser()
        binaries.library()
        useEsModules()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(libs.kotlinx.html)
            implementation(libs.kotlinx.browser)
        }
    }
}
