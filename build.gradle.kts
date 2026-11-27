plugins {
    `local-properties`
    alias(libs.plugins.version.catalog.update)
}

group = "de.amklee"
version = "1.0-SNAPSHOT"

val server = project(projects.server.path)
tasks.register<Copy>("runtime") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(server.tasks.named("runtime")) {
        exclude("legal/**")
    }
    into(layout.buildDirectory.dir("image"))
}
