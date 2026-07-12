package com.clearroad.app.benchmark

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory

internal const val STAGE0B_LIVE_PROPERTY = "benchmark.stage0b.live"
internal const val STAGE0B_LIVE_ENV = "BENCHMARK_STAGE0B_LIVE"
internal const val STAGE0B_CASE_COUNT_PROPERTY = "benchmark.stage0b.caseCount"
internal const val STAGE0B_CASE_COUNT_ENV = "BENCHMARK_STAGE0B_CASE_COUNT"

internal object Stage0BLivePilotConfig {
    const val DEFAULT_CASE_COUNT = 1
    val ALLOWED_CASE_COUNTS = setOf(1, 3, 10)

    fun isLiveEnabled(
        liveProperty: String? = System.getProperty(STAGE0B_LIVE_PROPERTY),
        environment: Map<String, String> = System.getenv(),
    ): Boolean {
        val raw =
            liveProperty?.trim()?.takeIf { it.isNotEmpty() }
                ?: environment[STAGE0B_LIVE_ENV]?.trim()?.takeIf { it.isNotEmpty() }
        return raw?.equals("true", ignoreCase = true) == true
    }

    fun resolveCaseCount(
        caseCountProperty: String? = System.getProperty(STAGE0B_CASE_COUNT_PROPERTY),
        environment: Map<String, String> = System.getenv(),
    ): Int? {
        val raw =
            caseCountProperty?.trim()?.takeIf { it.isNotEmpty() }
                ?: environment[STAGE0B_CASE_COUNT_ENV]?.trim()?.takeIf { it.isNotEmpty() }
                ?: DEFAULT_CASE_COUNT.toString()
        val parsed = raw.toIntOrNull() ?: return null
        return parsed.takeIf { it in ALLOWED_CASE_COUNTS }
    }

    fun selectedCases(caseCount: Int): List<UaeRouteBenchmarkCase> = Stage0BLiveCases.all.take(caseCount)
}

internal data class Stage0BLivePreflight(
    val shouldRunLive: Boolean,
    val skipReason: String?,
    val config: BenchmarkLiveConfig.LoadResult,
    val caseCount: Int = Stage0BLivePilotConfig.DEFAULT_CASE_COUNT,
    val selectedCases: List<UaeRouteBenchmarkCase> =
        Stage0BLivePilotConfig.selectedCases(Stage0BLivePilotConfig.DEFAULT_CASE_COUNT),
)

internal data class ResolvedApiKeys(
    val googleApiKey: String,
    val graphHopperApiKey: String,
) {
    override fun toString(): String = "ResolvedApiKeys(google=[redacted], graphHopper=[redacted])"
}

internal data class Stage0BLiveBenchmarkRun(
    val preflight: Stage0BLivePreflight,
    val result: Stage0BLiveRunResult?,
    val httpAttempts: Int,
    val providersCreated: Boolean,
    val httpExecutorCreated: Boolean,
)

internal object Stage0BLiveBenchmarkGate {
    fun preflight(
        liveProperty: String? = System.getProperty(STAGE0B_LIVE_PROPERTY),
        caseCountProperty: String? = System.getProperty(STAGE0B_CASE_COUNT_PROPERTY),
        environment: Map<String, String> = System.getenv(),
        startDirectory: Path = Path.of("").toAbsolutePath().normalize(),
    ): Stage0BLivePreflight {
        val config = BenchmarkLiveConfig.load(startDirectory = startDirectory, environment = environment)
        if (!Stage0BLivePilotConfig.isLiveEnabled(liveProperty, environment)) {
            return Stage0BLivePreflight(
                shouldRunLive = false,
                skipReason =
                    "Stage 0B live benchmark not enabled " +
                        "(set -D$STAGE0B_LIVE_PROPERTY=true or $STAGE0B_LIVE_ENV=true)",
                config = config,
            )
        }
        if (!config.isStage0BReady) {
            return Stage0BLivePreflight(
                shouldRunLive = false,
                skipReason = config.missingKeyReasons.joinToString("; "),
                config = config,
            )
        }
        val caseCount = Stage0BLivePilotConfig.resolveCaseCount(caseCountProperty, environment)
        if (caseCount == null) {
            return Stage0BLivePreflight(
                shouldRunLive = false,
                skipReason =
                    "Invalid Stage 0B case count (allowed: ${Stage0BLivePilotConfig.ALLOWED_CASE_COUNTS.sorted()}; " +
                        "set -D$STAGE0B_CASE_COUNT_PROPERTY or $STAGE0B_CASE_COUNT_ENV)",
                config = config,
            )
        }
        return Stage0BLivePreflight(
            shouldRunLive = true,
            skipReason = null,
            config = config,
            caseCount = caseCount,
            selectedCases = Stage0BLivePilotConfig.selectedCases(caseCount),
        )
    }
}

