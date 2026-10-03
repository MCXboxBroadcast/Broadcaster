import net.kyori.indra.git.IndraGitExtension
import net.kyori.indra.git.RepositoryValueSource
import org.eclipse.jgit.api.Git

// Modified from https://github.com/GeyserMC/Geyser/blob/ca2312c7f68c54f32314c40c2c1db5d9cda5a0b2/core/build.gradle.kts#L111-L143
class GitInfo(indraGit: IndraGitExtension) {
    val branch: String
    val commit: String
    val commitAbbrev: String

    val gitVersion: String
    val version: String
    val buildNumber: Int

    val repository: String

    init {
        branch = indraGit.branchName().getOrElse("DEV")

        val commit = indraGit.commit().orNull
        this.commit = commit?.name ?: "0".repeat(40)
        commitAbbrev = commit?.name?.substring(0, 7) ?: "0".repeat(7)

        gitVersion = "git-${branch}-${commitAbbrev}"
        buildNumber = (System.getenv("BUILD_NUMBER"))?.let { Integer.parseInt(it) } ?: -1

        if (buildNumber == -1) {
            version = "DEV ($gitVersion)"
        } else {
            version = "build ${buildNumber} ($gitVersion)"
        }

        repository = indraGit.repositoryValue(RepositoryUrlValueSource::class.java).getOrElse("")
    }
}

abstract class RepositoryUrlValueSource : RepositoryValueSource.Parameterless<String>() {
    override fun obtain(repository: Git): String? {
        return repository.repository.config.getString("remote", "origin", "url")
    }
}
