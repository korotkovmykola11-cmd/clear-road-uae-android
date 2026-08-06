package com.clearroad.app.benchmark

import com.clearroad.app.RealRouteDebugData
import com.clearroad.app.RouteRecommendationSelection
import com.clearroad.app.RoutesV2ResponseAdapter
import com.clearroad.app.buildDirectionsUrl
import com.clearroad.app.calculateLegacyRouteScore
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.RouteIdentityResolver
import com.clearroad.app.effectiveTollAedForScoring
import com.clearroad.app.estimateFuelCostAed
import com.clearroad.app.estimateTotalRouteCostAed
import com.clearroad.app.extractAllRouteObjectJson
import com.clearroad.app.extractRouteLabelsFromRouteJson
import com.clearroad.app.extractRouteLegsDebugData
import com.google.android.gms.maps.model.LatLng
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.junit.Test

/**
 * FASTEST gate failure investigation — live-downtown-ajman E311 vs E11 fork.
 * Verification only; does not change prod policy or gate thresholds.
 *
 * Each run writes a timestamped capture under [CAPTURE_DIR].
 */
class StageCFastestDowntownAjmanDiagnosticTest {

    @Test
    fun diagnose_liveDowntownAjman_fastestRepeatedCaptures() {
        val apiKey = loadPlacesApiKey() ?: run {
            println("SKIP: PLACES_API_KEY not in local.properties")
            return
        }
        val case = Stage0BLiveCases.all.first { it.caseId == "live-downtown-ajman" }
        repeat(REPEAT_RUNS) { run ->
            if (run > 0) Thread.sleep(INTER_RUN_DELAY_MS)
            val captureAt = Instant.now()
            val report = buildCaseReport(case, apiKey, captureAt, focusMode = PreferenceMode.FASTEST)
            val path = writeCapture("fastest-live-downtown-ajman", captureAt, report)
            println(report)
            println("\nCapture written: ${path.absolutePath}\n")
        }
    }

    @Test
    fun diagnose_neighborFastestE311E11ForkCases() {
        val apiKey = loadPlacesApiKey() ?: run {
            println("SKIP: PLACES_API_KEY not in local.properties")
            return
        }
        listOf(
            "live-ajman-difc",
            "live-sharjah-downtown",
            "live-marina-airport-t3",
            "live-downtown-ajman",
        ).forEach { caseId ->
            val case = Stage0BLiveCases.all.first { it.caseId == caseId }
            val captureAt = Instant.now()
            val report = buildCaseReport(case, apiKey, captureAt, focusMode = PreferenceMode.FASTEST)
            val path = writeCapture("fastest-neighbor-$caseId", captureAt, report)
            println(report)
            println("\nCapture written: ${path.absolutePath}\n")
            Thread.sleep(INTER_RUN_DELAY_MS)
        }
    }

