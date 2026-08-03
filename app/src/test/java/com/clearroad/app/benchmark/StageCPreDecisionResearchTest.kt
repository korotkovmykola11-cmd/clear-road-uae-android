package com.clearroad.app.benchmark

import com.clearroad.app.DirectionsStepRecord
import com.clearroad.app.DriverStressAudit
import com.clearroad.app.RouteRecommendationSelection
import com.clearroad.app.RoutesV2ManeuverPolicy
import com.clearroad.app.RoutesV2ResponseAdapter
import com.clearroad.app.buildDirectionsUrl
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.extractRouteLegsDebugData
import com.google.android.gms.maps.model.LatLng
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import org.junit.Test

/**
 * Stage C pre-decision verification (no prod changes):
 * 1. Policy B keyword-map recalibration for Routes v2 [Maneuver] enum
 * 2. Default vs non-default route selection frequency on Legacy data
 */
class StageCPreDecisionResearchTest {

    @Test
    fun research_keywordMapRecalibration_sharjahE11SamePath() {
        val legacyJson = readResource("benchmark/route3-Sharjah-Downtown.json")
        val v2Json = RoutesV2ResponseAdapterTestFixtures.read("live-sharjah-downtown-traffic.json")

        val legacySteps = extractRouteLegsDebugData(legacyJson)[0].googleSteps
        val v2Steps =
            RoutesV2ResponseAdapter.extractRouteLegsDebugData(v2Json)[0].googleSteps

        val legacyBefore = DriverStressAudit.keywordDistribution(legacySteps).values.sum()
        val v2Before = RoutesV2CriticalManeuverPolicy.countLegacySubstring(v2Steps)
        val v2After = RoutesV2CriticalManeuverPolicy.countRecalibrated(v2Steps)

        println("\n=== RESEARCH 1: KEYWORD-MAP RECALIBRATION (Sharjah→Downtown E11 route[0]) ===")
        println("| Policy | Critical count | vs Legacy ($legacyBefore) |")
        println("|--------|----------------:|-------------------------:|")
        println("| Legacy (prod Policy B) | $legacyBefore | baseline |")
        println("| V2 before (substring on enum) | $v2Before | ${pct(v2Before, legacyBefore)} |")
        println("| V2 after (recalibrated) | $v2After | ${pct(v2After, legacyBefore)} |")

        printManeuverAlignmentTable(legacySteps, v2Steps)
        printRoundaboutUturnAnalysis(legacyJson, v2Json)
        printRecalibratedEnumTable()

        val gapBefore = legacyBefore - v2Before
        val gapAfter = legacyBefore - v2After
        println(
            "\nGap closure: before=$gapBefore (${pct(v2Before, legacyBefore)} parity), " +
                "after=$gapAfter (${pct(v2After, legacyBefore)} parity)",
        )
    }

    @Test
    fun research_exportLegacyStageCBaseline_corridorGeometry() {
        val rows = loadLegacyBaselineRows()
        require(rows.size >= 15) { "Expected 15 baseline rows; got ${rows.size}" }

        println("\n=== STAGE C LEGACY BASELINE (corridor / geometry) ===")
        rows.forEach { row -> println(formatBaselineRow(row)) }

        val calmNonDefault = rows.count { it.calm.index != 0 }
        println(
            "\nCALM non-default: $calmNonDefault/${rows.size} " +
                "(${pctCount(calmNonDefault, rows.size)})",
        )
        val noTollsFails = rows.count { !it.noTollsSemanticallyCorrect }
        println("NO_TOLLS semantic fails: $noTollsFails/${rows.size}")
    }