internal fun resolveApiKeys(
    config: BenchmarkLiveConfig.LoadResult,
    environment: Map<String, String> = System.getenv(),
): ResolvedApiKeys? {
    if (!config.isStage0BReady) {
        return null
    }
    val repositoryRoot = config.repositoryRoot ?: return null

    val localProperties =
        run {
            val file = repositoryRoot.resolve("local.properties")
            if (!Files.isRegularFile(file)) {
                null
            } else {
                Properties().apply {
                    Files.newInputStream(file).use { load(it) }
                }
            }
        }

    fun resolve(propertyName: String): String {
        val localValue = localProperties?.getProperty(propertyName)?.trim().orEmpty()
        if (localValue.isNotEmpty()) {
            return localValue
        }
        return environment[propertyName]?.trim().orEmpty()
    }

    val googleApiKey = resolve(BenchmarkLiveConfig.GOOGLE_MAPS_API_KEY_PROPERTY)
    val graphHopperApiKey = resolve(BenchmarkLiveConfig.GRAPHHOPPER_API_KEY_PROPERTY)
    if (googleApiKey.isBlank() || graphHopperApiKey.isBlank()) {
        return null
    }
    return ResolvedApiKeys(googleApiKey = googleApiKey, graphHopperApiKey = graphHopperApiKey)
}

internal class Stage0BLiveBenchmarkHarness(
    private val liveProperty: String? = System.getProperty(STAGE0B_LIVE_PROPERTY),
    private val caseCountProperty: String? = System.getProperty(STAGE0B_CASE_COUNT_PROPERTY),
    private val environment: Map<String, String> = System.getenv(),
    private val startDirectory: Path = Path.of("").toAbsolutePath().normalize(),
    private val httpExecutorFactory: () -> BenchmarkLiveHttpExecutor = { BenchmarkLiveHttpExecutor.stage0B() },
    private val googleProviderFactory: ((String, BenchmarkLiveHttpExecutor) -> BenchmarkLiveProvider)? = null,
    private val graphHopperProviderFactory: ((String, BenchmarkLiveHttpExecutor) -> BenchmarkLiveProvider)? = null,
    private val runner: Stage0BLiveRunner = Stage0BLiveRunner(),
) {
    fun run(): Stage0BLiveBenchmarkRun {
        val preflight =
            Stage0BLiveBenchmarkGate.preflight(
                liveProperty = liveProperty,
                caseCountProperty = caseCountProperty,
                environment = environment,
                startDirectory = startDirectory,
            )
        if (!preflight.shouldRunLive) {
            return Stage0BLiveBenchmarkRun(
                preflight = preflight,
                result = null,
                httpAttempts = 0,
                providersCreated = false,
                httpExecutorCreated = false,
            )
        }

        val keys =
            resolveApiKeys(preflight.config, environment)
                ?: return Stage0BLiveBenchmarkRun(
                    preflight =
                        preflight.copy(
                            shouldRunLive = false,
                            skipReason = "API keys could not be resolved despite readiness check",
                        ),
                    result = null,
                    httpAttempts = 0,
                    providersCreated = false,
                    httpExecutorCreated = false,
                )

        val httpExecutor = httpExecutorFactory()
        val googleProvider =
            googleProviderFactory?.invoke(keys.googleApiKey, httpExecutor)
                ?: GoogleLiveBenchmarkProvider(keys.googleApiKey, httpExecutor)
        val graphHopperProvider =
            graphHopperProviderFactory?.invoke(keys.graphHopperApiKey, httpExecutor)
                ?: GraphHopperLiveBenchmarkProvider(keys.graphHopperApiKey, httpExecutor)

        val selectedCases = preflight.selectedCases
        val budget = BenchmarkLiveBudget.stage0B()
        val result = runner.run(selectedCases, googleProvider, graphHopperProvider, budget)

        return Stage0BLiveBenchmarkRun(
            preflight = preflight,
            result = result,
            httpAttempts = 0,
            providersCreated = true,
            httpExecutorCreated = true,
        )
    }
}

