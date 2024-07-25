plugins {
    id("com.rtm516.mcxboxbroadcast.shadow-conventions")
}

relocate("org.yaml.snakeyaml")
relocate("com.fasterxml.jackson")
relocate("com.google.gson")
relocate("net.raphimc.minecraftauth")

dependencies {
    api(project(":core"))
    api(libs.bundles.jackson)
    compileOnly(libs.bundles.geyser)
}

sourceSets {
    main {
        blossom {
            val info = GitInfo(indraGit)
            resources {
                property("version", info.version)
            }
        }
    }
}

nameJar("MCXboxBroadcastExtension")

description = "bootstrap-geyser"
