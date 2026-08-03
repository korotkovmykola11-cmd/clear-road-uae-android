package com.clearroad.app.benchmark

import com.clearroad.app.DriverStressAudit
import com.clearroad.app.RealRouteDebugData
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
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage C pre-flip gate — automated 15 O-D × 3 mode corridor/geometry comparison.
 *
 * One Gradle test run replaces manual 45-comparison UI pass. Requires `PLACES_API_KEY`
 * in `local.properties`. Does not switch prod fetch.
 *
 * Billing per run (approx.):
 * - 15× Routes v2 computeRoutes (Preferred + traffic) ≈ $0.23 at $15/1k
 * - up to 10× Legacy Directions (live cases only; frozen use fixtures) ≈ $0.10 at $10/1k
 * - Total ≈ **$0.33** per full run
 */
class StageCPreFlipGateTest {

    @Test
    fun live_preFlipGate_dualFetch_report45Comparisons() {
        val apiKey = loadPlacesApiKey()
        if (apiKey == null) {
            println("SKIP pre-flip gate: PLACES_API_KEY not found in local.properties")
            return
        }

        val caseDefs = loadAllPreFlipCaseDefs()
        require(caseDefs.size == 15) { "Expected 15 case defs, got ${caseDefs.size}" }

        val rows = mutableListOf<PreFlipComparisonRow>()
        caseDefs.forEach { def ->
            val legacyRoutes = loadLegacyRoutes(def, apiKey) ?: run {
                println("SKIP ${def.case.caseId}: Legacy routes unavailable")
                return@forEach
            }
            val v2Routes = loadV2Routes(def, apiKey) ?: run {
                println("SKIP ${def.case.caseId}: V2 routes unavailable")
                return@forEach
            }
            rows += buildComparisonRow(def, legacyRoutes, v2Routes)
        }

        require(rows.size >= 12) {
            "Too few cases completed (${rows.size}/15). Check network/API key."
        }

        val report = buildReport(rows)
        println(report.text)

        val outFile = File("build/reports/stage-c-preflip-gate-report.txt")
        outFile.parentFile?.mkdirs()
        outFile.writeText(report.text)

        println("\nReport written: ${outFile.absolutePath}")
        println("MANUAL ONLY: visual handoff on cases flagged VISUAL_REVIEW")
        report.visualReviewCaseIds.forEach { id ->
            println("  VISUAL_REVIEW → $id")
        }

        if (!report.summary.gatePass) {
            println(
                "\nWARNING: Pre-flip gate criteria not met " +
                    "(see GATE SUMMARY). Report still valid for review.",
            )
        }

        // Hard fail only on UNEXPLAINED geometry or NO_TOLLS semantic errors.
        assertTrue(
            "UNEXPLAINED differences block Stage C: ${report.summary.unexplained}",
            report.summary.unexplained == 0,
        )
        assertTrue(
            "NO_TOLLS semantic failures: ${report.summary.noTollsSemanticFails}",
            report.summary.noTollsSemanticFails == 0,
        )
    }

    private fun buildComparisonRow(
        def: PreFlipCaseDef,
        legacyRoutes: List<RealRouteDebugData>,
        v2Routes: List<RealRouteDebugData>,
    ): PreFlipComparisonRow {
        val case = def.case
        val v2Source = if (def.v2FixtureResource != null) "v2-frozen" else "v2"
        val legacy = StageCBaselineCapture.captureCase(
            caseId = case.caseId,
            originLabel = case.originLabel,
            destinationLabel = case.destinationLabel,
            routes = legacyRoutes,
            source = "legacy",
        )!!
        val v2 = StageCBaselineCapture.captureCase(
            caseId = case.caseId,
            originLabel = case.originLabel,
            destinationLabel = case.destinationLabel,
            routes = v2Routes,
            source = v2Source,
        )!!
        val modeResults =
            PreferenceMode.entries.associateWith { mode ->
                compareMode(case.caseId, mode, legacyRoutes, v2Routes, legacy, v2)
            }
        return PreFlipComparisonRow(
            case = case,
            legacy = legacy,
            v2 = v2,
            legacyRoutes = legacyRoutes,
            v2Routes = v2Routes,
            modeResults = modeResults,
        )
    }

