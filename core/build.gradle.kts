plugins {
    id("com.rtm516.mcxboxbroadcast.java-conventions")
}

val nativePlatforms = listOf(
    "windows-x86_64",
    "windows-aarch64",
    "linux-x86_64",
    "linux-aarch64",
    "macos-x86_64",
    "macos-aarch64"
)

dependencies {
    api(libs.gson)
    api(libs.nimbus.jose.jwt)
    api(libs.java.websocket)
    api(libs.methanol)
    // api(libs.minecraftauth)
    api("com.github.RaphiMC:MinecraftAuth:3eb6e3a913")
    api(libs.bundles.protocol)
    api("dev.kastle.NetworkCompatible:netty-transport-nethernet:6a8915db93")

    api(libs.webrtc)
    nativePlatforms.forEach { platform ->
        runtimeOnly(libs.webrtc) {
            artifact {
                classifier = platform
            }
        }
    }

    api(libs.sqlite)

    annotationProcessor(libs.configurate.`interface`.ap)
    api(libs.configurate.`interface`)
    implementation(libs.configurate.yaml)
}

sourceSets {
    main {
        blossom {
            val info = GitInfo(indraGit)
            javaSources {
                property("version", info.version)
                property("gitVersion", info.gitVersion)
                property("buildNumber", info.buildNumber.toString())
                property("branch", info.branch)
                property("commit", info.commit)
                property("repository", info.repository)
            }
        }
    }
}

description = "core"