    @Test
    fun research_defaultVsNonDefaultRouteSelection_legacyDecisionEngine() {
        val frozenRows = loadFrozenFixtureSelections()
        val liveRows = loadLiveLegacySelections()

        val allRows = (frozenRows + liveRows).distinctBy { it.caseId }
        require(allRows.size >= 5) { "Need at least 5 O-D cases; got ${allRows.size}" }

        println("\n=== RESEARCH 2: DEFAULT vs NON-DEFAULT ROUTE SELECTION (Legacy prod engine) ===")
        println("Cases: ${allRows.size} (${frozenRows.size} frozen fixtures + ${liveRows.size} live fetch)")
        println()

        PreferenceMode.entries.forEach { mode ->
            val total = allRows.size
            val idx0 = allRows.count { it.winners.getValue(mode) == 0 }
            val idx1 = allRows.count { it.winners.getValue(mode) == 1 }
            val idx2 = allRows.count { it.winners.getValue(mode) >= 2 }
            val nonDefault = total - idx0
            println(
                "$mode: route[0]=$idx0 (${pctCount(idx0, total)}), " +
                    "route[1]=$idx1 (${pctCount(idx1, total)}), " +
                    "route[2+]=$idx2 (${pctCount(idx2, total)}), " +
                    "non-default total=$nonDefault (${pctCount(nonDefault, total)})",
            )
        }

        println("\n| Mode | route[0] | route[1] | route[2+] | non-default |")
        println("|------|---------:|---------:|----------:|------------:|")
        PreferenceMode.entries.forEach { mode ->
            val total = allRows.size
            val idx0 = allRows.count { it.winners.getValue(mode) == 0 }
            val idx1 = allRows.count { it.winners.getValue(mode) == 1 }
            val idx2 = allRows.count { it.winners.getValue(mode) >= 2 }
            val nonDefault = total - idx0
            println(
                "| $mode | ${pctCount(idx0, total)} | ${pctCount(idx1, total)} | " +
                    "${pctCount(idx2, total)} | ${pctCount(nonDefault, total)} |",
            )
        }

        println("\nPer-case winners (FASTEST / NO_TOLLS / CALM):")
        allRows.forEach { row ->
            println(
                "  ${row.caseId} (${row.routeCount} alts): " +
                    "F=${row.winners.getValue(PreferenceMode.FASTEST)} " +
                    "S=${row.winners.getValue(PreferenceMode.NO_TOLLS)} " +
                    "C=${row.winners.getValue(PreferenceMode.CALM)}",
            )
        }
    }

    private fun loadLegacyBaselineRows(): List<StageCBaselineCapture.CaseBaselineRow> {
        val frozen =
            BenchmarkRunner.loadManifests(readResource("benchmark/cases.json")).mapNotNull { manifest ->
                val routes = extractRouteLegsDebugData(readResource("benchmark/${manifest.fixtureFile}"))
                StageCBaselineCapture.captureCase(
                    caseId = manifest.case.caseId,
                    originLabel = manifest.case.originLabel,
                    destinationLabel = manifest.case.destinationLabel,
                    routes = routes,
                    source = "frozen",
                )
            }
        val live = loadLiveLegacyBaselineRows()
        return (frozen + live).distinctBy { it.caseId }
    }

    private fun loadLiveLegacyBaselineRows(): List<StageCBaselineCapture.CaseBaselineRow> {
        val apiKey = loadPlacesApiKey() ?: return emptyList()
        return Stage0BLiveCases.all.mapNotNull { liveCase ->
            val url =
                buildDirectionsUrl(
                    LatLng(liveCase.originLat, liveCase.originLng),
                    LatLng(liveCase.destinationLat, liveCase.destinationLng),
                )
            val raw = fetchGetBlocking(url) ?: return@mapNotNull null
            val routes = extractRouteLegsDebugData(raw)
            StageCBaselineCapture.captureCase(
                caseId = liveCase.caseId,
                originLabel = liveCase.originLabel,
                destinationLabel = liveCase.destinationLabel,
                routes = routes,
                source = "live",
            )
        }
    }

