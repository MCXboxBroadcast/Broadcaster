include(":core")
include(":bootstrap-standalone")
include(":bootstrap-geyser")
project(":bootstrap-standalone").projectDir = file("bootstrap/standalone")
project(":bootstrap-geyser").projectDir = file("bootstrap/geyser")

if (file("bootstrap/manager").exists()) {
    include(":bootstrap-manager")
    project(":bootstrap-manager").projectDir = file("bootstrap/manager")
}

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}
