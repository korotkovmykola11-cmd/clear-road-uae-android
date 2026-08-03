package com.clearroad.app.benchmark

import com.clearroad.app.buildDirectionsUrl
import com.clearroad.app.RoutesV2ResponseAdapter
import com.clearroad.app.domain.PreferenceMode
import com.clearroad.app.domain.SmoothDriveScoring
import com.clearroad.app.effectiveTollAedForScoring
import com.clearroad.app.extractRouteLegsDebugData
import com.clearroad.app.fetchDirectionsRaw
import com.clearroad.app.RouteRecommendationSelection
import com.google.android.gms.maps.model.LatLng
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage B — scoring parity between Legacy Directions adapter and Routes v2 adapter.
 *
 * Live dual-fetch tests require `PLACES_API_KEY` in project `local.properties`.
 * Prod fetch is not switched; this validates adapter equivalence for recommendation inputs.
 */
class RoutesV2ScoringParityTest {

    @Test
    fun offline_v2Fixtures_produceValidRecommendationInputs() {
        listOf(
            "live-difc-marina-traffic.json",
            "live-sharjah-downtown-traffic.json",
            "live-business-bay-jlt-traffic.json",
        ).forEach { fixture ->
            val routes =
                RoutesV2ResponseAdapter.extractRouteLegsDebugData(
                    RoutesV2ResponseAdapterTestFixtures.read(fixture),
                )
            assertTrue("$fixture: no routes parsed", routes.isNotEmpty())
            routes.forEach { route ->
                assertTrue(route.distanceMeters > 0)
                assertTrue(route.durationSeconds > 0)
                assertTrue(route.criticalManeuversCount != null)
            }
            PreferenceMode.entries.forEach { mode ->
                assertTrue(
                    "$fixture $mode: invalid winner",
                    RouteRecommendationSelection.pickRecommendedRouteIndex(routes, mode) in routes.indices,
                )
            }
        }
    }

    @Test
    fun live_dualFetch_scoringParityAcrossUaeCases() {
        val apiKey = loadPlacesApiKey()
        if (apiKey == null) {
            println("SKIP live parity: PLACES_API_KEY not found in local.properties")
            return
        }

        val rows = PARITY_CASES.mapNotNull { case -> runParityCase(case, apiKey) }
        printParityTable(rows)

        assertTrue("Expected at least 5 live parity rows", rows.size >= 5)
        val comparable = rows.filter { it.comparable }
        val matched = comparable.count { it.allModesMatch }
        println(
            "PARITY SUMMARY comparable=${comparable.size} allModesMatch=$matched " +
                "mismatches=${comparable.size - matched}",
        )
    }

    private fun runParityCase(
        case: ParityCase,
        apiKey: String,
    ): ParityRow? {
        val origin = LatLng(case.originLat, case.originLng)
        val destination = LatLng(case.destinationLat, case.destinationLng)

        val legacyUrl = buildDirectionsUrl(origin, destination)
        val legacyRaw = fetchDirectionsRawBlocking(legacyUrl) ?: return null
        val legacyRoutes = extractRouteLegsDebugData(legacyRaw)

        val v2Raw =
            fetchRoutesV2Raw(
                apiKey = apiKey,
                originLat = case.originLat,
                originLng = case.originLng,
                destinationLat = case.destinationLat,
                destinationLng = case.destinationLng,
            ) ?: return null
        val v2Routes = RoutesV2ResponseAdapter.extractRouteLegsDebugData(v2Raw)

        if (legacyRoutes.isEmpty() || v2Routes.isEmpty()) return null

        val legacyWinners = modeWinners(legacyRoutes)
        val v2Winners = modeWinners(v2Routes)
        val comparable = legacyRoutes.size == v2Routes.size
        val modeResults =
            PreferenceMode.entries.associateWith { mode ->
                val legacyIdx = legacyWinners.getValue(mode)
                val v2Idx = v2Winners.getValue(mode)
                ModeParity(
                    legacyIndex = legacyIdx,
                    v2Index = v2Idx,
                    match = legacyIdx == v2Idx,
                    reason =
                        when {
                            !comparable ->
                                "route count differs (Legacy=${legacyRoutes.size}, V2=${v2Routes.size})"
                            legacyIdx != v2Idx ->
                                buildMismatchReason(mode, legacyRoutes, v2Routes, legacyIdx, v2Idx)
                            else -> "match"
                        },
                )
            }

        return ParityRow(
            caseId = case.caseId,
            legacyRouteCount = legacyRoutes.size,
            v2RouteCount = v2Routes.size,
            comparable = comparable,
            modes = modeResults,
            legacyFactors = summarizeFactors(legacyRoutes, legacyWinners),
            v2Factors = summarizeFactors(v2Routes, v2Winners),
        )
    }

