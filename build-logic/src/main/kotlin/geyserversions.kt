import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Resolves the Minecraft: Java Edition version Geyser currently supports by reading the
 * `minecraft` version out of Geyser's own version catalog.
 */
abstract class GeyserMinecraftVersion : ValueSource<String, GeyserMinecraftVersion.Params> {
    interface Params : ValueSourceParameters {
        val catalogUrl: Property<String>
        val fallback: Property<String>
    }

    override fun obtain(): String {
        val url = parameters.catalogUrl.get()
        try {
            val client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()

            val request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) {
                error("unexpected status code ${response.statusCode()}")
            }

            // Pull `minecraft = "..."` out of the `[versions]` table, the `[libraries]` table
            // has an entry with the same name so we can't just search the whole file
            val versions = response.body().substringAfter("[versions]", "").substringBefore("\n[")
            return Regex("""^\s*minecraft\s*=\s*"([^"]+)"\s*$""", RegexOption.MULTILINE)
                .find(versions)?.groupValues?.get(1)
                ?: error("no minecraft version found in the catalog")
        } catch (e: Exception) {
            val fallback = parameters.fallback.get()
            println("Warning: Unable to resolve Geyser's Minecraft version from $url (${e.message ?: e.toString()}), falling back to $fallback")
            return fallback
        }
    }
}
