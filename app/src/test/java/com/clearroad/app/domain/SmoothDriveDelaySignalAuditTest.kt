package com.clearroad.app.domain

import com.clearroad.app.RouteRecommendationSelection
import com.clearroad.app.extractRouteLegsDebugData
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage 35.x — audit whether Google `duration_in_traffic > duration` ever occurs in
 * saved Directions fixtures and whether [SmoothDriveScoring] delayRatio activates.
 */
class SmoothDriveDelaySignalAuditTest {

    data class FixtureAudit(
        val name: String,
        val routeCount: Int,
        val routesWithTrafficField: Int,
        val routesWithPositiveDelay: Int,
        val routesWithTrafficBelowBase: Int,
        val maxDelayRatio: Double,
        val delayRatioSpread: Double,
        val fullWinnerIndex: Int,
        val corridorOnlyWinnerIndex: Int,
        val delaySignalChangesWinner: Boolean,
    )

    @Test
    fun stage350AuditFixtures_havePositiveTrafficDelay() {
        val audits = auditFixturesInDir(File("../docs/stage-35-0-audit"))
        assertTrue("Expected stage-35-0-audit fixtures", audits.isNotEmpty())
        audits.forEach { audit ->
            assertTrue(
                "${audit.name}: expected at least one route with duration_in_traffic > duration",
                audit.routesWithPositiveDelay > 0,
            )
            assertTrue(
                "${audit.name}: expected delayRatio spread > 0",
                audit.delayRatioSpread > 0.0,
            )
        }
    }

    @Test
    fun stage350AuditFixtures_delaySignalCanChangeWinner() {
        val audits = auditFixturesInDir(File("../docs/stage-35-0-audit"))
        val changed = audits.count { it.delaySignalChangesWinner }
        assertTrue(
            "Expected delay signal to change SMOOTH winner in at least 2 peak fixtures; " +
                "audits=$audits",
            changed >= 2,
        )
    }

    @Test
    fun stage344LiveFixtures_trafficNeverExceedsBase() {
        val audits = auditFixturesInDir(File("../docs/stage-34-4-live"))
        assertTrue("Expected stage-34-4-live fixtures", audits.isNotEmpty())
        audits.forEach { audit ->
            assertTrue(
                "${audit.name}: stage-34-4-live has no positive delay in saved captures",
                audit.routesWithPositiveDelay == 0,
            )
        }
    }

    @Test
    fun parsedPeakFixture_delayRatioComponentsAreNonZero() {
        val fixture = File("../docs/stage-35-0-audit/difc-marina.json")
        if (!fixture.exists()) return

        val inputs = extractSmoothInputs(fixture)
        val scores = SmoothDriveScoring.scoreAll(inputs)
        assertTrue(scores.any { it.delayRatio > 0.0 })
        assertTrue(scores.any { it.delayRatioComponent > 0.0 })
        assertTrue(scores.map { it.delayRatio }.distinct().size > 1)
    }

    @Test
    fun printFullFixtureAuditReport() {
        val dirs =
            listOf(
                File("../docs/stage-35-0-audit"),
                File("../docs/stage-34-4-live"),
            )
        val report =
            buildString {
                appendLine("=== SMOOTH DELAY SIGNAL AUDIT ===")
                dirs.forEach { dir ->
                    if (!dir.isDirectory) return@forEach
                    appendLine("")
                    appendLine("DIR ${dir.name}")
                    auditFixturesInDir(dir).forEach { audit ->
                        appendLine(audit.toReportLine())
                    }
                }
            }
        System.err.println(report)
        assertTrue(report.contains("stage-35-0-audit"))
    }

    private fun auditFixturesInDir(dir: File): List<FixtureAudit> {
        if (!dir.isDirectory) return emptyList()
        return dir
            .listFiles { file -> file.extension.equals("json", ignoreCase = true) }
            ?.filter { it.name !in setOf("analysis.json", "results.json") }
            ?.sortedBy { it.name }
            ?.map { file -> auditFixture(file) }
            .orEmpty()
    }

    private fun auditFixture(file: File): FixtureAudit {
        val routes = extractRouteLegsDebugData(file.readText())
        val inputs =
            routes.mapNotNull { RouteRecommendationSelection.toSmoothDriveRouteInput(it) }
        val fullWinner = SmoothDriveScoring.pickWinnerIndex(inputs)
        val zeroDelayInputs =
            inputs.map { input ->
                input.copy(durationInTrafficSeconds = input.baseDurationSeconds)
            }
        val corridorOnlyWinner = SmoothDriveScoring.pickWinnerIndex(zeroDelayInputs)
        val scores = SmoothDriveScoring.scoreAll(inputs)
        val delayRatios = scores.map { it.delayRatio }
        var withField = 0
        var positiveDelay = 0
        var trafficBelowBase = 0
        routes.forEach { route ->
            val traffic = route.durationInTrafficSeconds
            if (traffic != null) {
                withField++
                when {
                    traffic > route.baseDurationSeconds -> positiveDelay++
                    traffic < route.baseDurationSeconds -> trafficBelowBase++
                }
            }
        }
        return FixtureAudit(
            name = file.name,
            routeCount = routes.size,
            routesWithTrafficField = withField,
            routesWithPositiveDelay = positiveDelay,
            routesWithTrafficBelowBase = trafficBelowBase,
            maxDelayRatio = delayRatios.maxOrNull() ?: 0.0,
            delayRatioSpread = (delayRatios.maxOrNull() ?: 0.0) - (delayRatios.minOrNull() ?: 0.0),
            fullWinnerIndex = fullWinner,
            corridorOnlyWinnerIndex = corridorOnlyWinner,
            delaySignalChangesWinner = fullWinner != corridorOnlyWinner,
        )
    }

    private fun extractSmoothInputs(file: File): List<SmoothDriveScoring.RouteInput> =
        extractRouteLegsDebugData(file.readText())
            .mapNotNull { RouteRecommendationSelection.toSmoothDriveRouteInput(it) }

    private fun FixtureAudit.toReportLine(): String =
        buildString {
            append(name)
            append(" routes=$routeCount")
            append(" trafficField=$routesWithTrafficField")
            append(" delay>0=$routesWithPositiveDelay")
            append(" traffic<base=$routesWithTrafficBelowBase")
            append(" maxDelayRatio=${"%.3f".format(maxDelayRatio)}")
            append(" delaySpread=${"%.3f".format(delayRatioSpread)}")
            append(" winner=$fullWinnerIndex")
            append(" corridorOnlyWinner=$corridorOnlyWinnerIndex")
            append(" delayChangesWinner=$delaySignalChangesWinner")
        }
}