    private fun compareMode(
        caseId: String,
        mode: PreferenceMode,
        legacyRoutes: List<RealRouteDebugData>,
        v2Routes: List<RealRouteDebugData>,
        legacy: StageCBaselineCapture.CaseBaselineRow,
        v2: StageCBaselineCapture.CaseBaselineRow,
    ): ModeComparisonResult {
        val legacySnap =
            when (mode) {
                PreferenceMode.FASTEST -> legacy.fastest
                PreferenceMode.NO_TOLLS -> legacy.noTolls
                PreferenceMode.CALM -> legacy.calm
            }
        val v2Snap =
            when (mode) {
                PreferenceMode.FASTEST -> v2.fastest
                PreferenceMode.NO_TOLLS -> v2.noTolls
                PreferenceMode.CALM -> v2.calm
            }
        val legacyPath = legacyRoutes[legacySnap.index.coerceIn(0, legacyRoutes.lastIndex)].routePathPoints
        val v2Path = v2Routes[v2Snap.index.coerceIn(0, v2Routes.lastIndex)].routePathPoints
        val result =
            StageCBaselineCapture.compareWinners(
                caseId = caseId,
                legacy = legacySnap,
                v2 = v2Snap,
                mode = mode,
                allLegacyRoutes = legacyRoutes,
                allV2Routes = v2Routes,
                legacyWinnerPath = legacyPath,
                v2WinnerPath = v2Path,
            )
        val geom = GeometrySimilarity.compare(legacyPath, v2Path)
        val (v2NoTollsOk, v2NoTollsNote) =
            if (mode == PreferenceMode.NO_TOLLS) {
                StageCBaselineCapture.evaluateNoTollsSemantic(v2Routes, v2Snap.index)
            } else {
                true to ""
            }
        val needsVisual =
            caseId in VISUAL_SPOT_CHECK_CASE_IDS ||
                (
                    mode == PreferenceMode.CALM &&
                        (
                            v2Snap.index != 0 ||
                                result != StageCBaselineCapture.ComparisonResult.SAME_CORRIDOR
                            )
                    )
        return ModeComparisonResult(
            mode = mode,
            result = result,
            sharedGeomPct = if (geom.isValid) geom.sharedPercentageOfShorter else 0.0,
            v2NoTollsSemanticallyCorrect = v2NoTollsOk,
            v2NoTollsNote = v2NoTollsNote,
            needsVisualReview = needsVisual,
        )
    }

    /** V2 adapter sets Policy B critical counts at parse time ([RoutesV2ManeuverPolicy]). */
    private fun applyV2RecalibratedCriticalCounts(
        routes: List<RealRouteDebugData>,
    ): List<RealRouteDebugData> = routes

    private fun loadV2Routes(
        def: PreFlipCaseDef,
        apiKey: String,
    ): List<RealRouteDebugData>? {
        def.v2FixtureResource?.let { resource ->
            return RoutesV2ResponseAdapter.extractRouteLegsDebugData(readResource(resource))
                .takeIf { it.isNotEmpty() }
        }
        val raw = fetchRoutesV2Raw(def.case, apiKey) ?: return null
        return RoutesV2ResponseAdapter.extractRouteLegsDebugData(raw).takeIf { it.isNotEmpty() }
    }

