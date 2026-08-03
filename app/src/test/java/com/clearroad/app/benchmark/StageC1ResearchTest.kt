package com.clearroad.app.benchmark

import com.clearroad.app.DriverStressAudit
import com.clearroad.app.RealRouteDebugData
import com.clearroad.app.RouteRecommendationSelection
import com.clearroad.app.RoutesV2ManeuverPolicy
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
 * Stage C.1 research — SmoothDrive input parity + Policy B false override audit.
 * Verification only; does not switch prod fetch.
 */
class StageC1ResearchTest {

    @Test
    fun stageC1_dualFetch_smoothDriveParity_and_policyBAudit() {
        val apiKey = loadPlacesApiKey()
        if (apiKey == null) {
            println("SKIP Stage C.1: PLACES_API_KEY not in local.properties")
            return
        }

        val caseDefs = loadAllPreFlipCaseDefs()
        require(caseDefs.size == 15) { "Expected 15 cases" }

        val parityRows = mutableListOf<MatchedRouteParityRow>()
        val policyRows = mutableListOf<PolicyBAuditRow>()
        val unmatchedLegacy = mutableListOf<String>()
        val unmatchedV2 = mutableListOf<String>()

        caseDefs.forEach { def ->
            val legacyRoutes = loadLegacyRoutes(def, apiKey) ?: run {
                println("SKIP ${def.case.caseId}: Legacy unavailable")
                return@forEach
            }
            val v2Raw = fetchRoutesV2Raw(def.case, apiKey) ?: run {
                println("SKIP ${def.case.caseId}: V2 unavailable")
                return@forEach
            }
            val v2RoutesRaw = RoutesV2ResponseAdapter.extractRouteLegsDebugData(v2Raw)
            val v2RoutesRecal = v2RoutesRaw

            val matches = matchRoutesByGeometry(legacyRoutes, v2RoutesRecal)
            matches.forEach { match ->
                parityRows += buildParityRow(def.case.caseId, match, legacyRoutes, v2RoutesRecal)
            }
            legacyRoutes.indices.filter { li ->
                matches.none { it.legacyIndex == li }
            }.forEach { li ->
                unmatchedLegacy +=
                    "${def.case.caseId} L[$li] ${StageCBaselineCapture.primaryCorridor(legacyRoutes[li])} " +
                        "geom=${StageCBaselineCapture.shortGeomFp(legacyRoutes[li].routePathPoints)}"
            }
            v2RoutesRecal.indices.filter { vi ->
                matches.none { it.v2Index == vi }
            }.forEach { vi ->
                unmatchedV2 +=
                    "${def.case.caseId} V2[$vi] ${StageCBaselineCapture.primaryCorridor(v2RoutesRecal[vi])} " +
                        "geom=${StageCBaselineCapture.shortGeomFp(v2RoutesRecal[vi].routePathPoints)}"
            }

            policyRows += auditPolicyB(def.case.caseId, legacyRoutes, v2RoutesRecal, v2RoutesRaw)
        }

        val report = buildReport(parityRows, policyRows, unmatchedLegacy, unmatchedV2)
        println(report)

        val outFile = File("build/reports/stage-c1-research-report.txt")
        outFile.parentFile?.mkdirs()
        outFile.writeText(report)
        println("\nReport: ${outFile.absolutePath}")
    }

    // --- Investigation 1: SmoothDrive input parity ---

    private data class RouteMatch(
        val legacyIndex: Int,
        val v2Index: Int,
        val sharedPct: Double,
        val corridor: String,
    )

    private data class MatchedRouteParityRow(
        val caseId: String,
        val legacyIdx: Int,
        val v2Idx: Int,
        val corridor: String,
        val sharedPct: Double,
        val legacyBaseSec: Int,
        val v2BaseSec: Int,
        val legacyTrafficSec: Int,
        val v2TrafficSec: Int,
        val legacyDelaySec: Int,
        val v2DelaySec: Int,
        val legacyDelayRatio: Double,
        val v2DelayRatio: Double,
        val delayDeltaSec: Int,
        val delayRatioDelta: Double,
        val legacyCorridorClass: SmoothDriveScoring.CorridorClass,
        val v2CorridorClass: SmoothDriveScoring.CorridorClass,
        val corridorClassMismatch: Boolean,
        val legacyDistM: Int,
        val v2DistM: Int,
        val distDeltaM: Int,
        val isMotorwayAlt: Boolean,
    )

