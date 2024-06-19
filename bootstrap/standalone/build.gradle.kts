import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

plugins {
    id("com.rtm516.mcxboxbroadcast.shadow-conventions")
}

dependencies {
    api(project(":core"))
    api(libs.bundles.jackson)
    api(libs.bedrock.common)

    api(libs.terminalconsoleappender) {
        exclude("org.apache.logging.log4j")
        exclude("org.jline")
    }

    api(libs.bundles.jline)

    api(libs.bundles.log4j)
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.rtm516.mcxboxbroadcast.bootstrap.standalone.StandaloneMain"
    }
}

tasks.withType<ShadowJar> {
    transform(Log4j2PluginsCacheFileTransformer())
}

nameJar("MCXboxBroadcastStandalone")

description = "bootstrap-standalone"
