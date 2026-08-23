plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("com.squareup:kotlinpoet:2.3.0")
    implementation("com.google.javascript:closure-compiler:v20260513")
}
