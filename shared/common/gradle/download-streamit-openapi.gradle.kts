import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import groovy.json.JsonSlurper

val streamitImportOpenApiFile = layout.buildDirectory.file("generated/openapi/openapi-importing.yaml")
val streamitReleasesApiUrl = "https://api.github.com/repos/iktdev-no/Streamit-service/releases?per_page=100"
val streamitOpenApiAssetName = "openapi-importing.yaml"

fun streamitReleaseAssetUrl(release: Map<*, *>): String? =
    (release["assets"] as? List<*>)
        ?.filterIsInstance<Map<*, *>>()
        ?.firstOrNull { it["name"] == streamitOpenApiAssetName }
        ?.get("browser_download_url") as? String

fun resolveStreamitOpenApiRelease(): Pair<String, String> {
    val connection = URL(streamitReleasesApiUrl).openConnection().apply {
        connectTimeout = 15_000
        readTimeout = 60_000
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "MediaProcessing-OpenAPI-Downloader")
    }

    val releases = try {
        if (connection is HttpURLConnection && connection.responseCode !in 200..299) {
            throw GradleException("GitHub releases API returned HTTP ${connection.responseCode}")
        }
        connection.getInputStream().bufferedReader(Charsets.UTF_8).use {
            JsonSlurper().parseText(it.readText()) as? List<*> ?: emptyList<Any>()
        }.filterIsInstance<Map<*, *>>()
    } finally {
        if (connection is HttpURLConnection) connection.disconnect()
    }

    val stableRelease = releases.firstOrNull {
        (it["tag_name"] as? String)?.contains("snapshot", ignoreCase = true) != true &&
            streamitReleaseAssetUrl(it) != null
    }
    val snapshotRelease = releases.firstOrNull {
        (it["prerelease"] == true || (it["tag_name"] as? String)?.contains("snapshot", ignoreCase = true) == true) &&
            streamitReleaseAssetUrl(it) != null
    }
    val release = stableRelease ?: snapshotRelease
        ?: throw GradleException("No GitHub release or snapshot contains $streamitOpenApiAssetName")

    return (release["tag_name"] as? String ?: "unknown") to
        (streamitReleaseAssetUrl(release) ?: error("Selected release has no $streamitOpenApiAssetName asset"))
}

tasks.register("downloadStreamitImportOpenApi") {
    group = "streamit"
    description = "Downloads the Streamit importing OpenAPI specification used to generate shared models."
    outputs.file(streamitImportOpenApiFile)
    outputs.upToDateWhen { false }

    doLast {
        val outputFile = streamitImportOpenApiFile.get().asFile
        outputFile.parentFile.mkdirs()
        val temporaryFile = outputFile.resolveSibling("${outputFile.name}.download")
        val (releaseTag, assetUrl) = resolveStreamitOpenApiRelease()
        val connection = URL(assetUrl).openConnection().apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("User-Agent", "MediaProcessing-OpenAPI-Downloader")
        }

        try {
            if (connection is HttpURLConnection && connection.responseCode !in 200..299) {
                throw GradleException(
                    "Downloading importing OpenAPI spec failed with HTTP ${connection.responseCode} from $assetUrl"
                )
            }

            connection.getInputStream().use { input ->
                temporaryFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (!temporaryFile.readText(Charsets.UTF_8).take(4096).contains("openapi:")) {
                throw GradleException("Downloaded file from $assetUrl is not an OpenAPI YAML document.")
            }

            if (!outputFile.exists() || Files.mismatch(temporaryFile.toPath(), outputFile.toPath()) != -1L) {
                Files.move(
                    temporaryFile.toPath(),
                    outputFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
                logger.lifecycle("Downloaded Streamit importing OpenAPI spec from release $releaseTag")
            } else {
                logger.lifecycle("Streamit importing OpenAPI spec from release $releaseTag is already up to date")
            }
        } finally {
            temporaryFile.delete()
            if (connection is HttpURLConnection) {
                connection.disconnect()
            }
        }
    }
}
