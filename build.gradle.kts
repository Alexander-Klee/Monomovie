plugins {
    kotlin("jvm") version "2.2.0"
    application
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
    id("io.ktor.plugin") version "3.2.0"
    id("com.gradleup.shadow") version "8.3.6"
}

group = "de.amklee"
version = "1.0-SNAPSHOT"

application {
    mainClass = "de.amklee.monomovie.MainKt"
}

repositories {
    mavenCentral()
    maven("https://maven.frohnmeyer-wds.de/artifacts")
}

dependencies {
    implementation(platform("io.gitlab.jfronny:commons-bom:1.8.0-SNAPSHOT"))

    testImplementation(kotlin("test"))

    implementation("io.gitlab.jfronny:commons-logger")
    implementation("io.gitlab.jfronny:slf4j-over-jpl")

    // client for JustWatch API
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-content-negotiation")

    // server for MonoMovie
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-host-common")
    implementation("io.ktor:ktor-server-status-pages")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

tasks {
    shadowJar {
        archiveFileName = "app.jar"
    }
}