    private fun matchRoutesByGeometry(
        legacyRoutes: List<RealRouteDebugData>,
        v2Routes: List<RealRouteDebugData>,
        minSharedPct: Double = StageCBaselineCapture.EXPLAINABLE_DIFF_SHARED_PCT,
    ): List<RouteMatch> {
        val matches = mutableListOf<RouteMatch>()
        for (li in legacyRoutes.indices) {
            var best: RouteMatch? = null
            for (vi in v2Routes.indices) {
                if (matches.any { it.v2Index == vi }) continue
                val g = GeometrySimilarity.compare(
                    legacyRoutes[li].routePathPoints,
                    v2Routes[vi].routePathPoints,
                )
                if (!g.isValid || g.sharedPercentageOfShorter < minSharedPct) continue
                val corridor = StageCBaselineCapture.primaryCorridor(legacyRoutes[li])
                val candidate =
                    RouteMatch(
                        legacyIndex = li,
                        v2Index = vi,
                        sharedPct = g.sharedPercentageOfShorter,
                        corridor = corridor,
                    )
                if (best == null || candidate.sharedPct > best.sharedPct) {
                    best = candidate
                }
            }
            if (best != null) matches += best
        }
        return matches
    }

    private fun buildParityRow(
        caseId: String,
        match: RouteMatch,
        legacyRoutes: List<RealRouteDebugData>,
        v2Routes: List<RealRouteDebugData>,
    ): MatchedRouteParityRow {
        val lRoute = legacyRoutes[match.legacyIndex]
        val vRoute = v2Routes[match.v2Index]
        val lInput = RouteRecommendationSelection.toSmoothDriveRouteInput(lRoute)!!
        val vInput = RouteRecommendationSelection.toSmoothDriveRouteInput(vRoute)!!
        val lDelay =
            SmoothDriveScoring.trafficDelaySeconds(lInput.baseDurationSeconds, lInput.durationInTrafficSeconds)
        val vDelay =
            SmoothDriveScoring.trafficDelaySeconds(vInput.baseDurationSeconds, vInput.durationInTrafficSeconds)
        val lRatio = SmoothDriveScoring.trafficDelayRatio(lInput.baseDurationSeconds, lInput.durationInTrafficSeconds)
        val vRatio = SmoothDriveScoring.trafficDelayRatio(vInput.baseDurationSeconds, vInput.durationInTrafficSeconds)
        val lClass = SmoothDriveScoring.classifyCorridor(lInput.corridorText)
        val vClass = SmoothDriveScoring.classifyCorridor(vInput.corridorText)
        val corridor = match.corridor
        return MatchedRouteParityRow(
            caseId = caseId,
            legacyIdx = match.legacyIndex,
            v2Idx = match.v2Index,
            corridor = corridor,
            sharedPct = match.sharedPct,
            legacyBaseSec = lInput.baseDurationSeconds,
            v2BaseSec = vInput.baseDurationSeconds,
            legacyTrafficSec = lInput.durationInTrafficSeconds,
            v2TrafficSec = vInput.durationInTrafficSeconds,
            legacyDelaySec = lDelay,
            v2DelaySec = vDelay,
            legacyDelayRatio = lRatio,
            v2DelayRatio = vRatio,
            delayDeltaSec = vDelay - lDelay,
            delayRatioDelta = vRatio - lRatio,
            legacyCorridorClass = lClass,
            v2CorridorClass = vClass,
            corridorClassMismatch = lClass != vClass,
            legacyDistM = lInput.distanceMeters,
            v2DistM = vInput.distanceMeters,
            distDeltaM = vInput.distanceMeters - lInput.distanceMeters,
            isMotorwayAlt = corridor.contains("E311") || corridor.contains("E611") ||
                (lClass == SmoothDriveScoring.CorridorClass.MOTORWAY && match.legacyIndex > 0),
        )
    }

