plugins {
    id("com.rtm516.mcxboxbroadcast.java-conventions")
    id("com.gradleup.shadow")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveVersion.set("")
    }

    build {
        dependsOn(shadowJar)
    }
}