include(":core")
include(":bootstrap-standalone")
include(":bootstrap-geyser")
include(":bootstrap-manager")
project(":bootstrap-standalone").projectDir = file("bootstrap/standalone")
project(":bootstrap-geyser").projectDir = file("bootstrap/geyser")
project(":bootstrap-manager").projectDir = file("bootstrap/manager")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}