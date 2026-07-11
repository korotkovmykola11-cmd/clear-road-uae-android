package com.clearroad.app.benchmark

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

/**
 * Stage 0B — test-only live benchmark configuration loader.
 *
 * Never logs or exposes secret key values.
 */
object BenchmarkLiveConfig {

    const val GOOGLE_MAPS_API_KEY_PROPERTY = "GOOGLE_MAPS_API_KEY"
    const val GRAPHHOPPER_API_KEY_PROPERTY = "GRAPHHOPPER_API_KEY"

    private const val REPOSITORY_MARKER = "settings.gradle.kts"
    private const val LOCAL_PROPERTIES_FILE = "local.properties"

    enum class KeyStatus {
        PRESENT,
        MISSING,
    }

    enum class KeySource {
        LOCAL_PROPERTIES,
        ENVIRONMENT,
        NONE,
    }

    data class ProviderKeyState(
        val status: KeyStatus,
        val source: KeySource,
    ) {
        override fun toString(): String = "ProviderKeyState(status=$status, source=$source)"
    }

    data class LoadResult(
        val repositoryRootFound: Boolean,
        val repositoryRoot: Path?,
        val google: ProviderKeyState,
        val graphHopper: ProviderKeyState,
        val repositoryRootError: String? = null,
    ) {
        val isStage0BReady: Boolean
            get() =
                google.status == KeyStatus.PRESENT &&
                    graphHopper.status == KeyStatus.PRESENT

        val missingKeyReasons: List<String>
            get() =
                buildList {
                    if (google.status == KeyStatus.MISSING) {
                        add("GOOGLE_MAPS_API_KEY is ${KeyStatus.MISSING} (${google.source})")
                    }
                    if (graphHopper.status == KeyStatus.MISSING) {
                        add("GRAPHHOPPER_API_KEY is ${KeyStatus.MISSING} (${graphHopper.source})")
                    }
                    repositoryRootError?.let { add(it) }
                }

        override fun toString(): String =
            "LoadResult(repositoryRootFound=$repositoryRootFound, google=$google, " +
                "graphHopper=$graphHopper, stage0BReady=$isStage0BReady)"
    }

    fun load(
        startDirectory: Path = Path.of("").toAbsolutePath().normalize(),
        environment: Map<String, String> = System.getenv(),
        localPropertiesContent: String? = null,
    ): LoadResult {
        val repositoryRoot = findRepositoryRoot(startDirectory)
        if (repositoryRoot == null) {
            return LoadResult(
                repositoryRootFound = false,
                repositoryRoot = null,
                google = resolveKey(GOOGLE_MAPS_API_KEY_PROPERTY, null, environment),
                graphHopper = resolveKey(GRAPHHOPPER_API_KEY_PROPERTY, null, environment),
                repositoryRootError = "Repository root not found (missing $REPOSITORY_MARKER)",
            )
        }

        val localProperties =
            localPropertiesContent?.let(::parseProperties)
                ?: readLocalProperties(repositoryRoot)

        return LoadResult(
            repositoryRootFound = true,
            repositoryRoot = repositoryRoot,
            google = resolveKey(GOOGLE_MAPS_API_KEY_PROPERTY, localProperties, environment),
            graphHopper = resolveKey(GRAPHHOPPER_API_KEY_PROPERTY, localProperties, environment),
        )
    }

    internal fun findRepositoryRoot(startDirectory: Path): Path? {
        var current = startDirectory.toAbsolutePath().normalize()
        while (true) {
            val marker = current.resolve(REPOSITORY_MARKER)
            if (Files.isRegularFile(marker)) {
                return current
            }
            val parent = current.parent ?: return null
            if (parent == current) {
                return null
            }
            current = parent
        }
    }

    internal fun resolveKey(
        propertyName: String,
        localProperties: Properties?,
        environment: Map<String, String>,
    ): ProviderKeyState {
        val localValue = localProperties?.getProperty(propertyName)?.trim().orEmpty()
        if (localValue.isNotEmpty()) {
            return ProviderKeyState(KeyStatus.PRESENT, KeySource.LOCAL_PROPERTIES)
        }

        val environmentValue = environment[propertyName]?.trim().orEmpty()
        if (environmentValue.isNotEmpty()) {
            return ProviderKeyState(KeyStatus.PRESENT, KeySource.ENVIRONMENT)
        }

        return ProviderKeyState(KeyStatus.MISSING, KeySource.NONE)
    }

    private fun readLocalProperties(repositoryRoot: Path): Properties? {
        val file = repositoryRoot.resolve(LOCAL_PROPERTIES_FILE)
        if (!Files.isRegularFile(file)) {
            return null
        }
        return Files.newInputStream(file).use { stream -> parseProperties(stream) }
    }

    private fun parseProperties(content: String): Properties =
        Properties().apply {
            load(content.byteInputStream())
        }

    private fun parseProperties(stream: InputStream): Properties =
        Properties().apply {
            load(stream)
        }
}