    // --- Investigation 2: Policy B audit ---

    private data class PolicyBAuditRow(
        val caseId: String,
        val api: String,
        val routeCount: Int,
        val smoothWinner: Int,
        val calmFinal: Int,
        val policySelected: Int,
        val overrideApplied: Boolean,
        val stressGapPct: Int,
        val timePenaltyMin: Int,
        val bestStressIdx: Int,
        val legacyCalmIdx: Int?,
        val v2CalmIfLegacyCrit: Int?,
        val perRouteCrit: List<Int>,
        val perRouteHybridDelta: List<Int>?,
        val overrideVerdict: OverrideVerdict,
        val notes: String,
    )

    private enum class OverrideVerdict {
        NO_OVERRIDE,
        CORRECT_OVERRIDE,
        FALSE_OVERRIDE,
        MISSED_OVERRIDE,
        N_A,
    }

    private fun auditPolicyB(
        caseId: String,
        legacyRoutes: List<RealRouteDebugData>,
        v2RoutesRecal: List<RealRouteDebugData>,
        v2RoutesRaw: List<RealRouteDebugData>,
    ): List<PolicyBAuditRow> {
        val legacyCalm =
            RouteRecommendationSelection.pickRecommendedRouteIndex(legacyRoutes, PreferenceMode.CALM)
        val rows = mutableListOf<PolicyBAuditRow>()

        rows += auditPolicyBSide(
            caseId = caseId,
            api = "LEGACY",
            routes = legacyRoutes,
            critMode = CritMode.LEGACY_PROD,
            legacyCalmIdx = legacyCalm,
            v2CalmIfLegacyCrit = null,
            hybridDeltas = null,
        )
        rows += auditPolicyBSide(
            caseId = caseId,
            api = "V2_RECAL",
            routes = v2RoutesRecal,
            critMode = CritMode.V2_RECALIBRATED,
            legacyCalmIdx = legacyCalm,
            v2CalmIfLegacyCrit =
                calmIndexWithCrit(v2RoutesRecal, CritMode.V2_LEGACY_SUBSTRING),
            hybridDeltas = hybridCritDeltaPerRoute(v2RoutesRaw),
        )
        return rows
    }

    private enum class CritMode {
        LEGACY_PROD,
        V2_RECALIBRATED,
        V2_LEGACY_SUBSTRING,
    }

    private fun auditPolicyBSide(
        caseId: String,
        api: String,
        routes: List<RealRouteDebugData>,
        critMode: CritMode,
        legacyCalmIdx: Int,
        v2CalmIfLegacyCrit: Int?,
        hybridDeltas: List<Int>?,
    ): PolicyBAuditRow {
        val inputs = routes.mapNotNull { RouteRecommendationSelection.toSmoothDriveRouteInput(it) }
        val smoothWinner =
            if (inputs.size == routes.size) {
                SmoothDriveScoring.pickWinnerIndex(inputs)
            } else {
                0
            }
        val stressInputs = buildStressInputs(routes, critMode)
        val policy = CalmStressTieBreak.applyPolicyB(smoothWinner, stressInputs)
        val calmFinal = RouteRecommendationSelection.pickRecommendedRouteIndex(routes, PreferenceMode.CALM)
        val crits = stressInputs.map { it.criticalManeuversCount }

        val verdict =
            when {
                api == "LEGACY" -> OverrideVerdict.N_A
                !policy.overrideApplied && smoothWinner != 0 && calmFinal == 0 ->
                    OverrideVerdict.FALSE_OVERRIDE
                policy.overrideApplied && smoothWinner != 0 && policy.selectedIndex == 0 ->
                    classifyV2Override(caseId, smoothWinner, legacyCalmIdx, v2CalmIfLegacyCrit, policy)
                policy.overrideApplied && legacyCalmIdx == calmFinal ->
                    OverrideVerdict.CORRECT_OVERRIDE
                policy.overrideApplied && v2CalmIfLegacyCrit == legacyCalmIdx && v2CalmIfLegacyCrit != calmFinal ->
                    OverrideVerdict.FALSE_OVERRIDE
                policy.overrideApplied ->
                    OverrideVerdict.CORRECT_OVERRIDE
                smoothWinner != 0 && calmFinal == 0 && policy.stressGapPct >= 20 ->
                    OverrideVerdict.MISSED_OVERRIDE
                else -> OverrideVerdict.NO_OVERRIDE
            }

        val notes =
            buildString {
                if (api == "V2_RECAL" && smoothWinner != 0 && calmFinal == 0) {
                    append("SmoothDrive picked non-default but CALM=route[0]. ")
                }
                if (api == "V2_RECAL" && v2CalmIfLegacyCrit != null && v2CalmIfLegacyCrit != calmFinal) {
                    append("With legacy-substring crit CALM would be route[$v2CalmIfLegacyCrit]. ")
                }
                if (hybridDeltas != null) {
                    val altInflated =
                        hybridDeltas.withIndex().count { (i, d) -> i > 0 && d > 0 }
                    if (altInflated > 0) append("Hybrid inflated crit on $altInflated alt(s). ")
                }
            }.trim()

        return PolicyBAuditRow(
            caseId = caseId,
            api = api,
            routeCount = routes.size,
            smoothWinner = smoothWinner,
            calmFinal = calmFinal,
            policySelected = policy.selectedIndex,
            overrideApplied = policy.overrideApplied,
            stressGapPct = policy.stressGapPct,
            timePenaltyMin = policy.timePenaltyMin,
            bestStressIdx = policy.bestStressRouteIndex,
            legacyCalmIdx = if (api == "V2_RECAL") legacyCalmIdx else null,
            v2CalmIfLegacyCrit = v2CalmIfLegacyCrit,
            perRouteCrit = crits,
            perRouteHybridDelta = hybridDeltas,
            overrideVerdict = verdict,
            notes = notes,
        )
    }

