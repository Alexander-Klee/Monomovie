import de.amklee.monomovie.GenerateResourceIndexTask
import de.amklee.monomovie.githubMaven
import de.amklee.monomovie.shrunkel.CssFilter
import de.amklee.monomovie.shrunkel.JsFilter
import de.amklee.monomovie.shrunkel.SvgFilter

plugins {
    org.jetbrains.kotlin.jvm
    ktlint
    application
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.shadow)
    alias(libs.plugins.jlink.runtime)
    alias(libs.plugins.version.catalog.update)
}

group = "de.amklee"
version = "1.0-SNAPSHOT"

application {
    mainClass = "de.amklee.monomovie.MainKt"
}

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            githubMaven("JFronny/kotlinx.html")
        }
        filter {
            includeModule("org.jetbrains.kotlinx", "kotlinx-html")
            includeModule("org.jetbrains.kotlinx", "kotlinx-html-jvm")
        }
    }
}

dependencies {
    // client for JustWatch API
    implementation(ktorLibs.client.core)
    implementation(ktorLibs.client.java)
    implementation(ktorLibs.client.contentNegotiation)
    implementation(libs.slf4j.jdk14)

    // Jellyfin SDK
    implementation(libs.jellyfin.core)

    // server for MonoMovie
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.cio)
    implementation(ktorLibs.server.statusPages)
    implementation(ktorLibs.server.sse)
    implementation(libs.kotlinx.html)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(25)
}

runtime {
    addOptions("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")
    addModules("java.logging", "java.net.http")
    enableCds()
    launcher {
        jvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
    }
}

tasks {
    processResources {
        filesMatching("**/*.js") {
            filter(JsFilter::class, mapOf("sourceName" to sourceName))
        }
        filesMatching("**/*.css") {
            filter(CssFilter::class)
        }
        filesMatching("**/*.svg") {
            filter(SvgFilter::class)
        }
    }
    shadowJar {
        archiveFileName = "app.jar"
    }
    register<Exec>("runImage") {
        description = "Runs the application from the generated runtime image"
        dependsOn(runtime)
        group = "application"
        executable = project.runtime.imageDir.file("bin/monomovie").get().asFile.absolutePath
        environment("MMV_HOSTNAME" to "http://localhost:8080")
    }
}

kotlin.sourceSets.main {
    kotlin.srcDir(tasks.register("generateResourceIndex", GenerateResourceIndexTask::class))
}
