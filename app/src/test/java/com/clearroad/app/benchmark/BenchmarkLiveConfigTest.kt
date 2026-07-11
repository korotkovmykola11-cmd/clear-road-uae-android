package com.clearroad.app.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

class BenchmarkLiveConfigTest {

    @Test
    fun localProperties_winOverEnvironment() {
        val repo = createRepositoryRoot()
        writeLocalProperties(
            repo,
            """
            GOOGLE_MAPS_API_KEY=local-google-secret
            GRAPHHOPPER_API_KEY=local-gh-secret
            """.trimIndent(),
        )

        val result =
            BenchmarkLiveConfig.load(
                startDirectory = repo.resolve("app"),
                environment =
                    mapOf(
                        BenchmarkLiveConfig.GOOGLE_MAPS_API_KEY_PROPERTY to "env-google-secret",
                        BenchmarkLiveConfig.GRAPHHOPPER_API_KEY_PROPERTY to "env-gh-secret",
                    ),
            )

        assertTrue(result.repositoryRootFound)
        assertEquals(BenchmarkLiveConfig.KeyStatus.PRESENT, result.google.status)
        assertEquals(BenchmarkLiveConfig.KeySource.LOCAL_PROPERTIES, result.google.source)
        assertEquals(BenchmarkLiveConfig.KeyStatus.PRESENT, result.graphHopper.status)
        assertEquals(BenchmarkLiveConfig.KeySource.LOCAL_PROPERTIES, result.graphHopper.source)
        assertTrue(result.isStage0BReady)
        assertNoSecretsInDiagnostics(result, "local-google-secret", "env-google-secret", "local-gh-secret", "env-gh-secret")
    }

    @Test
    fun environmentFallback_worksWhenLocalPropertiesMissing() {
        val repo = createRepositoryRoot()

        val result =
            BenchmarkLiveConfig.load(
                startDirectory = repo,
                environment =
                    mapOf(
                        BenchmarkLiveConfig.GOOGLE_MAPS_API_KEY_PROPERTY to "env-google-secret",
                        BenchmarkLiveConfig.GRAPHHOPPER_API_KEY_PROPERTY to "env-gh-secret",
                    ),
            )

        assertTrue(result.repositoryRootFound)
        assertEquals(BenchmarkLiveConfig.KeySource.ENVIRONMENT, result.google.source)
        assertEquals(BenchmarkLiveConfig.KeySource.ENVIRONMENT, result.graphHopper.source)
        assertTrue(result.isStage0BReady)
        assertNoSecretsInDiagnostics(result, "env-google-secret", "env-gh-secret")
    }

    @Test
    fun blankLocalValue_fallsBackToEnvironment() {
        val repo = createRepositoryRoot()
        writeLocalProperties(
            repo,
            """
            GOOGLE_MAPS_API_KEY=
            GRAPHHOPPER_API_KEY=   
            """.trimIndent(),
        )

        val result =
            BenchmarkLiveConfig.load(
                startDirectory = repo,
                environment =
                    mapOf(
                        BenchmarkLiveConfig.GOOGLE_MAPS_API_KEY_PROPERTY to "env-google-secret",
                        BenchmarkLiveConfig.GRAPHHOPPER_API_KEY_PROPERTY to "env-gh-secret",
                    ),
            )

        assertEquals(BenchmarkLiveConfig.KeySource.ENVIRONMENT, result.google.source)
        assertEquals(BenchmarkLiveConfig.KeySource.ENVIRONMENT, result.graphHopper.source)
        assertTrue(result.isStage0BReady)
        assertNoSecretsInDiagnostics(result, "env-google-secret", "env-gh-secret")
    }

