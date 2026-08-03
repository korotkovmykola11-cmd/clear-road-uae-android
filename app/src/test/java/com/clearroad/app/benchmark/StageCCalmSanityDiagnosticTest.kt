package com.clearroad.app.benchmark

import com.clearroad.app.DriverStressAudit
import com.clearroad.app.RouteRecommendationSelection
import com.clearroad.app.RoutesV2ResponseAdapter
import com.clearroad.app.buildDirectionsUrl
import com.clearroad.app.domain.CalmStressTieBreak
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.SmoothDriveScoring
import com.clearroad.app.extractRouteLegsDebugData
import com.google.android.gms.maps.model.LatLng
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import org.junit.Test

/**
 * CALM sanity fail investigation — live-ajman-difc (and optional cases).
 * Verification only; does not switch prod fetch.
 */
class StageCCalmSanityDiagnosticTest {

    @Test
    fun diagnose_liveAjmanDifc_calmScoringLegacyVsV2() {
        val apiKey = loadPlacesApiKey()
        if (apiKey == null) {
            println("SKIP: PLACES_API_KEY not in local.properties")
            return
        }
        val case = Stage0BLiveCases.all.first { it.caseId == "live-ajman-difc" }
        diagnoseCase(case, apiKey)
    }

    @Test
    fun diagnose_allCalmNonDefaultBaselineCases() {
        val apiKey = loadPlacesApiKey() ?: run {
            println("SKIP: PLACES_API_KEY not in local.properties")
            return
        }
        listOf(
            "live-difc-marina",
            "live-sharjah-downtown",
            "live-ajman-difc",
            "live-downtown-ajman",
            "live-marina-deira",
        ).forEach { caseId ->
            val case = Stage0BLiveCases.all.first { it.caseId == caseId }
            diagnoseCase(case, apiKey)
        }
    }

    private fun diagnoseCase(
        case: UaeRouteBenchmarkCase,
        apiKey: String,
    ) {
        val legacyRaw =
            fetchGetBlocking(
                buildDirectionsUrl(
                    LatLng(case.originLat, case.originLng),
                    LatLng(case.destinationLat, case.destinationLng),
                ),
            ) ?: run {
                println("Legacy fetch failed for ${case.caseId}")
                return
            }
        val legacyRoutes = extractRouteLegsDebugData(legacyRaw)
        val v2Raw = fetchRoutesV2Raw(case, apiKey) ?: run {
            println("V2 fetch failed for ${case.caseId}")
            return
        }
        val v2RoutesRaw = RoutesV2ResponseAdapter.extractRouteLegsDebugData(v2Raw)
        val v2RoutesRecal = applyRecalibrated(v2RoutesRaw)
        val v2RoutesLegacyCrit = applyLegacySubstringCrit(v2RoutesRaw)

        println("\n${"=".repeat(72)}")
        println("CALM DIAGNOSTIC: ${case.caseId} (${case.originLabel}→${case.destinationLabel})")
        println("=".repeat(72))

        printSide("LEGACY Directions", legacyRoutes, criticalMode = CriticalMode.LEGACY_PROD)
        printSide("V2 computeRoutes + Policy B recalibrated", v2RoutesRecal, criticalMode = CriticalMode.V2_RECALIBRATED)
        printSide("V2 computeRoutes + Legacy substring crit (no recalibration)", v2RoutesLegacyCrit, criticalMode = CriticalMode.V2_LEGACY_SUBSTRING)

        printCrossApiCriticalComparison(legacyRoutes, v2RoutesRecal, v2RoutesLegacyCrit)
    }

    private enum class CriticalMode {
        LEGACY_PROD,
        V2_RECALIBRATED,
        V2_LEGACY_SUBSTRING,
    }

