plugins {
    kotlin("jvm") version "2.2.0"
}

group = "de.amklee"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.frohnmeyer-wds.de/artifacts")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.gitlab.jfronny:commons-logger:1.8.0-SNAPSHOT")
    implementation("io.gitlab.jfronny:slf4j-over-jpl:1.8.0-SNAPSHOT")
    implementation("io.ktor:ktor-client-core:3.2.0")
    implementation("io.ktor:ktor-client-cio:3.2.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.2.0")
    implementation("io.ktor:ktor-serialization-gson:3.2.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}