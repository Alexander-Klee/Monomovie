plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

val ktlint = configurations.register("ktlint")

dependencies {
    ktlint(Spec.ktlintCli)
}

val outputDir = project.layout.buildDirectory.dir("reports/ktlint/")
val inputFiles = fileTree("src") { include("**/*.kt") } + fileTree("buildSrc/src") { include("**/*.kt") }
val editorconfig = rootProject.file(".editorconfig").absolutePath

tasks {
    val ktlintRun = register("ktlintRun", JavaExec::class) {
        group = "verification"
        inputs.files(inputFiles)
        outputs.dir(outputDir)
        mainClass = "com.pinterest.ktlint.Main"
        classpath(ktlint)
        args = listOf("--editorconfig=$editorconfig", "src/**/*.kt", "buildSrc/src/**/*.kt", "--reporter=plain?group_by_file")
        jvmArgs = listOf("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    }

    val ktlintFormat = register("ktlintFormat", JavaExec::class) {
        group = "verification"
        inputs.files(inputFiles)
        outputs.dir(outputDir)
        mainClass = "com.pinterest.ktlint.Main"
        classpath(ktlint)
        args = listOf("--editorconfig=$editorconfig", "-F", "src/**/*.kt", "buildSrc/src/**/*.kt")
        jvmArgs = listOf("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    }

    val lint = register("lint") {
        group = "verification"
        dependsOn(ktlintRun)
    }

    check { dependsOn(lint) }
    compileKotlin { dependsOn(ktlintFormat) }
}