    private fun formatBaselineRow(row: StageCBaselineCapture.CaseBaselineRow): String {
        fun fmt(m: StageCBaselineCapture.ModeWinnerSnapshot, prefix: String): String =
            "$prefix idx=${m.index} corridor=${m.corridor} geom=${m.geomFp} toll=${m.tollAed}aed"
        return buildString {
            append("${row.caseId} (${row.originLabel}→${row.destinationLabel}) alts=${row.routeCount} ")
            append(fmt(row.fastest, "F"))
            append(" | ")
            append(fmt(row.noTolls, "S"))
            append(" toll_note=${row.noTollsSemanticNote}")
            append(" | ")
            append(fmt(row.calm, "C"))
        }
    }

    private fun loadFrozenFixtureSelections(): List<SelectionRow> {
        val casesJson = readResource("benchmark/cases.json")
        val manifests = BenchmarkRunner.loadManifests(casesJson)
        return manifests.mapNotNull { manifest ->
            val json = readResource("benchmark/${manifest.fixtureFile}")
            val routes = extractRouteLegsDebugData(json)
            if (routes.isEmpty()) return@mapNotNull null
            SelectionRow(
                caseId = manifest.case.caseId,
                routeCount = routes.size,
                winners = modeWinners(routes),
                source = "frozen",
            )
        }
    }

    private fun loadLiveLegacySelections(): List<SelectionRow> {
        val apiKey = loadPlacesApiKey() ?: run {
            println("Live legacy fetch skipped: PLACES_API_KEY not in local.properties")
            return emptyList()
        }
        return Stage0BLiveCases.all
            .filter { liveCase ->
                // Skip cases already covered by frozen Stage 0A fixtures
                liveCase.caseId !in FROZEN_CASE_IDS
            }
            .mapNotNull { liveCase ->
                val url =
                    buildDirectionsUrl(
                        LatLng(liveCase.originLat, liveCase.originLng),
                        LatLng(liveCase.destinationLat, liveCase.destinationLng),
                    )
                val raw = fetchGetBlocking(url) ?: return@mapNotNull null
                val routes = extractRouteLegsDebugData(raw)
                if (routes.isEmpty()) return@mapNotNull null
                SelectionRow(
                    caseId = liveCase.caseId,
                    routeCount = routes.size,
                    winners = modeWinners(routes),
                    source = "live",
                )
            }
    }

    private fun modeWinners(
        routes: List<com.clearroad.app.RealRouteDebugData>,
    ): Map<PreferenceMode, Int> =
        PreferenceMode.entries.associateWith { mode ->
            RouteRecommendationSelection.pickRecommendedRouteIndex(routes, mode)
        }

    private fun printManeuverAlignmentTable(
        legacySteps: List<DirectionsStepRecord>,
        v2Steps: List<DirectionsStepRecord>,
    ) {
        println("\nStep alignment (Legacy critical-only steps vs V2 recalibrated):")
        println("| # | Legacy maneuver | critical | V2 maneuver | V2 before | V2 after | notes |")
        println("|---|-----------------|----------|-------------|-----------|----------|-------|")
        val max = maxOf(legacySteps.size, v2Steps.size)
        for (i in 0 until max) {
            val legacy = legacySteps.getOrNull(i)
            val v2 = v2Steps.getOrNull(i)
            val legacyCrit = legacy?.maneuver?.let { DriverStressAudit.matchedKeyword(it) != null } == true
            if (!legacyCrit && legacy != null) continue
            val v2Before = v2?.let { RoutesV2CriticalManeuverPolicy.isCriticalLegacySubstring(it) } == true
            val v2After = v2?.let { RoutesV2CriticalManeuverPolicy.isCriticalRecalibrated(it) } == true
            val note =
                when {
                    legacy?.maneuver?.contains("roundabout") == true &&
                        v2?.maneuver?.startsWith("UTURN") == true ->
                        "roundabout→UTURN taxonomy"
                    legacy?.maneuver?.contains("keep") == true &&
                        v2?.maneuver == "TURN_LEFT" ->
                        "keep-left→TURN_LEFT lane bias"
                    else -> ""
                }
            println(
                "| ${i + 1} | ${legacy?.maneuver ?: "—"} | Y | ${v2?.maneuver ?: "—"} | " +
                    "${if (v2Before) "Y" else "N"} | ${if (v2After) "Y" else "N"} | $note |",
            )
        }
    }

