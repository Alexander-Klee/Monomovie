import de.amklee.monomovie.GenerateResourceIndexTask
import de.amklee.monomovie.githubMaven

plugins {
    kotlin("jvm") version "2.3.20"
    application
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20"
    id("io.ktor.plugin") version "3.4.2"
    id("com.gradleup.shadow") version "9.4.1"
    id("org.beryx.runtime") version "2.0.1"
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
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-java")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("org.slf4j:slf4j-jdk14:2.0.17")

    // Jellyfin SDK
    implementation("org.jellyfin.sdk:jellyfin-core:1.8.8")

    // server for MonoMovie
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-cio")
    implementation("io.ktor:ktor-server-host-common")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-sse")
    implementation("org.jetbrains.kotlinx:kotlinx-html:0.12.0-jf.3")
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

val generateResourceIndex by tasks.registering(GenerateResourceIndexTask::class)

kotlin.sourceSets.main {
    kotlin.srcDir(generateResourceIndex)
}
