plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

buildscript {
    repositories {
        google()
    }

    dependencies {
        classpath("com.google.javascript:closure-compiler:v20260513")
    }
}

rootProject.name = "monomovie"