class Stage0BLiveEntryGateTest {

    private val fakeGoogleKey = "fake-google-key-offline-gate-7a"
    private val fakeGraphHopperKey = "fake-graphhopper-key-offline-gate-7b"

    @Test
    fun preflight_disabledFlag_skipsBeforeNetwork() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)

        val run =
            harness(
                liveProperty = null,
                startDirectory = repo,
                environment = bothKeyEnvironment(),
            ).run()

        assertFalse(run.preflight.shouldRunLive)
        assertNotNull(run.preflight.skipReason)
        assertNull(run.result)
        assertFalse(run.providersCreated)
        assertFalse(run.httpExecutorCreated)
        assertEquals(0, run.httpAttempts)
    }

    @Test
    fun preflight_missingGoogleKey_skipsBeforeNetwork() {
        val repo = createBenchmarkRepositoryRoot(withGoogleKey = false, withGraphHopperKey = true)

        val run =
            harness(
                liveProperty = "true",
                startDirectory = repo,
                environment = mapOf(BenchmarkLiveConfig.GRAPHHOPPER_API_KEY_PROPERTY to fakeGraphHopperKey),
            ).run()

        assertFalse(run.preflight.shouldRunLive)
        assertTrue(run.preflight.skipReason!!.contains("GOOGLE_MAPS_API_KEY"))
        assertNull(run.result)
        assertFalse(run.providersCreated)
        assertEquals(0, run.httpAttempts)
    }

    @Test
    fun preflight_missingGraphHopperKey_skipsBeforeNetwork() {
        val repo = createBenchmarkRepositoryRoot(withGoogleKey = true, withGraphHopperKey = false)

        val run =
            harness(
                liveProperty = "true",
                startDirectory = repo,
                environment = mapOf(BenchmarkLiveConfig.GOOGLE_MAPS_API_KEY_PROPERTY to fakeGoogleKey),
            ).run()

        assertFalse(run.preflight.shouldRunLive)
        assertTrue(run.preflight.skipReason!!.contains("GRAPHHOPPER_API_KEY"))
        assertNull(run.result)
        assertFalse(run.providersCreated)
        assertEquals(0, run.httpAttempts)
    }

    @Test
    fun preflight_bothKeysMissing_skipsBeforeNetwork() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = false)

        val run =
            harness(
                liveProperty = "true",
                startDirectory = repo,
                environment = emptyMap(),
            ).run()

        assertFalse(run.preflight.shouldRunLive)
        assertTrue(run.preflight.skipReason!!.contains("GOOGLE_MAPS_API_KEY"))
        assertTrue(run.preflight.skipReason!!.contains("GRAPHHOPPER_API_KEY"))
        assertNull(run.result)
        assertFalse(run.providersCreated)
        assertEquals(0, run.httpAttempts)
    }

    @Test
    fun skipPath_reservesZeroBudget() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = false)
        val budget = BenchmarkLiveBudget.stage0B()

        harness(liveProperty = null, startDirectory = repo).run()

        assertEquals(0, budget.snapshot().totalReserved)
        assertEquals(0, budget.snapshot().googleReserved)
        assertEquals(0, budget.snapshot().graphHopperReserved)
    }

    @Test
    fun skipPath_executesZeroHttpAttempts() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)
        val httpAttempts = AtomicInteger(0)

        val run =
            harness(
                liveProperty = "false",
                startDirectory = repo,
                environment = bothKeyEnvironment(),
                httpExecutorFactory = countingHttpExecutorFactory(httpAttempts),
            ).run()

        assertFalse(run.preflight.shouldRunLive)
        assertEquals(0, httpAttempts.get())
        assertEquals(0, run.httpAttempts)
    }

    @Test
    fun run_enabledWithFakeProviders_reachesRunnerOffline() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)
        val googleProvider = FakeRecordingProvider(BenchmarkProviderId.GOOGLE)
        val graphHopperProvider = FakeRecordingProvider(BenchmarkProviderId.GRAPHHOPPER)

        val run =
            harness(
                liveProperty = "true",
                startDirectory = repo,
                environment = bothKeyEnvironment(),
                httpExecutorFactory = { blockedHttpExecutor() },
                googleProviderFactory = { _, _ -> googleProvider },
                graphHopperProviderFactory = { _, _ -> graphHopperProvider },
            ).run()

        assertTrue(run.preflight.shouldRunLive)
        assertNotNull(run.result)
        assertTrue(run.providersCreated)
        assertEquals(1, run.result!!.requestedCases)
        assertEquals("live-difc-marina", run.result!!.caseResults.single().caseId)
        assertEquals(1, googleProvider.fetchCount)
        assertEquals(1, graphHopperProvider.fetchCount)
    }

    @Test
    fun run_fakeSuccessfulRun_oneCase_returnsTwoOutcomes() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)

        val run =
            harness(
                liveProperty = "true",
                caseCountProperty = "1",
                startDirectory = repo,
                environment = bothKeyEnvironment(),
                httpExecutorFactory = { blockedHttpExecutor() },
                googleProviderFactory = { _, _ -> FakeRecordingProvider(BenchmarkProviderId.GOOGLE) },
                graphHopperProviderFactory = { _, _ -> FakeRecordingProvider(BenchmarkProviderId.GRAPHHOPPER) },
            ).run()

        val result = requireNotNull(run.result)
        assertEquals(2, result.providerOutcomes.size)
        assertEquals(2, result.budgetSnapshot.totalReserved)
        assertEquals(1, result.budgetSnapshot.googleReserved)
        assertEquals(1, result.budgetSnapshot.graphHopperReserved)
        assertEquals("live-difc-marina", result.caseResults.single().caseId)
    }

    @Test
    fun run_fakeSuccessfulRun_threeCases_returnsSixOutcomes() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)

        val run =
            harness(
                liveProperty = "true",
                caseCountProperty = "3",
                startDirectory = repo,
                environment = bothKeyEnvironment(),
                httpExecutorFactory = { blockedHttpExecutor() },
                googleProviderFactory = { _, _ -> FakeRecordingProvider(BenchmarkProviderId.GOOGLE) },
                graphHopperProviderFactory = { _, _ -> FakeRecordingProvider(BenchmarkProviderId.GRAPHHOPPER) },
            ).run()

        val result = requireNotNull(run.result)
        assertEquals(6, result.providerOutcomes.size)
        assertEquals(6, result.budgetSnapshot.totalReserved)
        assertEquals(3, result.budgetSnapshot.googleReserved)
        assertEquals(3, result.budgetSnapshot.graphHopperReserved)
        assertEquals(Stage0BLiveCases.all.take(3).map { it.caseId }, result.caseResults.map { it.caseId })
    }

    @Test
    fun run_fakeSuccessfulRun_returnsTwentyOutcomes() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)

        val run =
            harness(
                liveProperty = "true",
                caseCountProperty = "10",
                startDirectory = repo,
                environment = bothKeyEnvironment(),
                httpExecutorFactory = { blockedHttpExecutor() },
                googleProviderFactory = { _, _ -> FakeRecordingProvider(BenchmarkProviderId.GOOGLE) },
                graphHopperProviderFactory = { _, _ -> FakeRecordingProvider(BenchmarkProviderId.GRAPHHOPPER) },
            ).run()

        val result = requireNotNull(run.result)
        assertEquals(20, result.providerOutcomes.size)
        assertEquals(20, result.budgetSnapshot.totalReserved)
        assertEquals(10, result.budgetSnapshot.googleReserved)
        assertEquals(10, result.budgetSnapshot.graphHopperReserved)
        assertEquals(0, result.providerFailureCount)
    }

    @Test
    fun run_fakeProviderFailure_recordedInFinalResult() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)
        val failingCase = Stage0BLiveCases.all.first()

        val run =
            harness(
                liveProperty = "true",
                caseCountProperty = "10",
                startDirectory = repo,
                environment = bothKeyEnvironment(),
                httpExecutorFactory = { blockedHttpExecutor() },
                googleProviderFactory = { _, _ ->
                    FakeRecordingProvider(BenchmarkProviderId.GOOGLE) { case, permit ->
                        if (case.caseId == failingCase.caseId) {
                            ProviderFetchResult.Failed(
                                providerId = BenchmarkProviderId.GOOGLE,
                                caseId = case.caseId,
                                requestId = permit.requestId,
                                errorCategory = FailureCategory.NETWORK,
                                message = "offline simulated network failure",
                            )
                        } else {
                            successResult(case, BenchmarkProviderId.GOOGLE)
                        }
                    }
                },
                graphHopperProviderFactory = { _, _ -> FakeRecordingProvider(BenchmarkProviderId.GRAPHHOPPER) },
            ).run()

        val result = requireNotNull(run.result)
        assertEquals(1, result.providerFailureCount)
        val caseResult = result.caseResults.first { it.caseId == failingCase.caseId }
        assertTrue(caseResult.googleOutcome.result is ProviderFetchResult.Failed)
        assertTrue(caseResult.graphHopperOutcome.result is ProviderFetchResult.Success)
        assertNotNull(caseResult.analysisResult)
    }

    @Test
    fun run_reportContainsNoFakeSecrets() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)

        val run =
            harness(
                liveProperty = "true",
                caseCountProperty = "10",
                startDirectory = repo,
                environment = bothKeyEnvironment(),
                httpExecutorFactory = { blockedHttpExecutor() },
                googleProviderFactory = { _, _ -> FakeRecordingProvider(BenchmarkProviderId.GOOGLE) },
                graphHopperProviderFactory = { _, _ -> FakeRecordingProvider(BenchmarkProviderId.GRAPHHOPPER) },
            ).run()

        val report = requireNotNull(run.result).formatReadable()
        assertFalse(report.contains(fakeGoogleKey))
        assertFalse(report.contains(fakeGraphHopperKey))
        assertFalse(run.preflight.config.toString().contains(fakeGoogleKey))
        assertFalse(run.preflight.config.toString().contains(fakeGraphHopperKey))
    }

    @Test
    fun run_usesExactlyStage0BLiveCasesAll() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)

        val run =
            harness(
                liveProperty = "true",
                caseCountProperty = "10",
                startDirectory = repo,
                environment = bothKeyEnvironment(),
                httpExecutorFactory = { blockedHttpExecutor() },
                googleProviderFactory = { _, _ -> FakeRecordingProvider(BenchmarkProviderId.GOOGLE) },
                graphHopperProviderFactory = { _, _ -> FakeRecordingProvider(BenchmarkProviderId.GRAPHHOPPER) },
            ).run()

        val result = requireNotNull(run.result)
        assertEquals(Stage0BLiveCases.all.size, result.requestedCases)
        assertEquals(Stage0BLiveCases.all.map { it.caseId }, result.caseResults.map { it.caseId })
    }

    @Test
    fun run_remainsSequentialAndCapped() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)
        val googleProvider = FakeRecordingProvider(BenchmarkProviderId.GOOGLE)
        val graphHopperProvider = FakeRecordingProvider(BenchmarkProviderId.GRAPHHOPPER)

        val run =
            harness(
                liveProperty = "true",
                caseCountProperty = "10",
                startDirectory = repo,
                environment = bothKeyEnvironment(),
                httpExecutorFactory = { blockedHttpExecutor() },
                googleProviderFactory = { _, _ -> googleProvider },
                graphHopperProviderFactory = { _, _ -> graphHopperProvider },
            ).run()

        val result = requireNotNull(run.result)
        val expectedOrder =
            Stage0BLiveCases.all.flatMap { case ->
                listOf(
                    case.caseId to BenchmarkProviderId.GOOGLE,
                    case.caseId to BenchmarkProviderId.GRAPHHOPPER,
                )
            }
        assertEquals(expectedOrder, result.providerOutcomes.map { it.caseId to it.providerId })
        assertTrue(result.budgetSnapshot.totalReserved <= BenchmarkLiveBudget.TOTAL_CAP)
        assertTrue(result.budgetSnapshot.googleReserved <= BenchmarkLiveBudget.GOOGLE_CAP)
        assertTrue(result.budgetSnapshot.graphHopperReserved <= BenchmarkLiveBudget.GRAPHHOPPER_CAP)
        assertEquals(Stage0BLiveCases.all.size, googleProvider.fetchCount)
        assertEquals(Stage0BLiveCases.all.size, graphHopperProvider.fetchCount)
    }

    @Test
    fun preflight_liveEnvVar_enablesWithoutJvmProperty() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)

        val preflight =
            Stage0BLiveBenchmarkGate.preflight(
                liveProperty = null,
                environment = bothKeyEnvironment() + (STAGE0B_LIVE_ENV to "true"),
                startDirectory = repo,
            )

        assertTrue(preflight.shouldRunLive)
        assertEquals(1, preflight.caseCount)
        assertEquals("live-difc-marina", preflight.selectedCases.single().caseId)
    }

    @Test
    fun preflight_liveTypos_doNotEnable() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)
        val environment = bothKeyEnvironment()

        assertFalse(
            Stage0BLiveBenchmarkGate.preflight("1", environment = environment, startDirectory = repo).shouldRunLive,
        )
        assertFalse(
            Stage0BLiveBenchmarkGate.preflight("yes", environment = environment, startDirectory = repo).shouldRunLive,
        )
        assertFalse(
            Stage0BLiveBenchmarkGate
                .preflight(null, environment = environment + (STAGE0B_LIVE_ENV to "1"), startDirectory = repo)
                .shouldRunLive,
        )
    }

    @Test
    fun preflight_invalidCaseCount_skipsBeforeProviders() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)
        val httpAttempts = AtomicInteger(0)

        val run =
            harness(
                liveProperty = "true",
                caseCountProperty = "5",
                startDirectory = repo,
                environment = bothKeyEnvironment(),
                httpExecutorFactory = countingHttpExecutorFactory(httpAttempts),
            ).run()

        assertFalse(run.preflight.shouldRunLive)
        assertTrue(run.preflight.skipReason!!.contains("Invalid Stage 0B case count"))
        assertNull(run.result)
        assertFalse(run.providersCreated)
        assertEquals(0, httpAttempts.get())
    }

    @Test
    fun preflight_caseCountFromEnv_selectsPilotCases() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)

        val preflight =
            Stage0BLiveBenchmarkGate.preflight(
                liveProperty = "true",
                caseCountProperty = null,
                environment = bothKeyEnvironment() + (STAGE0B_CASE_COUNT_ENV to "3"),
                startDirectory = repo,
            )

        assertTrue(preflight.shouldRunLive)
        assertEquals(3, preflight.caseCount)
        assertEquals(Stage0BLiveCases.all.take(3).map { it.caseId }, preflight.selectedCases.map { it.caseId })
    }

    @Test
    fun run_harnessUsesOnlyPreflightSelectedCases() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)
        val googleProvider = FakeRecordingProvider(BenchmarkProviderId.GOOGLE)
        val graphHopperProvider = FakeRecordingProvider(BenchmarkProviderId.GRAPHHOPPER)

        val run =
            harness(
                liveProperty = "true",
                caseCountProperty = "1",
                startDirectory = repo,
                environment = bothKeyEnvironment() + (STAGE0B_CASE_COUNT_ENV to "10"),
                httpExecutorFactory = { blockedHttpExecutor() },
                googleProviderFactory = { _, _ -> googleProvider },
                graphHopperProviderFactory = { _, _ -> graphHopperProvider },
            ).run()

        val result = requireNotNull(run.result)
        assertEquals(1, run.preflight.caseCount)
        assertEquals(1, result.requestedCases)
        assertEquals(2, result.budgetSnapshot.totalReserved)
        assertEquals(1, googleProvider.fetchCount)
        assertEquals(1, graphHopperProvider.fetchCount)
        assertEquals("live-difc-marina", result.caseResults.single().caseId)
    }

    @Test
    fun preflight_jvmLiveTrue_overridesEnvFalse() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)

        val preflight =
            Stage0BLiveBenchmarkGate.preflight(
                liveProperty = "true",
                environment = bothKeyEnvironment() + (STAGE0B_LIVE_ENV to "false"),
                startDirectory = repo,
            )

        assertTrue(preflight.shouldRunLive)
    }

    @Test
    fun preflight_jvmLiveFalse_overridesEnvTrue() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)

        val preflight =
            Stage0BLiveBenchmarkGate.preflight(
                liveProperty = "false",
                environment = bothKeyEnvironment() + (STAGE0B_LIVE_ENV to "true"),
                startDirectory = repo,
            )

        assertFalse(preflight.shouldRunLive)
    }

    @Test
    fun preflight_blankJvmLive_fallsBackToEnv() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)

        val preflight =
            Stage0BLiveBenchmarkGate.preflight(
                liveProperty = "   ",
                environment = bothKeyEnvironment() + (STAGE0B_LIVE_ENV to "true"),
                startDirectory = repo,
            )

        assertTrue(preflight.shouldRunLive)
    }

    @Test
    fun preflight_jvmCaseCount_overridesEnvCaseCount() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)

        val preflight =
            Stage0BLiveBenchmarkGate.preflight(
                liveProperty = "true",
                caseCountProperty = "1",
                environment = bothKeyEnvironment() + (STAGE0B_CASE_COUNT_ENV to "10"),
                startDirectory = repo,
            )

        assertTrue(preflight.shouldRunLive)
        assertEquals(1, preflight.caseCount)
        assertEquals(listOf("live-difc-marina"), preflight.selectedCases.map { it.caseId })
    }

    @Test
    fun preflight_blankJvmCaseCount_fallsBackToEnv() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)

        val preflight =
            Stage0BLiveBenchmarkGate.preflight(
                liveProperty = "true",
                caseCountProperty = "  ",
                environment = bothKeyEnvironment() + (STAGE0B_CASE_COUNT_ENV to "3"),
                startDirectory = repo,
            )

        assertTrue(preflight.shouldRunLive)
        assertEquals(3, preflight.caseCount)
        assertEquals(Stage0BLiveCases.all.take(3).map { it.caseId }, preflight.selectedCases.map { it.caseId })
    }

    @Test
    fun run_createsNoCaptureFiles() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)
        val benchmarkDir = benchmarkSourceDirectory()
        val before = listCaptureCandidateFiles(benchmarkDir)

        harness(
            liveProperty = "true",
            startDirectory = repo,
            environment = bothKeyEnvironment(),
            httpExecutorFactory = { blockedHttpExecutor() },
            googleProviderFactory = { _, _ -> FakeRecordingProvider(BenchmarkProviderId.GOOGLE) },
            graphHopperProviderFactory = { _, _ -> FakeRecordingProvider(BenchmarkProviderId.GRAPHHOPPER) },
        ).run()

        val after = listCaptureCandidateFiles(benchmarkDir)
        assertEquals(before, after)
    }

    private fun harness(
        liveProperty: String?,
        startDirectory: Path,
        environment: Map<String, String> = emptyMap(),
        caseCountProperty: String? = null,
        httpExecutorFactory: () -> BenchmarkLiveHttpExecutor = { blockedHttpExecutor() },
        googleProviderFactory: ((String, BenchmarkLiveHttpExecutor) -> BenchmarkLiveProvider)? = null,
        graphHopperProviderFactory: ((String, BenchmarkLiveHttpExecutor) -> BenchmarkLiveProvider)? = null,
    ): Stage0BLiveBenchmarkHarness =
        Stage0BLiveBenchmarkHarness(
            liveProperty = liveProperty,
            caseCountProperty = caseCountProperty,
            environment = environment,
            startDirectory = startDirectory,
            httpExecutorFactory = httpExecutorFactory,
            googleProviderFactory = googleProviderFactory,
            graphHopperProviderFactory = graphHopperProviderFactory,
        )

    private fun bothKeyEnvironment(): Map<String, String> =
        mapOf(
            BenchmarkLiveConfig.GOOGLE_MAPS_API_KEY_PROPERTY to fakeGoogleKey,
            BenchmarkLiveConfig.GRAPHHOPPER_API_KEY_PROPERTY to fakeGraphHopperKey,
        )

    private fun createBenchmarkRepositoryRoot(
        withBothKeys: Boolean = false,
        withGoogleKey: Boolean = withBothKeys,
        withGraphHopperKey: Boolean = withBothKeys,
    ): Path {
        val repo = createTempDirectory("stage0b-entry-gate-repo")
        Files.writeString(repo.resolve("settings.gradle.kts"), "// stage0b gate test repo marker")
        if (withGoogleKey || withGraphHopperKey) {
            val content =
                buildString {
                    if (withGoogleKey) {
                        appendLine("${BenchmarkLiveConfig.GOOGLE_MAPS_API_KEY_PROPERTY}=$fakeGoogleKey")
                    }
                    if (withGraphHopperKey) {
                        appendLine("${BenchmarkLiveConfig.GRAPHHOPPER_API_KEY_PROPERTY}=$fakeGraphHopperKey")
                    }
                }
            Files.writeString(repo.resolve("local.properties"), content)
        }
        return repo
    }
}