    private fun printRoundaboutUturnAnalysis(legacyJson: String, v2Json: String) {
        println("\nRoundabout vs UTURN cross-route analysis (Sharjah→Downtown all alternatives):")
        val legacyRoutes = extractRouteLegsDebugData(legacyJson)
        val v2Routes = RoutesV2ResponseAdapter.extractRouteLegsDebugData(v2Json)
        for (routeIndex in 0 until maxOf(legacyRoutes.size, v2Routes.size)) {
            val legacyRoute = legacyRoutes.getOrNull(routeIndex)?.googleSteps ?: emptyList()
            val v2Route = v2Routes.getOrNull(routeIndex)?.googleSteps ?: emptyList()
            val legacyRoundabout = legacyRoute.count { it.maneuver?.contains("roundabout") == true }
            val legacyUturn = legacyRoute.count { it.maneuver?.contains("uturn") == true }
            val v2Roundabout =
                v2Route.count {
                    it.maneuver == "ROUNDABOUT_LEFT" || it.maneuver == "ROUNDABOUT_RIGHT"
                }
            val v2Uturn =
                v2Route.count { it.maneuver == "UTURN_LEFT" || it.maneuver == "UTURN_RIGHT" }
            println(
                "  route[$routeIndex]: Legacy roundabout=$legacyRoundabout uturn=$legacyUturn | " +
                    "V2 ROUNDABOUT_*=$v2Roundabout UTURN_*=$v2Uturn",
            )
        }
        println(
            "  Interpretation: V2 uses UTURN_* for loop junctions where Legacy labels roundabout-right; " +
                "ROUNDABOUT_* enum exists but was not emitted on this capture.",
        )
    }

    private fun printRecalibratedEnumTable() {
        println("\nRecalibrated V2 Maneuver policy (full official enum):")
        println("| Maneuver | Critical | Legacy semantic equivalent |")
        println("|----------|----------|----------------------------|")
        OFFICIAL_V2_MANEUVERS.forEach { maneuver ->
            val critical = RoutesV2CriticalManeuverPolicy.isEnumCritical(maneuver)
            val equiv = RoutesV2CriticalManeuverPolicy.semanticEquivalent(maneuver)
            println("| $maneuver | ${if (critical) "Y" else "N"} | $equiv |")
        }
        println(
            "\nHybrid rules (not enum-only): TURN_LEFT/TURN_RIGHT when instructions indicate " +
                "keep-lane / continue-on-corridor; UTURN_* when instructions mention roundabout " +
                "or step ≤${RoutesV2CriticalManeuverPolicy.SHORT_LOOP_METERS}m (loop junction proxy).",
        )
    }

    private data class SelectionRow(
        val caseId: String,
        val routeCount: Int,
        val winners: Map<PreferenceMode, Int>,
        val source: String,
    )

    private fun pct(value: Int, baseline: Int): String {
        if (baseline == 0) return "n/a"
        val ratio = value * 100.0 / baseline
        return "${"%.0f".format(ratio)}%"
    }

    private fun pctCount(count: Int, total: Int): String {
        if (total == 0) return "0%"
        return "${"%.0f".format(count * 100.0 / total)}%"
    }