    private fun buildCaseReport(
        case: UaeRouteBenchmarkCase,
        apiKey: String,
        captureAt: Instant,
        focusMode: PreferenceMode,
    ): String {
        val sb = StringBuilder()
        val ts = captureAt.atOffset(ZoneOffset.UTC).format(CAPTURE_TS_FMT)
        sb.appendLine("=".repeat(80))
        sb.appendLine("FASTEST DIAGNOSTIC CAPTURE")
        sb.appendLine("caseId=${case.caseId} mode=$focusMode capturedUtc=$ts")
        sb.appendLine(
            "origin=${case.originLabel} (${case.originLat}, ${case.originLng}) " +
                "dest=${case.destinationLabel} (${case.destinationLat}, ${case.destinationLng})",
        )
        sb.appendLine("=".repeat(80))

        val legacyRaw =
            fetchGetBlocking(
                buildDirectionsUrl(
                    LatLng(case.originLat, case.originLng),
                    LatLng(case.destinationLat, case.destinationLng),
                ),
            ) ?: return sb.appendLine("ERROR: Legacy fetch failed").toString()
        val legacyRoutes = extractRouteLegsDebugData(legacyRaw)
        val legacyRouteJsons = extractAllRouteObjectJson(legacyRaw)

        val v2Raw = fetchRoutesV2Raw(case, apiKey) ?: return sb.appendLine("ERROR: V2 fetch failed").toString()
        val v2Routes = RoutesV2ResponseAdapter.extractRouteLegsDebugData(v2Raw)
        val v2RouteJsons = extractAllRouteObjectJson(v2Raw)

        val legacyWinner = RouteRecommendationSelection.pickRecommendedRouteIndex(legacyRoutes, focusMode)
        val v2Winner = RouteRecommendationSelection.pickRecommendedRouteIndex(v2Routes, focusMode)

        sb.appendLine("\n--- RAW ROUTE LABELS (from provider JSON) ---")
        appendRawRouteLabelsTable(
            sb = sb,
            provider = "Legacy Directions",
            routes = legacyRoutes,
            routeJsons = legacyRouteJsons,
            winnerIndex = legacyWinner,
        )
        appendRawRouteLabelsTable(
            sb = sb,
            provider = "Routes V2 computeRoutes",
            routes = v2Routes,
            routeJsons = v2RouteJsons,
            winnerIndex = v2Winner,
        )

        sb.appendLine("\n--- FULL ROUTE SET BREAKDOWN (all indices, winner marked) ---")
        appendSideBreakdown(sb, "Legacy", legacyRoutes, focusMode, legacyWinner)
        appendSideBreakdown(sb, "V2", v2Routes, focusMode, v2Winner)

        sb.appendLine("\n--- ACTUAL WINNER CROSS-API COMPARISON ---")
        appendWinnerComparison(
            sb = sb,
            legacyRoutes = legacyRoutes,
            v2Routes = v2Routes,
            legacyWinner = legacyWinner,
            v2Winner = v2Winner,
            focusMode = focusMode,
        )

        val legacySnap = StageCBaselineCapture.captureCase(
            caseId = case.caseId,
            originLabel = case.originLabel,
            destinationLabel = case.destinationLabel,
            routes = legacyRoutes,
            source = "legacy",
        )
        val v2Snap = StageCBaselineCapture.captureCase(
            caseId = case.caseId,
            originLabel = case.originLabel,
            destinationLabel = case.destinationLabel,
            routes = v2Routes,
            source = "v2-live",
        )

        sb.appendLine("\n--- FASTEST WINNER SELECTION (gate snapshot) ---")
        sb.appendLine("Legacy pickRecommendedRouteIndex(FASTEST)=route[$legacyWinner]")
        sb.appendLine("V2 pickRecommendedRouteIndex(FASTEST)=route[$v2Winner]")
        legacySnap?.let {
            sb.appendLine(
                "Legacy winner corridor=${it.fastest.corridor} geom=${it.fastest.geomFp} " +
                    "dur=${it.fastest.durationSec}s dist=${it.fastest.distanceM}m toll=${it.fastest.tollAed}",
            )
        }
        v2Snap?.let {
            sb.appendLine(
                "V2 winner corridor=${it.fastest.corridor} geom=${it.fastest.geomFp} " +
                    "dur=${it.fastest.durationSec}s dist=${it.fastest.distanceM}m toll=${it.fastest.tollAed}",
            )
        }

        if (legacySnap != null && v2Snap != null) {
            val legacyPath = legacyRoutes[legacySnap.fastest.index.coerceIn(0, legacyRoutes.lastIndex)].routePathPoints
            val v2Path = v2Routes[v2Snap.fastest.index.coerceIn(0, v2Routes.lastIndex)].routePathPoints
            val geom = GeometrySimilarity.compare(legacyPath, v2Path)
            val comparison =
                StageCBaselineCapture.compareWinners(
                    caseId = case.caseId,
                    legacy = legacySnap.fastest,
                    v2 = v2Snap.fastest,
                    mode = focusMode,
                    allLegacyRoutes = legacyRoutes,
                    allV2Routes = v2Routes,
                    legacyWinnerPath = legacyPath,
                    v2WinnerPath = v2Path,
                )
            sb.appendLine(
                "\nWinner-to-winner geometry overlap: ${"%.0f".format(geom.sharedPercentageOfShorter * 100)}% " +
                    "valid=${geom.isValid} classification=$comparison",
            )
        }

        sb.appendLine("\n--- PAIRWISE GEOMETRY (Legacy alt ↔ V2 alt, ≥70%) ---")
        for (li in legacyRoutes.indices) {
            for (vi in v2Routes.indices) {
                val g = GeometrySimilarity.compare(
                    legacyRoutes[li].routePathPoints,
                    v2Routes[vi].routePathPoints,
                )
                if (g.isValid && g.sharedPercentageOfShorter >= 0.70) {
                    sb.appendLine(
                        "  Legacy[$li] ${StageCBaselineCapture.primaryCorridor(legacyRoutes[li])} ↔ " +
                            "V2[$vi] ${StageCBaselineCapture.primaryCorridor(v2Routes[vi])} " +
                            "shared=${"%.0f".format(g.sharedPercentageOfShorter * 100)}%",
                    )
                }
            }
        }

        sb.appendLine("\n--- BASELINE REFERENCE (docs/stage-c-baseline.md §12) ---")
        sb.appendLine(
            "Historical Legacy FASTEST: idx=0 E311 (geom b607eee9); " +
                "Legacy CALM: idx=1 E11 (cross-mode fork documented).",
        )
        return sb.toString()
    }

