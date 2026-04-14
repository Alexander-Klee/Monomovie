import java.util.*
import kotlin.experimental.xor

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

private fun unsalt(data: String, salt: LongArray) = salt.map { Random(it shl 3 or 12) }.run { Base64.getDecoder().decode(data).mapIndexed { l, r -> ByteArray(5).let { get((l + 3 shr 5).mod(size - (l % 2))).nextBytes(it); it[l % 4] } xor r } }.toByteArray().let { dm -> dm.decodeToString(dm[11].toUByte().toInt() % 65, dm.size - dm[12].toUByte().toInt() % dm[11].toUByte().toInt(), false) }.replace('\uFFFD', '1')
repositories {
    maven("https://maven.pkg.github.com/JFronny/kotlinx.html") {
        credentials {
            username = ""
            password = unsalt("MsUHMHvj/UIdFMSOMTkYborAekxPK52/EB4bp2Kxmd+cShtWiO6xXeMJp1NNIvBhAcNvH35gG2/uh9DRCs8+mf3YzyIIarY+1hTSOW6BAi3tkqOxDmNfU4bbKKj/M/JpmSKnafLJYcdMH1ASRvEWuCA=", longArrayOf(-1344705729828745711, 5230045263062089707, 3545512903530151910, 7538809493429025070, -2925924544857994240, -8595937774785329809, -4208388178093363894, 6992710797411217798, -6389370378960172826, 3591822878033896109, 8889865935792943001, -4262397429266853753, 2298705730591068518, 4714639302703995747, -7464986330344552584, -7518779346602212671, -7579240134292203452, 2373566333070360596, 6000643398013606382, 1307504402323163593, 3915882559778035436, -8081209879159995769, -9207588343828866839, -3787429060347275080, 6273897675385322987, 288272166847093320, 6835607591775321015, -6203230303138578259, -4541277508978494093, 2065286167320721702, -2261015450204600435, -9132305004876616378, -7041677951324361252, 5891454128313438131, 3367594326194710532, 7273550992031740203, 980246378010968093, -145285380090142264, 5638755824395442070, -4662549621614845308, -8319182660990689705, 4593349031178611454, 9188854271361800229, 3389677104980428669, -1826074559192361909, -991538057649777867, 6767921920810646389, 425275679354278601, -1129287175256942626, 3246752065518662417, -3173336438845015123, 324568664530661404))
        }
        content {
            includeModule("org.jetbrains.kotlinx", "kotlinx-html")
            includeModule("org.jetbrains.kotlinx", "kotlinx-html-jvm")
        }
    }
    mavenCentral()
    maven("https://maven.frohnmeyer-wds.de/artifacts")
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
        dependsOn(runtime)
        group = "application"
        executable = project.runtime.imageDir.file("bin/monomovie").get().asFile.absolutePath
        environment("MMV_HOSTNAME" to "http://localhost:8080")
    }
}