    private fun classifyV2Override(
        caseId: String,
        smoothWinner: Int,
        legacyCalmIdx: Int,
        v2CalmIfLegacyCrit: Int?,
        policy: CalmStressTieBreak.Decision,
    ): OverrideVerdict {
        if (v2CalmIfLegacyCrit == legacyCalmIdx && legacyCalmIdx != 0) {
            return OverrideVerdict.FALSE_OVERRIDE
        }
        if (legacyCalmIdx == 0 && smoothWinner != 0) {
            return OverrideVerdict.CORRECT_OVERRIDE
        }
        return OverrideVerdict.CORRECT_OVERRIDE
    }

    private fun calmIndexWithCrit(
        routes: List<RealRouteDebugData>,
        critMode: CritMode,
    ): Int {
        val inputs = routes.mapNotNull { RouteRecommendationSelection.toSmoothDriveRouteInput(it) }
        if (inputs.size != routes.size) return 0
        val smoothWinner = SmoothDriveScoring.pickWinnerIndex(inputs)
        val stressInputs = buildStressInputs(routes, critMode)
        return CalmStressTieBreak.applyPolicyB(smoothWinner, stressInputs).selectedIndex
    }

    private fun hybridCritDeltaPerRoute(routes: List<RealRouteDebugData>): List<Int> =
        routes.map { route ->
            val legacySub = RoutesV2CriticalManeuverPolicy.countLegacySubstring(route.googleSteps)
            val recal = RoutesV2ManeuverPolicy.countPolicyBStress(route.googleSteps)
            recal - legacySub
        }

    private fun buildStressInputs(
        routes: List<RealRouteDebugData>,
        mode: CritMode,
    ): List<CalmStressTieBreak.RouteStressInput> =
        routes.map { route ->
            val crit =
                when (mode) {
                    CritMode.LEGACY_PROD -> legacyCritical(route)
                    CritMode.V2_RECALIBRATED ->
                        RoutesV2ManeuverPolicy.countPolicyBStress(route.googleSteps)
                    CritMode.V2_LEGACY_SUBSTRING ->
                        RoutesV2CriticalManeuverPolicy.countLegacySubstring(route.googleSteps)
                }
            CalmStressTieBreak.RouteStressInput(
                durationInTrafficMin = minutesRounded(route.durationInTrafficSeconds ?: route.durationSeconds),
                criticalManeuversCount = crit,
            )
        }

