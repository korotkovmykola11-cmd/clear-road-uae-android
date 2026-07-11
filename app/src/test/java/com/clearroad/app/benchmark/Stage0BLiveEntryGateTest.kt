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

internal data class Stage0BLivePreflight(
    val shouldRunLive: Boolean,
    val skipReason: String?,
    val config: BenchmarkLiveConfig.LoadResult,
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
    fun isLiveFlagEnabled(liveFlag: String? = System.getProperty(STAGE0B_LIVE_PROPERTY)): Boolean =
        liveFlag?.trim()?.equals("true", ignoreCase = true) == true

    fun preflight(
        liveFlag: String? = System.getProperty(STAGE0B_LIVE_PROPERTY),
        environment: Map<String, String> = System.getenv(),
        startDirectory: Path = Path.of("").toAbsolutePath().normalize(),
    ): Stage0BLivePreflight {
        val config = BenchmarkLiveConfig.load(startDirectory = startDirectory, environment = environment)
        if (!isLiveFlagEnabled(liveFlag)) {
            return Stage0BLivePreflight(
                shouldRunLive = false,
                skipReason = "$STAGE0B_LIVE_PROPERTY is not enabled (expected -D$STAGE0B_LIVE_PROPERTY=true)",
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
        return Stage0BLivePreflight(shouldRunLive = true, skipReason = null, config = config)
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
    private val liveFlag: String? = System.getProperty(STAGE0B_LIVE_PROPERTY),
    private val environment: Map<String, String> = System.getenv(),
    private val startDirectory: Path = Path.of("").toAbsolutePath().normalize(),
    private val httpExecutorFactory: () -> BenchmarkLiveHttpExecutor = { BenchmarkLiveHttpExecutor.stage0B() },
    private val googleProviderFactory: ((String, BenchmarkLiveHttpExecutor) -> BenchmarkLiveProvider)? = null,
    private val graphHopperProviderFactory: ((String, BenchmarkLiveHttpExecutor) -> BenchmarkLiveProvider)? = null,
    private val runner: Stage0BLiveRunner = Stage0BLiveRunner(),
    private val cases: List<UaeRouteBenchmarkCase> = Stage0BLiveCases.all,
) {
    fun run(): Stage0BLiveBenchmarkRun {
        val preflight = Stage0BLiveBenchmarkGate.preflight(liveFlag, environment, startDirectory)
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

        val budget = BenchmarkLiveBudget.stage0B()
        val result = runner.run(cases, googleProvider, graphHopperProvider, budget)

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
                liveFlag = null,
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
                liveFlag = "true",
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
                liveFlag = "true",
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
                liveFlag = "true",
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

        harness(liveFlag = null, startDirectory = repo).run()

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
                liveFlag = "false",
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
                liveFlag = "true",
                startDirectory = repo,
                environment = bothKeyEnvironment(),
                httpExecutorFactory = { blockedHttpExecutor() },
                googleProviderFactory = { _, _ -> googleProvider },
                graphHopperProviderFactory = { _, _ -> graphHopperProvider },
            ).run()

        assertTrue(run.preflight.shouldRunLive)
        assertNotNull(run.result)
        assertTrue(run.providersCreated)
        assertEquals(Stage0BLiveCases.all.size, run.result!!.requestedCases)
        assertEquals(googleProvider.fetchCount, Stage0BLiveCases.all.size)
        assertEquals(graphHopperProvider.fetchCount, Stage0BLiveCases.all.size)
    }

    @Test
    fun run_fakeSuccessfulRun_returnsTwentyOutcomes() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)

        val run =
            harness(
                liveFlag = "true",
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
                liveFlag = "true",
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
                liveFlag = "true",
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
                liveFlag = "true",
                startDirectory = repo,
                environment = bothKeyEnvironment(),
                httpExecutorFactory = { blockedHttpExecutor() },
                googleProviderFactory = { _, _ -> FakeRecordingProvider(BenchmarkProviderId.GOOGLE) },
                graphHopperProviderFactory = { _, _ -> FakeRecordingProvider(BenchmarkProviderId.GRAPHHOPPER) },
                cases = Stage0BLiveCases.all,
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
                liveFlag = "true",
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
    fun run_createsNoCaptureFiles() {
        val repo = createBenchmarkRepositoryRoot(withBothKeys = true)
        val benchmarkDir = benchmarkSourceDirectory()
        val before = listCaptureCandidateFiles(benchmarkDir)

        harness(
            liveFlag = "true",
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
        liveFlag: String?,
        startDirectory: Path,
        environment: Map<String, String> = emptyMap(),
        httpExecutorFactory: () -> BenchmarkLiveHttpExecutor = { blockedHttpExecutor() },
        googleProviderFactory: ((String, BenchmarkLiveHttpExecutor) -> BenchmarkLiveProvider)? = null,
        graphHopperProviderFactory: ((String, BenchmarkLiveHttpExecutor) -> BenchmarkLiveProvider)? = null,
        cases: List<UaeRouteBenchmarkCase> = Stage0BLiveCases.all,
    ): Stage0BLiveBenchmarkHarness =
        Stage0BLiveBenchmarkHarness(
            liveFlag = liveFlag,
            environment = environment,
            startDirectory = startDirectory,
            httpExecutorFactory = httpExecutorFactory,
            googleProviderFactory = googleProviderFactory,
            graphHopperProviderFactory = graphHopperProviderFactory,
            cases = cases,
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
