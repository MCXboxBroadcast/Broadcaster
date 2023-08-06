plugins {
    id("com.rtm516.mcxboxbroadcast.shadow-conventions")
}

relocate("org.yaml.snakeyaml")
relocate("com.fasterxml.jackson")

dependencies {
    api(project(":core"))
    api(libs.bundles.jackson.yaml)
    compileOnly(libs.bundles.geyser)
    compileOnly(libs.floodgate.spigot) {
        exclude("dev.folia")
        exclude("com.mojang")
    }
}

nameJar("MCXboxBroadcastExtension")

description = "bootstrap-geyser"