    private fun loadLegacyRoutes(
        def: PreFlipCaseDef,
        apiKey: String,
    ): List<RealRouteDebugData>? {
        def.legacyFixtureResource?.let { resource ->
            return extractRouteLegsDebugData(readResource(resource))
                .takeIf { it.isNotEmpty() }
        }
        val url =
            buildDirectionsUrl(
                LatLng(def.case.originLat, def.case.originLng),
                LatLng(def.case.destinationLat, def.case.destinationLng),
            )
        val raw = fetchGetBlocking(url) ?: return null
        return extractRouteLegsDebugData(raw).takeIf { it.isNotEmpty() }
    }

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
            if (conn.responseCode !in 200..299) {
                println("V2 HTTP ${conn.responseCode} for ${case.caseId}")
                return null
            }
            conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            println("V2 fetch error ${case.caseId}: ${e.message}")
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun appendPathBSpotChecks(
        out: StringBuilder,
        rows: List<PreFlipComparisonRow>,
    ) {
        PATH_B_SPOT_CHECK_CASE_IDS.forEach { caseId ->
            val row = rows.firstOrNull { it.case.caseId == caseId } ?: run {
                out.appendLine("  $caseId: (not in completed rows)")
                return@forEach
            }
            val calmResult = row.modeResults.getValue(PreferenceMode.CALM)
            val v2Calm = row.v2.calm
            val altGap =
                StageCBaselineCapture.legacyWinnerPathAbsentFromV2Alternatives(
                    legacyWinner = row.legacy.calm,
                    legacyRoutes = row.legacyRoutes,
                    v2Routes = row.v2Routes,
                )
            val inputs = row.v2Routes.mapNotNull { RouteRecommendationSelection.toSmoothDriveRouteInput(it) }
            val smoothWin =
                if (inputs.size == row.v2Routes.size) {
                    SmoothDriveScoring.pickWinnerIndex(inputs)
                } else {
                    -1
                }
            val stressInputs = DriverStressAudit.buildCalmStressInputs(row.v2Routes)
            val policy =
                if (stressInputs != null && smoothWin >= 0) {
                    CalmStressTieBreak.applyPolicyB(smoothWin, stressInputs)
                } else {
                    null
                }
            val corridorClass =
                row.v2Routes.getOrNull(v2Calm.index)?.let { route ->
                    SmoothDriveScoring.classifyCorridor(route.corridorScanText)
                }
            out.appendLine(
                "  ${row.case.caseId}: V2 CALM idx=${v2Calm.index} corridor=${v2Calm.corridor} " +
                    "geom=${v2Calm.geomFp} class=$corridorClass result=${calmResult.result} " +
                    "smoothWin=$smoothWin policyOverride=${policy?.overrideApplied} " +
                    "stressGap=${policy?.stressGapPct}% altSetGap=$altGap",
            )
        }
    }

