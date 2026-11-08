plugins {
    org.gradle.toolchains.`foojay-resolver-convention`
}

dependencyResolutionManagement {
    repositories.mavenCentral()
    versionCatalogs.create("ktorLibs").from(Spec.ktorVersionCatalog)
}