    private fun appendRawRouteLabelsTable(
        sb: StringBuilder,
        provider: String,
        routes: List<RealRouteDebugData>,
        routeJsons: List<String>,
        winnerIndex: Int,
    ) {
        sb.appendLine("\n  provider=$provider (${routes.size} routes)")
        val identities = RouteIdentityResolver.resolveAll(routes)
        routes.forEachIndexed { i, route ->
            val routeJson = routeJsons.getOrNull(i)
            val labelsText =
                when {
                    routeJson == null -> "routeLabels=ABSENT(no route JSON)"
                    else -> formatRouteLabelsDiagnostic(routeJson)
                }
            val id = identities.getOrNull(i)
            val base = route.baseDurationSeconds
            val traffic = route.durationInTrafficSeconds ?: route.durationSeconds
            val delay = (traffic - base).coerceAtLeast(0)
            val winnerMark = if (i == winnerIndex) " **WINNER**" else ""
            sb.appendLine(
                "  [$i]$winnerMark provider=$provider $labelsText " +
                    "identityKey=${id?.stableKey ?: "—"} corridor=${StageCBaselineCapture.primaryCorridor(route)} " +
                    "duration=${route.durationSeconds}s static=${base}s trafficDelta=${delay}s " +
                    "distance=${route.distanceMeters}m geom=${StageCBaselineCapture.shortGeomFp(route.routePathPoints)}",
            )
        }
    }

    /**
     * Distinguishes absent `routeLabels` key, empty `[]`, and populated label lists.
     * Diagnostic-only — not used in production adapter.
     */
    private fun formatRouteLabelsDiagnostic(routeJson: String): String {
        val key = "\"routeLabels\""
        val idx = routeJson.indexOf(key)
        if (idx == -1) return "routeLabels=ABSENT"
        val bracket = routeJson.indexOf('[', idx + key.length)
        if (bracket == -1) return "routeLabels=ABSENT(malformed)"
        val close = routeJson.indexOf(']', bracket)
        if (close == -1) return "routeLabels=ABSENT(malformed)"
        val arrayBody = routeJson.substring(bracket + 1, close).trim()
        if (arrayBody.isEmpty()) return "routeLabels=EMPTY[]"
        val labels = extractRouteLabelsFromRouteJson(routeJson)
        return if (labels.isEmpty()) {
            "routeLabels=EMPTY[]"
        } else {
            "routeLabels=[${labels.joinToString(", ")}]"
        }
    }

    private fun appendSideBreakdown(
        sb: StringBuilder,
        label: String,
        routes: List<RealRouteDebugData>,
        mode: PreferenceMode,
        winnerIndex: Int,
    ) {
        sb.appendLine("\n  === $label (winner=route[$winnerIndex]) ===")
        val identities = RouteIdentityResolver.resolveAll(routes)
        routes.forEachIndexed { i, route ->
            appendRouteMetricsLine(
                sb = sb,
                prefix = "  route[$i]",
                route = route,
                allRoutes = routes,
                mode = mode,
                identity = identities.getOrNull(i),
                isWinner = i == winnerIndex,
            )
        }
    }

    private fun appendWinnerComparison(
        sb: StringBuilder,
        legacyRoutes: List<RealRouteDebugData>,
        v2Routes: List<RealRouteDebugData>,
        legacyWinner: Int,
        v2Winner: Int,
        focusMode: PreferenceMode,
    ) {
        val legacyIdentities = RouteIdentityResolver.resolveAll(legacyRoutes)
        val v2Identities = RouteIdentityResolver.resolveAll(v2Routes)
        sb.appendLine("  Legacy winner route[$legacyWinner]:")
        appendRouteMetricsLine(
            sb = sb,
            prefix = "   ",
            route = legacyRoutes[legacyWinner.coerceIn(0, legacyRoutes.lastIndex)],
            allRoutes = legacyRoutes,
            mode = focusMode,
            identity = legacyIdentities.getOrNull(legacyWinner),
            isWinner = true,
        )
        sb.appendLine("  V2 winner route[$v2Winner]:")
        appendRouteMetricsLine(
            sb = sb,
            prefix = "   ",
            route = v2Routes[v2Winner.coerceIn(0, v2Routes.lastIndex)],
            allRoutes = v2Routes,
            mode = focusMode,
            identity = v2Identities.getOrNull(v2Winner),
            isWinner = true,
        )

        val lId = legacyIdentities.getOrNull(legacyWinner)?.stableKey
        val vId = v2Identities.getOrNull(v2Winner)?.stableKey
        sb.appendLine(
            "  identityKey match=${lId == vId && lId != null} " +
                "(Legacy=$lId V2=$vId)",
        )

        val l = legacyRoutes[legacyWinner.coerceIn(0, legacyRoutes.lastIndex)]
        val v = v2Routes[v2Winner.coerceIn(0, v2Routes.lastIndex)]
        val lCor = StageCBaselineCapture.primaryCorridor(l)
        val vCor = StageCBaselineCapture.primaryCorridor(v)
        val lTraffic = l.durationInTrafficSeconds ?: l.durationSeconds
        val vTraffic = v.durationInTrafficSeconds ?: v.durationSeconds
        sb.appendLine(
            "  corridor fork=${lCor != vCor} (Legacy=$lCor V2=$vCor) " +
                "index fork=${legacyWinner != v2Winner} " +
                "winner traffic delta=${lTraffic - vTraffic}s",
        )
        sb.appendLine(
            "  FASTEST score Legacy=${"%.3f".format(fastestScore(l, legacyRoutes))} " +
                "V2=${"%.3f".format(fastestScore(v, v2Routes))}",
        )
    }