    private fun readResource(path: String): String {
        val stream =
            checkNotNull(javaClass.classLoader.getResourceAsStream(path)) {
                "Missing test resource: $path"
            }
        return stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
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
                connectTimeout = 25_000
                readTimeout = 25_000
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
        private val FROZEN_CASE_IDS =
            setOf(
                "stage34-route1",
                "stage34-route2",
                "stage34-route3",
                "stage34-route4",
                "stage34-route5",
            )

        /** Official Routes v2 Maneuver enum (google.maps.routing.v2.Maneuver). */
        val OFFICIAL_V2_MANEUVERS =
            listOf(
                "MANEUVER_UNSPECIFIED",
                "TURN_SLIGHT_LEFT",
                "TURN_SHARP_LEFT",
                "UTURN_LEFT",
                "TURN_LEFT",
                "TURN_SLIGHT_RIGHT",
                "TURN_SHARP_RIGHT",
                "UTURN_RIGHT",
                "TURN_RIGHT",
                "STRAIGHT",
                "RAMP_LEFT",
                "RAMP_RIGHT",
                "MERGE",
                "FORK_LEFT",
                "FORK_RIGHT",
                "FERRY",
                "FERRY_TRAIN",
                "ROUNDABOUT_LEFT",
                "ROUNDABOUT_RIGHT",
                "DEPART",
                "NAME_CHANGE",
            )
    }
}

/**
 * Verification-only Policy B recalibration for Routes v2 maneuvers.
 * Not wired to prod — research artifact for Stage C decision.
 */
internal object RoutesV2CriticalManeuverPolicy {

    const val SHORT_LOOP_METERS = RoutesV2ManeuverPolicy.SHORT_LOOP_METERS

    fun countLegacySubstring(steps: List<DirectionsStepRecord>): Int =
        steps.count { isCriticalLegacySubstring(it) }

    fun countRecalibrated(steps: List<DirectionsStepRecord>): Int =
        RoutesV2ManeuverPolicy.countPolicyBStress(steps)

    fun isCriticalLegacySubstring(step: DirectionsStepRecord): Boolean {
        val maneuver = step.maneuver ?: return false
        val normalized = maneuver.lowercase().replace('_', '-')
        return DriverStressAudit.CRITICAL_MANEUVER_KEYWORDS.any { keyword ->
            normalized.contains(keyword.replace(' ', '-'))
        }
    }

    fun isCriticalRecalibrated(step: DirectionsStepRecord): Boolean =
        RoutesV2ManeuverPolicy.isPolicyBStress(step)

    fun isEnumCritical(maneuver: String): Boolean = maneuver in CRITICAL_ENUMS

    /** Direct enum equivalents of Legacy substring keywords (merge, fork, ramp, slight/sharp, roundabout). */
    private val CRITICAL_ENUMS =
        setOf(
            "TURN_SLIGHT_LEFT",
            "TURN_SLIGHT_RIGHT",
            "TURN_SHARP_LEFT",
            "TURN_SHARP_RIGHT",
            "MERGE",
            "RAMP_LEFT",
            "RAMP_RIGHT",
            "FORK_LEFT",
            "FORK_RIGHT",
            "ROUNDABOUT_LEFT",
            "ROUNDABOUT_RIGHT",
        )

    fun semanticEquivalent(maneuver: String): String =
        when (maneuver) {
            "MANEUVER_UNSPECIFIED" -> "—"
            "TURN_SLIGHT_LEFT" -> "slight left"
            "TURN_SLIGHT_RIGHT" -> "slight right"
            "TURN_SHARP_LEFT" -> "sharp left"
            "TURN_SHARP_RIGHT" -> "sharp right"
            "MERGE" -> "merge"
            "RAMP_LEFT", "RAMP_RIGHT" -> "ramp / exit"
            "FORK_LEFT", "FORK_RIGHT" -> "fork"
            "ROUNDABOUT_LEFT", "ROUNDABOUT_RIGHT" -> "roundabout"
            "UTURN_LEFT", "UTURN_RIGHT" -> "roundabout loop (hybrid) / complex junction"
            "TURN_LEFT", "TURN_RIGHT" -> "plain turn (non-critical) OR keep-lane (hybrid)"
            "STRAIGHT", "DEPART", "NAME_CHANGE" -> "non-critical continuity"
            "FERRY", "FERRY_TRAIN" -> "non-critical (not in Legacy list)"
            else -> "—"
        }
}
