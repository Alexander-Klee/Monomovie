import org.gradle.api.internal.artifacts.DefaultModuleIdentifier.newId
import org.gradle.api.internal.artifacts.dependencies.DefaultMinimalDependency

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
}

val Provider<PluginDependency>.lib get() = map { DefaultMinimalDependency(
    newId(it.pluginId, "${it.pluginId}.gradle.plugin"),
    it.version as MutableVersionConstraint
) }

dependencies {
    implementation(libs.kotlinpoet)
    implementation(libs.closure.compiler)
    implementation(libs.plugins.kotlin.jvm.lib)
}