internal class FakeRecordingProvider(
    override val providerId: BenchmarkProviderId,
    private val handler: (
        UaeRouteBenchmarkCase,
        BenchmarkLiveBudget.ReservationResult.Granted,
    ) -> ProviderFetchResult = { case, permit -> successResult(case, providerId, permit.requestId) },
) : BenchmarkLiveProvider {
    override val displayName: String = providerId.name
    var fetchCount: Int = 0

    override fun fetch(
        case: UaeRouteBenchmarkCase,
        permit: BenchmarkLiveBudget.ReservationResult.Granted,
    ): ProviderFetchResult {
        fetchCount++
        return handler(case, permit)
    }
}

internal fun successResult(
    case: UaeRouteBenchmarkCase,
    providerId: BenchmarkProviderId,
    requestId: String = "${providerId.name.lowercase()}-${case.caseId}-offline",
): ProviderFetchResult.Success {
    val providerLabel = if (providerId == BenchmarkProviderId.GOOGLE) "GOOGLE" else "GRAPHHOPPER"
    return ProviderFetchResult.Success(
        providerId = providerId,
        caseId = case.caseId,
        requestId = requestId,
        candidates =
            listOf(
                BenchmarkRouteCandidate(
                    candidateId = "${case.caseId}-$providerLabel-0",
                    caseId = case.caseId,
                    provider = providerLabel,
                    routeIndex = 0,
                    routeSummary = "Sheikh Zayed Rd/E11",
                    corridorScanText = "Merge onto Sheikh Zayed Rd / E11",
                    routePathPoints =
                        listOf(
                            LatLng(case.originLat, case.originLng),
                            LatLng(case.destinationLat, case.destinationLng),
                        ),
                    distanceMeters = 18_000,
                    durationSeconds = 1_200,
                    tollAed = if (providerLabel == "GOOGLE") 4 else 0,
                    sourceFixture = "live-offline-gate",
                ),
            ),
        httpStatus = 200,
        latencyMs = 1L,
    )
}

internal fun blockedHttpExecutor(): BenchmarkLiveHttpExecutor =
    BenchmarkLiveHttpExecutor.stage0B { throw IOException("offline-http-blocked") }

internal fun countingHttpExecutorFactory(counter: AtomicInteger): () -> BenchmarkLiveHttpExecutor =
    {
        BenchmarkLiveHttpExecutor.stage0B {
            counter.incrementAndGet()
            throw IOException("offline-http-blocked")
        }
    }

internal fun benchmarkSourceDirectory(): Path =
    Path.of("").toAbsolutePath().normalize()
        .resolve("app/src/test/java/com/clearroad/app/benchmark")

internal fun listCaptureCandidateFiles(directory: Path): List<String> {
    if (!Files.isDirectory(directory)) {
        return emptyList()
    }
    return Files.walk(directory)
        .filter { Files.isRegularFile(it) }
        .map { it.fileName.toString() }
        .filter { name ->
            name.contains("capture", ignoreCase = true) ||
                name.startsWith("stage0b-live-", ignoreCase = true)
        }
        .sorted()
        .toList()
}