    private fun buildReport(rows: List<PreFlipComparisonRow>): PreFlipGateReport {
        val modeStats =
            PreferenceMode.entries.associateWith { mode ->
                val results = rows.map { it.modeResults.getValue(mode) }
                ModeGateStats(
                    sameCorridor = results.count { it.result == StageCBaselineCapture.ComparisonResult.SAME_CORRIDOR },
                    explainable =
                        results.count {
                            it.result == StageCBaselineCapture.ComparisonResult.EXPLAINABLE_DIFFERENCE
                        },
                    unexplained =
                        results.count {
                            it.result == StageCBaselineCapture.ComparisonResult.UNEXPLAINED_DIFFERENCE
                        },
                    acceptable = results.count { it.result != StageCBaselineCapture.ComparisonResult.UNEXPLAINED_DIFFERENCE },
                )
            }
        val calmNonDefault = rows.count { it.v2.calm.index != 0 }
        val calmAltSetGaps =
            rows.count { row ->
                StageCBaselineCapture.legacyWinnerPathAbsentFromV2Alternatives(
                    legacyWinner = row.legacy.calm,
                    legacyRoutes = row.legacyRoutes,
                    v2Routes = row.v2Routes,
                )
            }
        val noTollsFails =
            rows.count {
                val r = it.modeResults.getValue(PreferenceMode.NO_TOLLS)
                !r.v2NoTollsSemanticallyCorrect
            }
        val unexplainedTotal =
            rows.sumOf { row ->
                row.modeResults.values.count {
                    it.result == StageCBaselineCapture.ComparisonResult.UNEXPLAINED_DIFFERENCE
                }
            }
        val fastestPass =
            modeStats.getValue(PreferenceMode.FASTEST).acceptable >= 14
        val noTollsPass = noTollsFails == 0
        val calmPass =
            modeStats.getValue(PreferenceMode.CALM).acceptable >= 12 &&
                modeStats.getValue(PreferenceMode.CALM).unexplained == 0 &&
                unexplainedTotal == 0
        val gatePass = fastestPass && noTollsPass && calmPass && unexplainedTotal == 0

        val visualReview =
            rows.filter { row ->
                row.modeResults.values.any { it.needsVisualReview } ||
                    row.case.caseId in VISUAL_SPOT_CHECK_CASE_IDS
            }.map { it.case.caseId }.distinct()

        val text = buildString {
            appendLine("=== STAGE C PRE-FLIP GATE — Path B CALM (automated 15×3 = 45 comparisons) ===")
            appendLine("Cases completed: ${rows.size}/15")
            appendLine()
            appendLine("GATE SUMMARY")
            appendLine("| Mode | SAME | EXPLAINABLE | UNEXPLAINED | acceptable | gate |")
            PreferenceMode.entries.forEach { mode ->
                val s = modeStats.getValue(mode)
                val gate =
                    when (mode) {
                        PreferenceMode.FASTEST -> if (s.acceptable >= 14 && s.unexplained == 0) "PASS" else "FAIL"
                        PreferenceMode.NO_TOLLS ->
                            if (noTollsFails == 0 && s.unexplained == 0) "PASS" else "FAIL"
                        PreferenceMode.CALM ->
                            if (s.acceptable >= 12 && s.unexplained == 0) {
                                "PASS"
                            } else {
                                "FAIL"
                            }
                    }
                appendLine(
                    "| $mode | ${s.sameCorridor} | ${s.explainable} | ${s.unexplained} | " +
                        "${s.acceptable}/15 | $gate |",
                )
            }
            appendLine("CALM V2 non-default: $calmNonDefault/15 (Path B — informational, not gated)")
            appendLine("CALM Legacy winner alt-set gaps (EXPLAINABLE): $calmAltSetGaps/15")
            appendLine("NO_TOLLS V2 semantic fails: $noTollsFails/15")
            appendLine("OVERALL GATE: ${if (gatePass) "PASS" else "FAIL"}")
            appendLine()
            appendLine("PATH B SPOT-CHECK (manual review — 5 cases)")
            appendPathBSpotChecks(this, rows)
            appendLine()
            appendLine()
            appendLine("DETAIL (Legacy vs V2 winners)")
            appendLine("| case | mode | L_idx | L_cor | L_geom | V2_idx | V2_cor | V2_geom | shared% | result |")
            appendLine("|------|------|------:|-------|--------|-------:|--------|---------|--------:|--------|")
            rows.forEach { row ->
                PreferenceMode.entries.forEach { mode ->
                    val l =
                        when (mode) {
                            PreferenceMode.FASTEST -> row.legacy.fastest
                            PreferenceMode.NO_TOLLS -> row.legacy.noTolls
                            PreferenceMode.CALM -> row.legacy.calm
                        }
                    val v =
                        when (mode) {
                            PreferenceMode.FASTEST -> row.v2.fastest
                            PreferenceMode.NO_TOLLS -> row.v2.noTolls
                            PreferenceMode.CALM -> row.v2.calm
                        }
                    val mc = row.modeResults.getValue(mode)
                    appendLine(
                        "| ${row.case.caseId} | $mode | ${l.index} | ${l.corridor} | ${l.geomFp} | " +
                            "${v.index} | ${v.corridor} | ${v.geomFp} | " +
                            "${"%.0f".format(mc.sharedGeomPct * 100)}% | ${mc.result} |",
                    )
                }
            }
            appendLine()
            appendLine("API billing estimate this run:")
            val liveV2Calls = rows.count { it.v2.source == "v2" }
            appendLine("  V2 computeRoutes (live): $liveV2Calls calls × ~\$0.015 ≈ \$${"%.2f".format(liveV2Calls * 0.015)}")
            val frozenV2Calls = rows.count { it.v2.source == "v2-frozen" }
            if (frozenV2Calls > 0) {
                appendLine("  V2 computeRoutes (frozen fixtures): $frozenV2Calls — \$0")
            }
            val liveLegacyCalls = rows.count { it.legacy.source == "legacy" && it.case.caseId.startsWith("live") }
            appendLine("  Legacy Directions (live only): ~$liveLegacyCalls × ~\$0.010 ≈ \$${"%.2f".format(liveLegacyCalls * 0.010)}")
            appendLine("  (4 frozen Legacy cases use fixtures — \$0; stage34-route4 uses paired Legacy+V2 frozen)")
        }
        return PreFlipGateReport(
            text = text.toString(),
            summary =
                GateSummary(
                    gatePass = gatePass,
                    unexplained = unexplainedTotal,
                    noTollsSemanticFails = noTollsFails,
                    calmNonDefault = calmNonDefault,
                ),
            visualReviewCaseIds = visualReview,
        )
    }