    private fun printSide(
        label: String,
        routes: List<com.clearroad.app.RealRouteDebugData>,
        criticalMode: CriticalMode,
    ) {
        println("\n--- $label (${routes.size} routes) ---")
        val inputs = routes.mapNotNull { RouteRecommendationSelection.toSmoothDriveRouteInput(it) }
        if (inputs.size != routes.size) {
            println("  SmoothDrive inputs incomplete")
            return
        }
        val scores = SmoothDriveScoring.scoreAll(inputs)
        val smoothWinner = SmoothDriveScoring.pickWinnerIndex(inputs)
        val stressInputs = buildStressInputs(routes, criticalMode)
        val policy = CalmStressTieBreak.applyPolicyB(smoothWinner, stressInputs)
        val calmIdx = RouteRecommendationSelection.pickRecommendedRouteIndex(routes, PreferenceMode.CALM)
        if (criticalMode == CriticalMode.V2_RECALIBRATED) {
            val calmIfLegacySubstring =
                calmWithStressOverride(
                    smoothWinner,
                    buildStressInputs(routes, CriticalMode.V2_LEGACY_SUBSTRING),
                )
            println(
                "  (hypothesis) CALM if V2 used legacy-substring crit instead: route[$calmIfLegacySubstring]",
            )
        }

        println(
            "  SmoothDrive winner=route[$smoothWinner] | " +
                "PolicyB selected=route[${policy.selectedIndex}] override=${policy.overrideApplied} | " +
                "pickRecommendedRouteIndex(CALM)=route[$calmIdx]",
        )
        println(
            "  PolicyB: stressGapPct=${policy.stressGapPct}% timePenaltyMin=${policy.timePenaltyMin} " +
                "bestStress=route[${policy.bestStressRouteIndex}]",
        )
        routes.forEachIndexed { index, route ->
            val corridor = StageCBaselineCapture.primaryCorridor(route)
            val geom = StageCBaselineCapture.shortGeomFp(route.routePathPoints)
            val crit = stressInputs[index].criticalManeuversCount
            val score = scores[index]
            val input = inputs[index]
            val delaySec =
                SmoothDriveScoring.trafficDelaySeconds(
                    input.baseDurationSeconds,
                    input.durationInTrafficSeconds,
                )
            println(
                "\n  route[$index] corridor=$corridor geom=$geom " +
                    "dist=${route.distanceMeters}m traffic=${input.durationInTrafficSeconds}s " +
                    "base=${input.baseDurationSeconds}s delaySec=$delaySec crit=$crit",
            )
            println(
                "    SCORE total=${fmt(score.total)} " +
                    "(delayRatio=${fmt(score.delayRatioComponent)} " +
                    "delayMin=${fmt(score.delayMinComponent)} " +
                    "timePen=${fmt(score.timePenaltyComponent)} " +
                    "corridor=${fmt(score.corridorComponent)} " +
                    "dist=${fmt(score.distanceComponent)} " +
                    "inactiveGuard=${score.delaySignalInactiveGuardApplied})",
            )
            println(
                "    corridorClass=${SmoothDriveScoring.classifyCorridor(input.corridorText)} " +
                    "winnerMarkers: smooth=${index == smoothWinner} calm=${index == calmIdx} " +
                    "lowestStress=${index == policy.bestStressRouteIndex}",
            )
        }
    }

    private fun calmWithStressOverride(
        smoothWinner: Int,
        stressInputs: List<CalmStressTieBreak.RouteStressInput>,
    ): Int = CalmStressTieBreak.applyPolicyB(smoothWinner, stressInputs).selectedIndex

    private fun printCrossApiCriticalComparison(
        legacyRoutes: List<com.clearroad.app.RealRouteDebugData>,
        v2Recal: List<com.clearroad.app.RealRouteDebugData>,
        v2LegacySub: List<com.clearroad.app.RealRouteDebugData>,
    ) {
        println("\n--- CRITICAL COUNT MATRIX (per route index — indices NOT aligned across APIs!) ---")
        println("| idx | Legacy prod | V2 recalibrated | V2 legacy-substring | L corridor | V2 corridor |")
        val max = maxOf(legacyRoutes.size, v2Recal.size)
        for (i in 0 until max) {
            val l = legacyRoutes.getOrNull(i)
            val v = v2Recal.getOrNull(i)
            val lCrit = l?.let { legacyCritical(it) } ?: -1
            val vRec = v?.let { RoutesV2CriticalManeuverPolicy.countRecalibrated(it.googleSteps) } ?: -1
            val vSub = v?.let { RoutesV2CriticalManeuverPolicy.countLegacySubstring(it.googleSteps) } ?: -1
            val lCor = l?.let { StageCBaselineCapture.primaryCorridor(it) } ?: "—"
            val vCor = v?.let { StageCBaselineCapture.primaryCorridor(it) } ?: "—"
            println("| $i | $lCrit | $vRec | $vSub | $lCor | $vCor |")
        }
        println("\n--- PAIRWISE GEOMETRY (which Legacy alt matches which V2 route) ---")
        for (li in legacyRoutes.indices) {
            for (vi in v2Recal.indices) {
                val g = GeometrySimilarity.compare(
                    legacyRoutes[li].routePathPoints,
                    v2Recal[vi].routePathPoints,
                )
                if (g.isValid && g.sharedPercentageOfShorter >= 0.70) {
                    val lCor = StageCBaselineCapture.primaryCorridor(legacyRoutes[li])
                    val vCor = StageCBaselineCapture.primaryCorridor(v2Recal[vi])
                    println(
                        "  Legacy[$li] $lCor ↔ V2[$vi] $vCor " +
                            "shared=${"%.0f".format(g.sharedPercentageOfShorter * 100)}%",
                    )
                }
            }
        }
    }

