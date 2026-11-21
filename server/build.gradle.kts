import de.amklee.monomovie.*
import de.amklee.monomovie.shrunkel.*

plugins {
    `local-properties`
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
    implementation(ktorLibs.server.netty)
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
    test {
        useJUnitPlatform()
    }
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

    val runImage = register<Exec>("runImage") {
        description = "Runs the application from the generated runtime image"
        dependsOn(runtime)
        group = "application"
        executable = project.runtime.imageDir.file("bin/monomovie").get().asFile.absolutePath
    }
    listOf(run, runImage, runShadow).forEach { it {
        val storeFile = prop("mmv.ssl.storeFile", "")
        environment("MMV_HOSTNAME" to if (storeFile.isBlank()) "http://localhost:8080" else "https://localhost:8443")
        environment("MMV_SSL_HOST", prop("mmv.ssl.host", ""))
        environment("MMV_SSL_STOREFILE", storeFile)
        environment("MMV_SSL_KEYALIAS", prop("mmv.ssl.keyAlias", ""))
        environment("MMV_SSL_KEYSTORE_PASSWORD", prop("mmv.ssl.keyStorePassword", ""))
        environment("MMV_SSL_PRIVATEKEY_PASSWORD", prop("mmv.ssl.privateKeyPassword", ""))
        workingDirectory = rootProject.layout.projectDirectory
    } }

    register<SetupSelfSignedCertificateTask>("setupSelfSignedCertificate")

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
        "jsBrowser${profile}LibraryDistribution"
    })
kotlin.sourceSets.main {
    kotlin.srcDir(tasks.register<GenerateResourceIndexTask>("generateResourceIndex"))
    resources.srcDir(tasks.register<Copy>("copyWebResources") {
        group = "build"
        dependsOn(libraryDistribution)
        from(libraryDistribution) {
            into("client")
        }
        into(layout.buildDirectory.dir("web/resources"))
    })
}
