package de.amklee.monomovie

import org.gradle.api.tasks.Exec
import org.gradle.internal.jvm.Jvm
import java.security.SecureRandom
import java.util.Properties
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists

abstract class SetupSelfSignedCertificateTask : Exec() {
    private val storeFile = project.rootProject.layout.buildDirectory.file("keystore.p12").get().asFile.absolutePath
    private val keyAlias = "monomovie"

    init {
        group = "application"
        executable = Jvm.current().javaHome.resolve("bin/keytool").absolutePath

        args(
            "-keystore", storeFile,
            "-storetype", "PKCS12",
            "-alias", keyAlias,
            "-genkeypair",
            "-keyalg", "RSA",
            "-keysize", "4096",
            "-validity", "3",
            "-dname", "CN=localhost, OU=ktor, O=ktor, L=Unspecified, ST=Unspecified, C=US",
            "-noprompt",
        )

        workingDirectory.set(project.rootProject.layout.buildDirectory)
    }

    override fun exec() {
        val random = SecureRandom()
        val keyStorePassword = ByteArray(16)
            .also { random.nextBytes(it) }
            .joinToString("") { "%02x".format(it) }

        args(
            "-storepass", keyStorePassword,
            "-keypass", keyStorePassword,
        )

        Path(storeFile).deleteIfExists()

        super.exec()

        val localProperties = Properties()
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) localPropertiesFile.inputStream().use { localProperties.load(it) }

        localProperties["mmv.ssl.storeFile"] = storeFile
        localProperties["mmv.ssl.keyAlias"] = keyAlias
        localProperties["mmv.ssl.keyStorePassword"] = keyStorePassword
        localProperties["mmv.ssl.privateKeyPassword"] = keyStorePassword

        localPropertiesFile.outputStream().use { localProperties.store(it, null) }
    }
}