    private fun legacyCritical(route: RealRouteDebugData): Int =
        route.criticalManeuversCount
            ?: DriverStressAudit.keywordDistribution(route.googleSteps).values.sum()

    // --- Report ---

    private fun buildReport(
        parityRows: List<MatchedRouteParityRow>,
        policyRows: List<PolicyBAuditRow>,
        unmatchedLegacy: List<String>,
        unmatchedV2: List<String>,
    ): String = buildString {
        appendLine("=== STAGE C.1 RESEARCH REPORT ===")
        appendLine("Matched route pairs (geom ≥70%): ${parityRows.size}")
        appendLine()

        // --- Inv 1 summary ---
        appendLine("## INVESTIGATION 1 — SmoothDrive input parity")
        val withDelay = parityRows.filter { it.legacyDelaySec > 0 || it.v2DelaySec > 0 }
        val v2Higher = withDelay.count { it.delayDeltaSec > 30 }
        val v2Lower = withDelay.count { it.delayDeltaSec < -30 }
        val v2Similar = withDelay.count { kotlin.math.abs(it.delayDeltaSec) <= 30 }
        appendLine("Delay delta (V2−Legacy) on matched pairs with any delay signal:")
        appendLine("  V2 higher by >30s: $v2Higher")
        appendLine("  V2 lower by >30s: $v2Lower")
        appendLine("  Within ±30s: $v2Similar")
        val avgDelayDelta =
            if (withDelay.isNotEmpty()) withDelay.map { it.delayDeltaSec }.average() else 0.0
        val avgRatioDelta =
            if (withDelay.isNotEmpty()) withDelay.map { it.delayRatioDelta }.average() else 0.0
        appendLine("  Mean delay delta: ${"%.0f".format(avgDelayDelta)}s")
        appendLine("  Mean delayRatio delta: ${"%.4f".format(avgRatioDelta)}")

        val classMismatch = parityRows.count { it.corridorClassMismatch }
        appendLine("CorridorClass mismatches (same physical route): $classMismatch/${parityRows.size}")
        parityRows.filter { it.corridorClassMismatch }.forEach { r ->
            appendLine(
                "  ${r.caseId} L[${r.legacyIdx}] ${r.legacyCorridorClass} → V2[${r.v2Idx}] ${r.v2CorridorClass} " +
                    "corridor=${r.corridor}",
            )
        }

        appendLine()
        appendLine("### E311 / motorway alternatives (matched pairs)")
        appendLine("| case | L_idx | corridor | L_delay | V2_delay | Δdelay | L_ratio | V2_ratio | L_class | V2_class |")
        parityRows.filter { it.isMotorwayAlt || it.corridor.contains("E311") }.forEach { r ->
            appendLine(
                "| ${r.caseId} | ${r.legacyIdx} | ${r.corridor} | ${r.legacyDelaySec}s | ${r.v2DelaySec}s | " +
                    "${r.delayDeltaSec}s | ${fmtRatio(r.legacyDelayRatio)} | ${fmtRatio(r.v2DelayRatio)} | " +
                    "${r.legacyCorridorClass} | ${r.v2CorridorClass} |",
            )
        }

        appendLine()
        appendLine("### Full parity table (all matched pairs)")
        appendLine(
            "| case | L→V2 | corridor | shared% | base L/V2 | traffic L/V2 | delay L/V2 | Δdelay | ratio L/V2 | class L/V2 | dist Δm |",
        )
        parityRows.sortedWith(compareBy({ it.caseId }, { it.legacyIdx })).forEach { r ->
            appendLine(
                "| ${r.caseId} | ${r.legacyIdx}→${r.v2Idx} | ${r.corridor} | ${pct(r.sharedPct)} | " +
                    "${r.legacyBaseSec}/${r.v2BaseSec} | ${r.legacyTrafficSec}/${r.v2TrafficSec} | " +
                    "${r.legacyDelaySec}/${r.v2DelaySec} | ${r.delayDeltaSec} | " +
                    "${fmtRatio(r.legacyDelayRatio)}/${fmtRatio(r.v2DelayRatio)} | " +
                    "${r.legacyCorridorClass}/${r.v2CorridorClass} | ${r.distDeltaM} |",
            )
        }

        if (unmatchedLegacy.isNotEmpty() || unmatchedV2.isNotEmpty()) {
            appendLine()
            appendLine("### Unmatched routes (alt-set divergence — not fixable by weight tuning alone)")
            unmatchedLegacy.forEach { appendLine("  Legacy only: $it") }
            unmatchedV2.forEach { appendLine("  V2 only: $it") }
        }

        // SmoothDrive winner flip analysis
        appendLine()
        appendLine("### SmoothDrive winner: Legacy vs V2 (per case, geom-matched inputs)")
        appendLine("| case | L_routes | V2_routes | L_smoothWin | V2_smoothWin | L_calm | V2_calm | flip? |")
        policyRows.filter { it.api == "V2_RECAL" }.forEach { v2 ->
            val leg = policyRows.first { it.caseId == v2.caseId && it.api == "LEGACY" }
            val flip = leg.smoothWinner != v2.smoothWinner || leg.calmFinal != v2.calmFinal
            appendLine(
                "| ${v2.caseId} | ${leg.routeCount} | ${v2.routeCount} | ${leg.smoothWinner} | ${v2.smoothWinner} | " +
                    "${leg.calmFinal} | ${v2.calmFinal} | ${if (flip) "YES" else "no"} |",
            )
        }

        // --- Inv 2 ---
        appendLine()
        appendLine("## INVESTIGATION 2 — Policy B false override audit")
        val v2Policy = policyRows.filter { it.api == "V2_RECAL" }
        val falseOverrides = v2Policy.filter { it.overrideVerdict == OverrideVerdict.FALSE_OVERRIDE }
        val overrideToZero =
            v2Policy.filter { it.overrideApplied && it.smoothWinner != 0 && it.policySelected == 0 }
        appendLine("V2 cases with Policy B override to route[0]: ${overrideToZero.size}")
        appendLine("Classified FALSE_OVERRIDE: ${falseOverrides.size}")

        appendLine()
        appendLine("### Policy B detail (V2 recalibrated)")
        appendLine(
            "| case | routes | smoothWin | calmFinal | override | stressGap% | timePen | bestStress | crit[] | hybridΔ[] | verdict | notes |",
        )
        v2Policy.forEach { r ->
            appendLine(
                "| ${r.caseId} | ${r.routeCount} | ${r.smoothWinner} | ${r.calmFinal} | ${r.overrideApplied} | " +
                    "${r.stressGapPct} | ${r.timePenaltyMin} | ${r.bestStressIdx} | ${r.perRouteCrit} | " +
                    "${r.perRouteHybridDelta ?: "—"} | ${r.overrideVerdict} | ${r.notes} |",
            )
        }

        appendLine()
        appendLine("### Legacy CALM non-default cases (reference behavior)")
        policyRows.filter { it.api == "LEGACY" && it.calmFinal != 0 }.forEach { leg ->
            val v2 = v2Policy.first { it.caseId == leg.caseId }
            appendLine(
                "  ${leg.caseId}: Legacy calm=${leg.calmFinal} crit=${leg.perRouteCrit} | " +
                    "V2 calm=${v2.calmFinal} smoothWin=${v2.smoothWinner} crit=${v2.perRouteCrit} " +
                    "legacyCritWouldBe=${v2.v2CalmIfLegacyCrit} verdict=${v2.overrideVerdict}",
            )
        }

        appendLine()
        appendLine("### Hybrid rule inflation (recal − legacy-substring per route index)")
        var altInflated = 0
        var r0Inflated = 0
        v2Policy.forEach { r ->
            val deltas = r.perRouteHybridDelta ?: return@forEach
            deltas.forEachIndexed { idx, d ->
                if (d > 0) {
                    if (idx == 0) r0Inflated++ else altInflated++
                }
            }
        }
        appendLine("Routes with hybrid inflation (recal > legacy-substring): route[0]=$r0Inflated, alts=$altInflated")
        v2Policy.forEach { r ->
            val deltas = r.perRouteHybridDelta ?: return@forEach
            if (deltas.any { it > 0 }) {
                appendLine("  ${r.caseId}: hybridΔ=$deltas crit=${r.perRouteCrit}")
            }
        }

        appendLine()
        appendLine("## SYNTHESIS (for human decision)")
        append(synthesize(parityRows, v2Policy, unmatchedLegacy, unmatchedV2))
    }.toString()

