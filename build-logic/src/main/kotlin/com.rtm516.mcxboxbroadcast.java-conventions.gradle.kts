plugins {
    `java-library`
    `maven-publish`

    // Allow blossom to mark sources root of templates
    idea
    id("net.kyori.indra.git")
    id("net.kyori.blossom")
}

repositories {
    mavenLocal()
    maven("https://repo.maven.apache.org/maven2/")
    maven("https://repo.opencollab.dev/maven-releases/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://jitpack.io")
    maven("https://maven.lenni0451.net/snapshots")
}

group = properties["group"] as String
version = properties["version"] as String
java.sourceCompatibility = JavaVersion.VERSION_17

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}