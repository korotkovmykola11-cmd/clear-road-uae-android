package com.clearroad.app.replay

/**
 * Parses frozen Stage 38.3 CALM logcat blocks into deduplicated [FieldRouteSet]s.
 * Test-only.
 */
internal object Stage39AFieldLogParser {

    private val calmHeaderRegex =
        Regex(
            """(\d{4}-\d{2}-\d{2}) (\d{2}:\d{2}:\d{2}).*routeCount=(\d+) mode=CALM currentWinnerIndex=(\d+)""",
        )

    private val routeLineRegex =
        Regex(
            """ROUTE route_index=(\d+) .* duration_min=(\d+) duration_in_traffic_min=(\d+) .* distance_km=([\d.]+) .* critical_maneuvers_count=(\d+)""",
        )

    fun parseCalmBlocks(logcat: String): List<FieldRouteSet> {
        val blocks = mutableListOf<FieldRouteSet>()
        val lines = logcat.lines()
        var i = 0
        while (i < lines.size) {
            val header = lines[i]
            val calmHeader = calmHeaderRegex.find(header)
            if (calmHeader == null) {
                i++
                continue
            }
            val date = calmHeader.groupValues[1]
            val time = calmHeader.groupValues[2]
            val routeCount = calmHeader.groupValues[3].toInt()
            val winnerIndex = calmHeader.groupValues[4].toInt()
            val bucket = bucketForHour(time.substringBefore(':').toInt())
            val routes = mutableListOf<FieldRouteReplayRoute>()
            i++
            while (i < lines.size && routes.size < routeCount) {
                val routeMatch = routeLineRegex.find(lines[i])
                if (routeMatch != null) {
                    routes.add(
                        FieldRouteReplayRoute(
                            durationMin = routeMatch.groupValues[2].toInt(),
                            durationInTrafficMin = routeMatch.groupValues[3].toInt(),
                            distanceKm = routeMatch.groupValues[4].toDouble(),
                            criticalManeuversCount = routeMatch.groupValues[5].toInt(),
                        ),
                    )
                }
                if (lines[i].contains("SESSION currentWinnerIndex=")) break
                i++
            }
            if (routes.size == routeCount) {
                blocks.add(
                    FieldRouteSet(
                        id = "${date}T$time",
                        bucket = bucket,
                        routes = routes,
                        fieldCalmWinnerIndex = winnerIndex,
                    ),
                )
            }
            i++
        }
        return deduplicate(blocks)
    }

    internal fun deduplicate(blocks: List<FieldRouteSet>): List<FieldRouteSet> {
        val seen = linkedSetOf<String>()
        val result = mutableListOf<FieldRouteSet>()
        blocks.forEach { block ->
            val key = fingerprint(block)
            if (seen.add(key)) result.add(block)
        }
        return result
    }

    private fun fingerprint(block: FieldRouteSet): String =
        buildString {
            append(block.routes.size)
            block.routes.forEach { route ->
                append('|')
                append(route.durationMin)
                append(',')
                append(route.durationInTrafficMin)
                append(',')
                append(route.distanceKm)
                append(',')
                append(route.criticalManeuversCount)
            }
        }

    private fun bucketForHour(hour: Int): FieldBucket =
        when (hour) {
            in 5..11 -> FieldBucket.AM_RUSH
            in 17..20 -> FieldBucket.PM_RUSH
            else -> FieldBucket.PREV_PM
        }
}
