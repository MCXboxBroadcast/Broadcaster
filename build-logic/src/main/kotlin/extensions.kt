import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.kotlin.dsl.named

fun Project.relocate(pattern: String) {
    tasks.named<ShadowJar>("shadowJar") {
        relocate(pattern, "com.rtm516.mcxboxbroadcast.shaded.$pattern")
    }
}

fun Project.nameJar(name: String) {
    tasks.named<ShadowJar>("shadowJar") {
        archiveBaseName.set(name)
    }
}