    private fun buildMismatchReason(
        mode: PreferenceMode,
        legacyRoutes: List<com.clearroad.app.RealRouteDebugData>,
        v2Routes: List<com.clearroad.app.RealRouteDebugData>,
        legacyIdx: Int,
        v2Idx: Int,
    ): String {
        val legacy = legacyRoutes[legacyIdx]
        val v2 = v2Routes[v2Idx]
        val parts = mutableListOf<String>()
        if (legacy.durationSeconds != v2.durationSeconds) {
            parts += "durationSec L=${legacy.durationSeconds} V2=${v2.durationSeconds}"
        }
        if (legacy.distanceMeters != v2.distanceMeters) {
            parts += "distanceM L=${legacy.distanceMeters} V2=${v2.distanceMeters}"
        }
        if (legacy.tollAED != v2.tollAED) {
            parts += "tollAED L=${legacy.tollAED} V2=${v2.tollAED}"
        }
        if (legacy.criticalManeuversCount != v2.criticalManeuversCount) {
            parts += "critical L=${legacy.criticalManeuversCount} V2=${v2.criticalManeuversCount}"
        }
        val legacyCorridor = SmoothDriveScoring.classifyCorridor(legacy.corridorScanText)
        val v2Corridor = SmoothDriveScoring.classifyCorridor(v2.corridorScanText)
        if (legacyCorridor != v2Corridor) {
            parts += "corridor L=$legacyCorridor V2=$v2Corridor"
        }
        if (mode == PreferenceMode.CALM &&
            legacy.criticalManeuversCount != null &&
            v2.criticalManeuversCount != null
        ) {
            parts += "calmStressInputs available on both"
        }
        return parts.joinToString("; ").ifBlank { "winner index differs at fetch time ($mode)" }
    }

    private fun modeWinners(
        routes: List<com.clearroad.app.RealRouteDebugData>,
    ): Map<PreferenceMode, Int> =
        PreferenceMode.entries.associateWith { mode ->
            RouteRecommendationSelection.pickRecommendedRouteIndex(routes, mode)
        }

    private fun summarizeFactors(
        routes: List<com.clearroad.app.RealRouteDebugData>,
        winners: Map<PreferenceMode, Int>,
    ): Map<PreferenceMode, String> =
        winners.mapValues { (_, idx) ->
            val route = routes[idx]
            val toll = effectiveTollAedForScoring(route, routes)
            "idx=$idx dur=${route.durationSeconds}s dist=${route.distanceMeters}m " +
                "toll=$toll crit=${route.criticalManeuversCount} " +
                "intervals=${route.trafficSpeedIntervals.size}"
        }

    private fun fetchDirectionsRawBlocking(url: String): String? {
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

    private fun fetchRoutesV2Raw(
        apiKey: String,
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double,
    ): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn =
                (URL("https://routes.googleapis.com/directions/v2:computeRoutes").openConnection()
                    as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 25_000
                    readTimeout = 25_000
                    RoutesV2ResponseAdapter.buildComputeRoutesHeaders(apiKey).forEach { (k, v) ->
                        setRequestProperty(k, v)
                    }
                }
            val body =
                RoutesV2ResponseAdapter.buildComputeRoutesRequestBody(
                    originLat = originLat,
                    originLng = originLng,
                    destinationLat = destinationLat,
                    destinationLng = destinationLng,
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
        val candidates =
            listOf(
                File("local.properties"),
                File("../local.properties"),
            )
        for (file in candidates) {
            if (!file.exists()) continue
            val key =
                file.readLines()
                    .firstOrNull { it.startsWith("PLACES_API_KEY=") }
                    ?.substringAfter("=")
                    ?.trim()
            if (!key.isNullOrBlank()) return key
        }
        return null
    }

    private fun printParityTable(rows: List<ParityRow>) {
        println("\n=== ROUTES V2 SCORING PARITY (Stage B) ===")
        rows.forEach { row ->
            println(
                "${row.caseId}: routes L=${row.legacyRouteCount} V2=${row.v2RouteCount} " +
                    "comparable=${row.comparable}",
            )
            row.modes.forEach { (mode, result) ->
                println(
                    "  $mode: Legacy=${result.legacyIndex} V2=${result.v2Index} " +
                        "match=${result.match} reason=${result.reason}",
                )
            }
            println("  Legacy factors: ${row.legacyFactors}")
            println("  V2 factors:     ${row.v2Factors}")
        }
    }

    private data class ParityCase(
        val caseId: String,
        val originLat: Double,
        val originLng: Double,
        val destinationLat: Double,
        val destinationLng: Double,
    )

    private data class ModeParity(
        val legacyIndex: Int,
        val v2Index: Int,
        val match: Boolean,
        val reason: String,
    )

    private data class ParityRow(
        val caseId: String,
        val legacyRouteCount: Int,
        val v2RouteCount: Int,
        val comparable: Boolean,
        val modes: Map<PreferenceMode, ModeParity>,
        val legacyFactors: Map<PreferenceMode, String>,
        val v2Factors: Map<PreferenceMode, String>,
    ) {
        val allModesMatch: Boolean =
            comparable && modes.values.all { it.match }
    }

    companion object {
        private val PARITY_CASES =
            listOf(
                ParityCase("live-difc-marina", 25.2138142, 55.2820336, 25.0784811, 55.1375463),
                ParityCase("live-sharjah-downtown", 25.338715, 55.420114, 25.1976146, 55.2743122),
                ParityCase("live-business-bay-jlt", 25.1850, 55.2608, 25.0694, 55.1413),
                ParityCase("live-marina-airport-t3", 25.0784811, 55.1375463, 25.2584301, 55.3698345),
                ParityCase("live-ajman-difc", 25.4051615, 55.5135881, 25.2138142, 55.2820336),
                ParityCase("live-jvc-abu-dhabi", 25.0600077, 55.2099569, 24.4873245, 54.6066894),
                ParityCase("live-downtown-abu-dhabi", 25.1976146, 55.2743122, 24.4873245, 54.6066894),
            )
    }
}
