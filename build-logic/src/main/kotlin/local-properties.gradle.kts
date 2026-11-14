import org.jetbrains.kotlin.gradle.plugin.extraProperties
import java.util.Properties

val localProperties = Properties()

generateSequence(project) { it.parent }
    .forEach {
        val localPropertiesFile = it.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }
    }

localProperties.forEach { (k, v) -> extraProperties[k.toString()] = v }
