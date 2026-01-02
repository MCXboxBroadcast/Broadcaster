plugins {
    id("com.rtm516.mcxboxbroadcast.shadow-conventions")
    id("com.rtm516.mcxboxbroadcast.modrinth-uploading-conventions")
}

relocate("org.yaml.snakeyaml")
relocate("org.spongepowered.configurate")
relocate("com.google.gson")
relocate("net.raphimc.minecraftauth")
relocate("org.bouncycastle")
relocate("net.lenni0451.commons.httpclient")

configurations.all {
    resolutionStrategy {
        // Force our version of MinecraftAuth to prevent using v5 from Geyser
        force(libs.minecraftauth)
    }
}

dependencies {
    api(project(":core"))
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

modrinth {
    uploadFile.set(tasks.getByPath("shadowJar"))
}

nameJar("MCXboxBroadcastExtension")

description = "bootstrap-geyser"
