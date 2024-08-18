import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

plugins {
    id("com.rtm516.mcxboxbroadcast.shadow-conventions")
    application
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

application {
    mainClass.set("com.rtm516.mcxboxbroadcast.bootstrap.standalone.StandaloneMain")
}

tasks.withType<ShadowJar> {
    transform(Log4j2PluginsCacheFileTransformer())
}

nameJar("MCXboxBroadcastStandalone")

description = "bootstrap-standalone"
