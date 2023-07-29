plugins {
    id("com.rtm516.mcxboxbroadcast.shadow-conventions")
}

dependencies {
    api(project(":core"))
    api(libs.bundles.jackson.yaml)
    api(libs.bedrock.common)
    api(libs.slf4j.simple)
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.rtm516.mcxboxbroadcast.bootstrap.standalone.StandaloneMain"
    }
}

nameJar("MCXboxBroadcastStandalone")

description = "bootstrap-standalone"
