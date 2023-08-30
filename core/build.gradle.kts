plugins {
    id("com.rtm516.mcxboxbroadcast.java-conventions")
}

dependencies {
    api(libs.bundles.jackson.databind)
    api(libs.nimbus.jose.jwt)
    api(libs.java.websocket)
}

description = "core"
