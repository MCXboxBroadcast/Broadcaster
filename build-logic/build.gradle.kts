plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.shadow)
    implementation(libs.indra.git)
    implementation(libs.blossom)
    implementation(libs.minotaur)
}

