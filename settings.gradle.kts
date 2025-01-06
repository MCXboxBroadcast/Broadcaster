include(":core")
include(":bootstrap-standalone")
include(":bootstrap-geyser")
include(":bootstrap-tester")
project(":bootstrap-standalone").projectDir = file("bootstrap/standalone")
project(":bootstrap-geyser").projectDir = file("bootstrap/geyser")
project(":bootstrap-tester").projectDir = file("bootstrap/tester")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}