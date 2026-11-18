import de.amklee.monomovie.*
import de.amklee.monomovie.shrunkel.*

plugins {
    org.jetbrains.kotlin.jvm
    ktlint
    application
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.shadow)
    alias(libs.plugins.jlink.runtime)
}

group = rootProject.group
version = rootProject.version

application {
    mainClass = "de.amklee.monomovie.MainKt"
}

repositories {
    mavenCentral()
    kotlinxHtml()
}

val migrationTool = configurations.create("migrationTool")
val migrationsGen = sourceSets.create("migrationsGen") {
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output + migrationTool
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output + migrationTool
}

dependencies {
    implementation(projects.shared)

    // Ktor Server
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.cio)
    implementation(ktorLibs.server.statusPages)
    implementation(ktorLibs.server.sse)
    implementation(libs.kotlinx.html)

    // Database
    implementation(exposedLibs.core)
    implementation(exposedLibs.r2dbc)
    implementation(exposedLibs.json)
    implementation(exposedLibs.kotlin.datetime)
    migrationTool(exposedLibs.migration.core)
    migrationTool(exposedLibs.migration.r2dbc)
    implementation(libs.h2)
    implementation(libs.r2dbc.h2)

    // Jellyfin SDK
    implementation(libs.jellyfin.core)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(25)
}

runtime {
    addOptions("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")
    addModules("java.logging", "java.net.http", "java.sql")
    enableCds()
    launcher {
        jvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
    }
}

tasks {
    processResources {
//        filesMatching("**/*.js") {
//            filter(JsFilter::class, mapOf("sourceName" to sourceName))
//        }
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

evaluationDependsOn(":web")
val libraryDistribution = project(projects.web.path)
    .tasks
    .named<Copy>(run {
        val profile = if (project.hasProperty("isProduction")) "Production" else "Development"
        val kind = if (project.hasProperty("isWasm")) "wasmJs" else "js"
        "${kind}Browser${profile}LibraryDistribution"
    })
kotlin.sourceSets.main {
    kotlin.srcDir(tasks.register("generateResourceIndex", GenerateResourceIndexTask::class))
    resources.srcDir(tasks.register("copyWebResources", Copy::class) {
        group = "build"
        dependsOn(libraryDistribution)
        from(libraryDistribution) {
            into("client")
        }
        into(layout.buildDirectory.dir("web/resources"))
    })
}
