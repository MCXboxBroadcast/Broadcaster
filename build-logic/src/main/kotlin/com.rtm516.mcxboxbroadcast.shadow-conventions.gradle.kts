plugins {
    id("com.rtm516.mcxboxbroadcast.java-conventions")
    id("com.gradleup.shadow")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveVersion.set("")

        // libdatachannel limits us to glibc Linux x86_64/aarch64, Windows x86_64 and macOS x86_64/arm64

        // Strip sqlite-jdbc natives for every other platform
        listOf(
            "FreeBSD", "Linux-Android", "Linux-Musl",
            "Linux/arm", "Linux/armv6", "Linux/armv7", "Linux/ppc64", "Linux/riscv64", "Linux/x86",
            "Windows/aarch64", "Windows/armv7", "Windows/x86"
        ).forEach { exclude("org/sqlite/native/$it/**") }

        // Strip JNA natives for every other platform
        listOf(
            "aix-ppc", "aix-ppc64", "freebsd-x86", "freebsd-x86-64", "linux-arm", "linux-armel",
            "linux-loongarch64", "linux-mips64el", "linux-ppc", "linux-ppc64le", "linux-riscv64",
            "linux-s390x", "linux-x86", "openbsd-x86", "openbsd-x86-64", "sunos-sparc",
            "sunos-sparcv9", "sunos-x86", "sunos-x86-64", "win32-aarch64", "win32-x86"
        ).forEach { exclude("com/sun/jna/$it/**") }

        // Strip BouncyCastle post-quantum engines, keeping pqc/asn1 and pqc/jcajce which its provider needs
        listOf("crypto", "legacy").forEach {
            exclude("org/bouncycastle/pqc/$it/**")
            exclude("META-INF/versions/*/org/bouncycastle/pqc/$it/**")
        }
    }

    build {
        dependsOn(shadowJar)
    }
}