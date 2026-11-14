import de.amklee.monomovie.*
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    org.jetbrains.kotlin.multiplatform
    ktlint
    alias(libs.plugins.kotlin.plugin.serialization)
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

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
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
