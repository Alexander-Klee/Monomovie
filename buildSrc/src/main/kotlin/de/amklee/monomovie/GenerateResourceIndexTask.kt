package de.amklee.monomovie

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.useDirectoryEntries

abstract class GenerateResourceIndexTask : DefaultTask() {
    @get:InputDirectory
    abstract val resourcesDir: DirectoryProperty

    @get:OutputDirectory
    abstract val generatedKotlinDir: DirectoryProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val stringExtensions: ListProperty<String>

    init {
        resourcesDir.convention(project.layout.projectDirectory.dir("src/main/resources"))
        generatedKotlinDir.convention(project.layout.buildDirectory.dir("generated/resourceIndex"))
        packageName.convention("de.amklee.monomovie")
        stringExtensions.convention(listOf("js", "svg", "css"))

        description = "Generates a Kotlin file containing all resources in the resources directory"
        group = "build"
    }

    @TaskAction
    fun main() {
        FileSpec.builder(ClassName(packageName.get(), "R"))
            .addType(
                TypeSpec.objectBuilder("R")
                    .apply {
                        val origin = resourcesDir.asFile.get().toPath()
                        handle(this, origin, origin)
                    }
                    .build()
            ).build()
            .writeTo(generatedKotlinDir.get().asFile)
    }

    fun handle(builder: TypeSpec.Builder, origin: Path, current: Path) {
        current.useDirectoryEntries {
            it.sortedBy { it.fileName.toString() }
                .forEach { entry ->
                    when {
                        entry.isRegularFile() -> {
                            if (!stringExtensions.get().contains(entry.extension)) return@forEach
                            builder.addProperty(
                                PropertySpec.builder(entry.asFilePropName(), String::class)
                                    .delegate(
                                        "%T(%S)",
                                        ClassName("de.amklee.monomovie.util", "Resource"),
                                        origin.relativize(entry).toString()
                                    )
                                    .build()
                            )
                        }

                        entry.isDirectory() -> builder.addType(
                            TypeSpec.objectBuilder(entry.fileName.toString())
                                .apply { handle(this, origin, entry) }
                                .build()
                        )

                        else -> throw IllegalStateException("Unknown entry type: $entry")
                    }
            }
        }
    }

    private fun Path.asFilePropName(): String = buildString {
        fileName
            .toString()
            .splitToSequence('-', '_', '.')
            .forEach {
                append(
                    if (isNotEmpty()) it.uppercaseFirst()
                    else it
                )
            }
    }

    private fun String.uppercaseFirst() = replaceFirstChar { it.uppercase() }
}