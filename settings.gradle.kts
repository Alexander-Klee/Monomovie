pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("deps")
}

includeBuild("build-logic")

rootProject.name = "monomovie"
