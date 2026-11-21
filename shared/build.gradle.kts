import de.amklee.monomovie.*
import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `local-properties`
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
    jvm()
    js {
        browser()
        useEsModules()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        useEsModules()
    }

    sourceSets {
        commonMain.dependencies {
            api(ktorLibs.serialization.kotlinx.json)
            api(libs.kotlinx.html)
            api(ktorLibs.client.core)
            api(ktorLibs.client.contentNegotiation)
        }
        jvmMain.dependencies {
            api(ktorLibs.client.java)
            implementation(libs.slf4j.jdk14)
        }
        webMain.dependencies {
            api(ktorLibs.client.js)
            api(libs.kotlinx.browser)
        }
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions.freeCompilerArgs.add("-Xexplicit-context-arguments")
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir(tasks.register<GenerateResourceIndexTask>("generateResourceIndex") {
        resourcesDir = projects.server.path.let(::project).layout.projectDirectory.dir("src/main/resources")
    })
}