    private fun synthesize(
        parityRows: List<MatchedRouteParityRow>,
        v2Policy: List<PolicyBAuditRow>,
        unmatchedLegacy: List<String>,
        unmatchedV2: List<String>,
    ): String = buildString {
        val motorwayRows = parityRows.filter { it.isMotorwayAlt || it.corridor.contains("E311") }
        val motorwayV2Higher =
            motorwayRows.count { it.delayDeltaSec > 60 && it.legacyIdx > 0 }
        val classMismatchOnR0 =
            parityRows.count { it.legacyIdx == 0 && it.corridorClassMismatch && it.v2CorridorClass != SmoothDriveScoring.CorridorClass.MOTORWAY }
        val legacyNonDefault = v2Policy.count { (it.legacyCalmIdx ?: 0) != 0 }
        val v2NonDefault = v2Policy.count { it.calmFinal != 0 }
        val falseOverrides = v2Policy.count { it.overrideVerdict == OverrideVerdict.FALSE_OVERRIDE }
        val smoothOnlyFlips =
            v2Policy.count {
                val leg = it.legacyCalmIdx
                it.smoothWinner != 0 && it.calmFinal == 0 && !it.overrideApplied &&
                    (leg != null && leg != 0)
            }

        appendLine("- Legacy CALM non-default: $legacyNonDefault/15; V2: $v2NonDefault/15")
        appendLine("- SmoothDrive-only flips to route[0] (no Policy B override): $smoothOnlyFlips")
        appendLine("- Policy B false overrides: $falseOverrides")
        appendLine("- E311/motorway alts with V2 delay >60s vs Legacy: $motorwayV2Higher/${motorwayRows.size}")
        appendLine("- route[0] corridorClass downgrades (Legacy MOTORWAY → V2 not): $classMismatchOnR0")
        appendLine("- Unmatched alt routes: Legacy=${unmatchedLegacy.size} V2=${unmatchedV2.size}")
    }

