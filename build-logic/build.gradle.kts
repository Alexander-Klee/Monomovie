import org.gradle.api.internal.artifacts.DefaultModuleIdentifier.newId
import org.gradle.api.internal.artifacts.dependencies.DefaultMinimalDependency

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

val Provider<PluginDependency>.lib get() = map { DefaultMinimalDependency(
    newId(it.pluginId, "${it.pluginId}.gradle.plugin"),
    it.version as MutableVersionConstraint
) }

dependencies {
    implementation(libs.kotlinpoet)
    implementation(libs.closure.compiler)
    implementation(libs.plugins.kotlin.jvm.lib)
    implementation(libs.plugins.foojay.resolver.convention.lib)
}

kotlin.sourceSets.main { kotlin.srcDir(tasks.register("generateKotlin") {
    val output = layout.buildDirectory.dir("generated/source")
    inputs.property("ktor", libs.ktor.version.catalog)
    inputs.property("ktlint", libs.ktlint.cli)
    outputs.dir(output)
    doLast {
        output.get().asFile.resolve("specs.kt").writeText("""
            object Spec {
                val ktorVersionCatalog = "${libs.ktor.version.catalog.get()}"
                val ktlintCli = "${libs.ktlint.cli.get()}"
            }
        """.trimIndent())
    }
}) }
