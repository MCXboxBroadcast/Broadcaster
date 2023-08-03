include(":bootstrap-standalone")
include(":bootstrap-parent")
include(":core")
include(":bootstrap-geyser")
project(":bootstrap-standalone").projectDir = file("bootstrap/standalone")
project(":bootstrap-parent").projectDir = file("bootstrap")
project(":bootstrap-geyser").projectDir = file("bootstrap/geyser")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}