    private fun buildStressInputs(
        routes: List<com.clearroad.app.RealRouteDebugData>,
        mode: CriticalMode,
    ): List<CalmStressTieBreak.RouteStressInput> =
        routes.map { route ->
            val crit =
                when (mode) {
                    CriticalMode.LEGACY_PROD -> legacyCritical(route)
                    CriticalMode.V2_RECALIBRATED ->
                        RoutesV2CriticalManeuverPolicy.countRecalibrated(route.googleSteps)
                    CriticalMode.V2_LEGACY_SUBSTRING ->
                        RoutesV2CriticalManeuverPolicy.countLegacySubstring(route.googleSteps)
                }
            CalmStressTieBreak.RouteStressInput(
                durationInTrafficMin = minutesRounded(route.durationInTrafficSeconds ?: route.durationSeconds),
                criticalManeuversCount = crit,
            )
        }

    private fun legacyCritical(route: com.clearroad.app.RealRouteDebugData): Int =
        route.criticalManeuversCount
            ?: DriverStressAudit.keywordDistribution(route.googleSteps).values.sum()

    private fun applyRecalibrated(
        routes: List<com.clearroad.app.RealRouteDebugData>,
    ): List<com.clearroad.app.RealRouteDebugData> =
        routes.map { route ->
            route.copy(
                criticalManeuversCount =
                    RoutesV2CriticalManeuverPolicy.countRecalibrated(route.googleSteps),
            )
        }

    private fun applyLegacySubstringCrit(
        routes: List<com.clearroad.app.RealRouteDebugData>,
    ): List<com.clearroad.app.RealRouteDebugData> =
        routes.map { route ->
            route.copy(
                criticalManeuversCount =
                    RoutesV2CriticalManeuverPolicy.countLegacySubstring(route.googleSteps),
            )
        }

    private fun minutesRounded(seconds: Int): Int = (seconds.coerceAtLeast(0) + 59) / 60

    private fun fmt(v: Double): String = "%.2f".format(v)

    private fun fetchRoutesV2Raw(
        case: UaeRouteBenchmarkCase,
        apiKey: String,
    ): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn =
                (URL("https://routes.googleapis.com/directions/v2:computeRoutes").openConnection()
                    as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 30_000
                    readTimeout = 30_000
                    RoutesV2ResponseAdapter.buildComputeRoutesHeaders(apiKey).forEach { (k, v) ->
                        setRequestProperty(k, v)
                    }
                }
            val body =
                RoutesV2ResponseAdapter.buildComputeRoutesRequestBody(
                    originLat = case.originLat,
                    originLng = case.originLng,
                    destinationLat = case.destinationLat,
                    destinationLng = case.destinationLng,
                )
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun loadPlacesApiKey(): String? {
        listOf(File("local.properties"), File("../local.properties")).forEach { file ->
            if (!file.exists()) return@forEach
            val key =
                file.readLines()
                    .firstOrNull { it.startsWith("PLACES_API_KEY=") }
                    ?.substringAfter("=")
                    ?.trim()
            if (!key.isNullOrBlank()) return key
        }
        return null
    }

    private fun fetchGetBlocking(url: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 30_000
                readTimeout = 30_000
            }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