    private data class PreFlipCaseDef(
        val case: UaeRouteBenchmarkCase,
        val legacyFixtureResource: String?,
        val v2FixtureResource: String?,
    )

    private data class PreFlipComparisonRow(
        val case: UaeRouteBenchmarkCase,
        val legacy: StageCBaselineCapture.CaseBaselineRow,
        val v2: StageCBaselineCapture.CaseBaselineRow,
        val legacyRoutes: List<RealRouteDebugData>,
        val v2Routes: List<RealRouteDebugData>,
        val modeResults: Map<PreferenceMode, ModeComparisonResult>,
    )

    private data class ModeComparisonResult(
        val mode: PreferenceMode,
        val result: StageCBaselineCapture.ComparisonResult,
        val sharedGeomPct: Double,
        val v2NoTollsSemanticallyCorrect: Boolean,
        val v2NoTollsNote: String,
        val needsVisualReview: Boolean,
    )

    private data class ModeGateStats(
        val sameCorridor: Int,
        val explainable: Int,
        val unexplained: Int,
        val acceptable: Int,
    )

    private data class GateSummary(
        val gatePass: Boolean,
        val unexplained: Int,
        val noTollsSemanticFails: Int,
        val calmNonDefault: Int,
    )

    private data class PreFlipGateReport(
        val text: String,
        val summary: GateSummary,
        val visualReviewCaseIds: List<String>,
    )

    private fun loadAllPreFlipCaseDefs(): List<PreFlipCaseDef> {
        val frozen =
            BenchmarkRunner.loadManifests(readResource("benchmark/cases.json")).map { manifest ->
                PreFlipCaseDef(
                    case = manifest.case,
                    legacyFixtureResource = "benchmark/${manifest.fixtureFile}",
                    v2FixtureResource =
                        manifest.v2FixtureFile?.let { "benchmark/routes-v2-traffic/$it" },
                )
            }
        val live =
            Stage0BLiveCases.all.map { case ->
                PreFlipCaseDef(
                    case = case,
                    legacyFixtureResource = null,
                    v2FixtureResource = null,
                )
            }
        return frozen + live
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

    private fun readResource(path: String): String {
        val stream =
            checkNotNull(javaClass.classLoader.getResourceAsStream(path)) {
                "Missing test resource: $path"
            }
        return stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }

    companion object {
        /** Path B manual spot-check — always review regardless of automated gate. */
        val PATH_B_SPOT_CHECK_CASE_IDS =
            listOf(
                "live-sharjah-downtown",
                "live-ajman-difc",
                "live-downtown-ajman",
                "live-difc-marina",
                "live-marina-deira",
            )

        /** @deprecated use [PATH_B_SPOT_CHECK_CASE_IDS] */
        val VISUAL_SPOT_CHECK_CASE_IDS = PATH_B_SPOT_CHECK_CASE_IDS.toSet()
    }
}