    private fun appendRouteMetricsLine(
        sb: StringBuilder,
        prefix: String,
        route: RealRouteDebugData,
        allRoutes: List<RealRouteDebugData>,
        mode: PreferenceMode,
        identity: com.clearroad.app.domain.RouteIdentity?,
        isWinner: Boolean,
    ) {
        val base = route.baseDurationSeconds
        val traffic = route.durationInTrafficSeconds ?: route.durationSeconds
        val delay = (traffic - base).coerceAtLeast(0)
        val corridor = StageCBaselineCapture.primaryCorridor(route)
        val geom = StageCBaselineCapture.shortGeomFp(route.routePathPoints)
        val distanceKm = route.distanceMeters / 1000.0
        val toll = effectiveTollAedForScoring(route, allRoutes)
        val totalCost = estimateTotalRouteCostAed(toll, estimateFuelCostAed(distanceKm)).toDouble()
        val score =
            calculateLegacyRouteScore(
                mode = mode,
                durationMinutes = route.durationSeconds / 60,
                distanceKm = distanceKm,
                tollAed = toll.toDouble(),
                totalCostAed = totalCost,
            )
        val winnerTag = if (isWinner) " **WINNER**" else ""
        sb.appendLine(
            "$prefix$winnerTag corridor=$corridor geom=$geom identityKey=${identity?.stableKey ?: "—"} " +
                "identity=${identity?.primaryName ?: "—"}",
        )
        sb.appendLine(
            "$prefix  duration=${route.durationSeconds}s (${route.durationText}) " +
                "static=${base}s traffic=${traffic}s trafficDelta=${delay}s " +
                "distance=${route.distanceMeters}m toll=${toll}aed",
        )
        sb.appendLine(
            "$prefix  ${mode.name} score=${"%.3f".format(score)} " +
                "(durationMin=${route.durationSeconds / 60} distKm=${"%.2f".format(distanceKm)} " +
                "totalCost=${"%.2f".format(totalCost)})",
        )
    }

    private fun fastestScore(route: RealRouteDebugData, all: List<RealRouteDebugData>): Double {
        val distanceKm = route.distanceMeters / 1000.0
        val toll = effectiveTollAedForScoring(route, all)
        val totalCost = estimateTotalRouteCostAed(toll, estimateFuelCostAed(distanceKm)).toDouble()
        return calculateLegacyRouteScore(
            mode = PreferenceMode.FASTEST,
            durationMinutes = route.durationSeconds / 60,
            distanceKm = distanceKm,
            tollAed = toll.toDouble(),
            totalCostAed = totalCost,
        )
    }

    private fun writeCapture(prefix: String, at: Instant, body: String): File {
        val dir = resolveCaptureDir()
        dir.mkdirs()
        val stamp = at.atOffset(ZoneOffset.UTC).format(FILE_TS_FMT)
        val file = File(dir, "$prefix-$stamp.txt")
        file.writeText(body)
        return file
    }

    private fun resolveCaptureDir(): File {
        var root = File(System.getProperty("user.dir") ?: ".")
        while (true) {
            if (File(root, "settings.gradle.kts").exists()) break
            val parent = root.parentFile ?: break
            root = parent
        }
        return File(root, CAPTURE_DIR)
    }

    private fun fetchRoutesV2Raw(case: UaeRouteBenchmarkCase, apiKey: String): String? {
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

    companion object {
        private const val CAPTURE_DIR = "docs/stage-35-7-gate/captures"
        private const val REPEAT_RUNS = 3
        private const val INTER_RUN_DELAY_MS = 5_000L
        private val CAPTURE_TS_FMT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        private val FILE_TS_FMT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss'Z'")
    }
}
