import de.amklee.monomovie.GenerateResourceIndexTask
import de.amklee.monomovie.githubMaven
import de.amklee.monomovie.shrunkel.CssFilter
import de.amklee.monomovie.shrunkel.JsFilter
import de.amklee.monomovie.shrunkel.SvgFilter

plugins {
    kotlin("jvm")
    ktlint
    application
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10"
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

val migrationTool by configurations.creating
val migrationsGen by sourceSets.creating {
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output + migrationTool
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output + migrationTool
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

    // Database
    implementation("org.jetbrains.exposed:exposed-core:1.4.0")
    implementation("org.jetbrains.exposed:exposed-r2dbc:1.4.0")
    migrationTool("org.jetbrains.exposed:exposed-migration-core:1.4.0")
    migrationTool("org.jetbrains.exposed:exposed-migration-r2dbc:1.4.0")
    implementation("com.h2database:h2:2.4.240")
    implementation("io.r2dbc:r2dbc-h2:1.1.0.RELEASE")
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
    val migrationsTmpDir = layout.buildDirectory.file("migrationsgen")
    register<JavaExec>("generateMigrations") {
        group = "application"
        mainClass = "de.amklee.monomovie.GenerateMigrationsKt"
        classpath(migrationsGen.runtimeClasspath)
        systemProperty("io.ktor.development", "true")
        args(migrationsTmpDir.get().asFile.resolve("db").absolutePath)
        doFirst {
            migrationsTmpDir.get().asFile.deleteRecursively()
            migrationsTmpDir.get().asFile.mkdirs()
        }
    }
}

kotlin.sourceSets.main {
    kotlin.srcDir(tasks.register("generateResourceIndex", GenerateResourceIndexTask::class))
}
