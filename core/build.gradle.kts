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
    api(libs.bundles.protocol)

    api("org.jitsi:ice4j:3.0-72-g824cd4b")
    api(libs.bundles.bouncycastle)

    // Needs https://github.com/steely-glint/srtplight and https://github.com/pipe/sctp4j to be installed locally
    api("pe.pi:sctp4j:1.0.7-SNAPSHOT")
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