    @Test
    fun bothKeysMissing_reportsNotReady() {
        val repo = createRepositoryRoot()

        val result =
            BenchmarkLiveConfig.load(
                startDirectory = repo,
                environment = emptyMap(),
            )

        assertFalse(result.isStage0BReady)
        assertEquals(BenchmarkLiveConfig.KeyStatus.MISSING, result.google.status)
        assertEquals(BenchmarkLiveConfig.KeyStatus.MISSING, result.graphHopper.status)
        assertEquals(BenchmarkLiveConfig.KeySource.NONE, result.google.source)
        assertEquals(BenchmarkLiveConfig.KeySource.NONE, result.graphHopper.source)
        assertEquals(2, result.missingKeyReasons.size)
        assertNoSecretsInDiagnostics(result)
    }

    @Test
    fun oneKeyMissing_reportsPartialReadiness() {
        val repo = createRepositoryRoot()
        writeLocalProperties(
            repo,
            """
            GOOGLE_MAPS_API_KEY=local-google-secret
            """.trimIndent(),
        )

        val result =
            BenchmarkLiveConfig.load(
                startDirectory = repo,
                environment = emptyMap(),
            )

        assertFalse(result.isStage0BReady)
        assertEquals(BenchmarkLiveConfig.KeyStatus.PRESENT, result.google.status)
        assertEquals(BenchmarkLiveConfig.KeyStatus.MISSING, result.graphHopper.status)
        assertEquals(1, result.missingKeyReasons.size)
        assertNoSecretsInDiagnostics(result, "local-google-secret")
    }

    @Test
    fun repositoryRootNotFound_returnsStructuredResult() {
        val isolated = createTempDirectory("benchmark-live-config-isolated")
        try {
            val result =
                BenchmarkLiveConfig.load(
                    startDirectory = isolated,
                    environment = emptyMap(),
                )

            assertFalse(result.repositoryRootFound)
            assertNull(result.repositoryRoot)
            assertNotNull(result.repositoryRootError)
            assertFalse(result.isStage0BReady)
            assertTrue(result.missingKeyReasons.any { it.contains("Repository root not found") })
            assertNoSecretsInDiagnostics(result)
        } finally {
            isolated.toFile().deleteRecursively()
        }
    }

    @Test
    fun resolveKey_prefersLocalOverEnvironment() {
        val local = java.util.Properties().apply {
            put(BenchmarkLiveConfig.GOOGLE_MAPS_API_KEY_PROPERTY, "local-google-secret")
        }

        val state =
            BenchmarkLiveConfig.resolveKey(
                propertyName = BenchmarkLiveConfig.GOOGLE_MAPS_API_KEY_PROPERTY,
                localProperties = local,
                environment = mapOf(BenchmarkLiveConfig.GOOGLE_MAPS_API_KEY_PROPERTY to "env-google-secret"),
            )

        assertEquals(BenchmarkLiveConfig.KeyStatus.PRESENT, state.status)
        assertEquals(BenchmarkLiveConfig.KeySource.LOCAL_PROPERTIES, state.source)
        assertFalse(state.toString().contains("local-google-secret"))
        assertFalse(state.toString().contains("env-google-secret"))
    }

    private fun createRepositoryRoot(): Path {
        val repo = createTempDirectory("benchmark-live-config-repo")
        Files.writeString(repo.resolve("settings.gradle.kts"), "// test repo marker")
        return repo
    }

    private fun writeLocalProperties(repo: Path, content: String) {
        Files.writeString(repo.resolve("local.properties"), content)
    }

    private fun assertNoSecretsInDiagnostics(result: BenchmarkLiveConfig.LoadResult, vararg secrets: String) {
        val diagnostics =
            listOf(
                result.toString(),
                result.google.toString(),
                result.graphHopper.toString(),
                result.missingKeyReasons.joinToString(),
                result.repositoryRootError.orEmpty(),
            ).joinToString(" ")

        secrets.forEach { secret ->
            assertFalse("Secret leaked in diagnostics: $secret", diagnostics.contains(secret))
        }
        assertFalse(diagnostics.contains("api_key="))
        assertFalse(diagnostics.contains("secret"))
    }
}
