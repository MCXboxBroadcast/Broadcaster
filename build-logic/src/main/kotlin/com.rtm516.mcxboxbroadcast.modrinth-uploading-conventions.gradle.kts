plugins {
    id("com.modrinth.minotaur")
}

// Ensure that the readme is synched
tasks.modrinth.get().dependsOn(tasks.modrinthSyncBody)

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN") ?: "") // Even though this is the default value, apparently this prevents GitHub Actions caching the token?
    debugMode.set(System.getenv("MODRINTH_TOKEN") == null)
    projectId.set("mcxboxbroadcast")
    versionName.set("Build " + System.getenv("BUILD_NUMBER"))
    versionNumber.set(System.getenv("BUILD_NUMBER"))
    versionType.set("release")
    changelog.set(rootProject.file("release_notes.md").readText())
    gameVersions.addAll("1.21.10")
    loaders.addAll("geyser")
    failSilently.set(true)

    syncBodyFrom.set(rootProject.file("README.md").readText())
}