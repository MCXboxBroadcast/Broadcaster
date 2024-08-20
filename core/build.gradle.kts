plugins {
    id("com.rtm516.mcxboxbroadcast.java-conventions")
}

dependencies {
    api(libs.gson)
    api(libs.bundles.jackson)
    api(libs.nimbus.jose.jwt)
    api(libs.java.websocket)
    api(libs.methanol)
    api(libs.minecraftauth)

    implementation("dev.onvoid.webrtc:webrtc-java:0.8.0")
    implementation("dev.onvoid.webrtc", "webrtc-java", "0.8.0", classifier = "windows-x86_64")
    implementation("dev.onvoid.webrtc", "webrtc-java", "0.8.0", classifier = "macos-x86_64")
    implementation("dev.onvoid.webrtc", "webrtc-java", "0.8.0", classifier = "macos-aarch64")
    implementation("dev.onvoid.webrtc", "webrtc-java", "0.8.0", classifier = "linux-x86_64")
    implementation("dev.onvoid.webrtc", "webrtc-java", "0.8.0", classifier = "linux-aarch64")
    implementation("dev.onvoid.webrtc", "webrtc-java", "0.8.0", classifier = "linux-aarch32")

    api("org.jitsi:ice4j:3.0-72-g824cd4b")
    api("org.bouncycastle:bcprov-jdk18on:1.78.1")
    api("org.bouncycastle:bctls-jdk18on:1.78.1")
    api("org.bouncycastle:bcpkix-jdk18on:1.78.1")

    // Needs https://github.com/steely-glint/srtplight and https://github.com/pipe/sctp4j to be installed locally
    api("pe.pi:sctp4j:1.0.7-SNAPSHOT")

    api("org.cloudburstmc.protocol:common:3.0.0.Beta3-20240814.133201-7")
    api("org.cloudburstmc.protocol:bedrock-codec:3.0.0.Beta3-20240814.133201-7")
    api("org.cloudburstmc.protocol:bedrock-connection:3.0.0.Beta3-20240814.133201-7")
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