    // --- Shared fetch helpers (mirror StageCPreFlipGateTest) ---

    private fun applyRecalibrated(routes: List<RealRouteDebugData>): List<RealRouteDebugData> =
        routes.map { route ->
            route.copy(
                criticalManeuversCount =
                    RoutesV2CriticalManeuverPolicy.countRecalibrated(route.googleSteps),
            )
        }

    private fun loadLegacyRoutes(
        def: PreFlipCaseDef,
        apiKey: String,
    ): List<RealRouteDebugData>? {
        def.legacyFixtureResource?.let { resource ->
            return extractRouteLegsDebugData(readResource(resource)).takeIf { it.isNotEmpty() }
        }
        val url =
            buildDirectionsUrl(
                LatLng(def.case.originLat, def.case.originLng),
                LatLng(def.case.destinationLat, def.case.destinationLng),
            )
        return fetchGetBlocking(url)?.let { extractRouteLegsDebugData(it).takeIf { r -> r.isNotEmpty() } }
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

    private data class PreFlipCaseDef(
        val case: UaeRouteBenchmarkCase,
        val legacyFixtureResource: String?,
    )

    private fun loadAllPreFlipCaseDefs(): List<PreFlipCaseDef> {
        val frozen =
            BenchmarkRunner.loadManifests(readResource("benchmark/cases.json")).map { manifest ->
                PreFlipCaseDef(
                    case = manifest.case,
                    legacyFixtureResource = "benchmark/${manifest.fixtureFile}",
                )
            }
        val live = Stage0BLiveCases.all.map { PreFlipCaseDef(it, null) }
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

    private fun minutesRounded(seconds: Int): Int = (seconds.coerceAtLeast(0) + 59) / 60

    private fun fmtRatio(v: Double): String = "%.3f".format(v)

    private fun pct(v: Double): String = "${"%.0f".format(v * 100)}%"
}
