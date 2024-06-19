plugins {
    id("com.rtm516.mcxboxbroadcast.shadow-conventions")
}

relocate("org.yaml.snakeyaml")
relocate("com.fasterxml.jackson")
relocate("com.google.gson")

dependencies {
    api(project(":core"))
    api(libs.bundles.jackson)
    compileOnly(libs.bundles.geyser)
}

nameJar("MCXboxBroadcastExtension")

description = "bootstrap-geyser"
