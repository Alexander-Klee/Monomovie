pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("deps")
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

includeBuild("build-logic")

rootProject.name = "monomovie"

include(
    "shared",
    "web",
    "server",
)
