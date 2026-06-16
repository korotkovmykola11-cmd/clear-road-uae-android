package com.clearroad.app.domain

import java.util.Locale

/**
 * UAE road normalization for Route Identity (presentation only).
 * Does not affect route selection or scoring.
 */
internal object UaeRoadCanon {

    data class CanonEntry(
        val stableKey: String,
        val primaryName: String,
        val patterns: List<String>,
        val disambiguatorLabel: String? = null,
    )

    val entries: List<CanonEntry> =
        listOf(
            CanonEntry(
                stableKey = "E11",
                primaryName = "Sheikh Zayed Road (E11)",
                patterns =
                    listOf(
                        "e11",
                        "sheikh zayed rd",
                        "sheikh zayed road",
                        "szr",
                    ),
            ),
            CanonEntry(
                stableKey = "E311",
                primaryName = "E311 (MBZ Road)",
                patterns =
                    listOf(
                        "e311",
                        "mohammed bin zayed",
                        "sheikh mohammed bin zayed",
                        "mbz road",
                    ),
            ),
            CanonEntry(
                stableKey = "E611",
                primaryName = "Emirates Road (E611)",
                patterns =
                    listOf(
                        "e611",
                        "emirates rd",
                        "emirates road",
                    ),
            ),
            CanonEntry(
                stableKey = "E10",
                primaryName = "Abu Dhabi Highway (E10)",
                patterns =
                    listOf(
                        "e10",
                        "sheikh zayed bin sultan",
                        "al shahama",
                        "abu dhabi rd",
                    ),
            ),
            CanonEntry(
                stableKey = "E77",
                primaryName = "Expo Road (E77)",
                patterns =
                    listOf(
                        "e77",
                        "expo rd",
                        "expo road",
                    ),
            ),
            CanonEntry(
                stableKey = "E44",
                primaryName = "E44 (Ras Al Khor)",
                patterns =
                    listOf(
                        "e44",
                        "ras al khor",
                    ),
            ),
            CanonEntry(
                stableKey = "E66",
                primaryName = "Dubai–Al Ain Road (E66)",
                patterns =
                    listOf(
                        "e66",
                        "dubai–al ain",
                        "dubai-al ain",
                    ),
            ),
            CanonEntry(
                stableKey = "S120",
                primaryName = "Sharjah Link (S120)",
                patterns = listOf("s120"),
                disambiguatorLabel = "via S120",
            ),
            CanonEntry(
                stableKey = "D94",
                primaryName = "D94 Marina Link",
                patterns =
                    listOf(
                        "d94",
                        "king salman bin abdulaziz",
                    ),
                disambiguatorLabel = "via D94",
            ),
            CanonEntry(
                stableKey = "D61",
                primaryName = "D61",
                patterns = listOf("d61", "al naseem"),
                disambiguatorLabel = "via D61",
            ),
            CanonEntry(
                stableKey = "D59",
                primaryName = "D59",
                patterns = listOf("d59", "garn al sabkha"),
                disambiguatorLabel = "via D59",
            ),
        )

    private val unknownEntry =
        CanonEntry(
            stableKey = UNKNOWN_STABLE_KEY,
            primaryName = "UAE Route",
            patterns = emptyList(),
        )

    const val UNKNOWN_STABLE_KEY = "UNKNOWN"

    private val urbanDubaiEntry =
        CanonEntry(
            stableKey = "URBAN-DXB",
            primaryName = "Dubai Local Streets",
            patterns = emptyList(),
        )

    private val urbanSharjahEntry =
        CanonEntry(
            stableKey = "URBAN-SHJ",
            primaryName = "Sharjah Urban Route",
            patterns = emptyList(),
        )

    private val urbanAjmanEntry =
        CanonEntry(
            stableKey = "URBAN-AJM",
            primaryName = "Ajman local route",
            patterns = emptyList(),
        )

    private val urbanAbuDhabiEntry =
        CanonEntry(
            stableKey = "URBAN-AUH",
            primaryName = "Abu Dhabi Local Streets",
            patterns = emptyList(),
        )

    /** Disambiguator codes tried in order when siblings share a primary key. */
    val disambiguatorCodeOrder: List<String> =
        listOf("D61", "D94", "D59", "E44", "E77", "E10", "S120", "E311", "E611")

    fun normalize(text: String): String =
        text.lowercase(Locale.US).replace(Regex("\\s+"), " ")

    fun matchCanon(text: String): CanonEntry? {
        val normalized = normalize(text)
        if (normalized.isBlank()) return null
        for (entry in entries) {
            if (entry.patterns.any { normalized.contains(it) }) {
                return entry
            }
        }
        return null
    }

    fun matchHighestPriorityCanon(text: String): CanonEntry? {
        val normalized = normalize(text)
        for (entry in entries) {
            if (entry.patterns.any { normalized.contains(it) }) {
                return entry
            }
        }
        return null
    }

    fun findRoadCodes(text: String): List<String> {
        val normalized = normalize(text)
        val found = linkedSetOf<String>()
        Regex("\\b(e\\d+|d\\d+|s\\d+)\\b").findAll(normalized).forEach { match ->
            found.add(match.value.uppercase(Locale.US))
        }
        return found.filter { code -> entries.any { it.stableKey == code } }
    }

    fun entryForKey(stableKey: String): CanonEntry =
        entries.firstOrNull { it.stableKey == stableKey }
            ?: unknownEntry

    fun disambiguatorLabelForKey(stableKey: String): String? =
        entryForKey(stableKey).disambiguatorLabel
            ?: when (stableKey) {
                "E44" -> "via E44"
                "E77" -> "via E77"
                "E10" -> "via E10"
                "S120" -> "via S120"
                else -> "via $stableKey"
            }

    fun urbanFallback(
        corridorClass: SmoothDriveScoring.CorridorClass,
        scan: String,
    ): CanonEntry {
        val normalized = normalize(scan)
        return when {
            normalized.contains("ajman") && !normalized.contains("sharjah") ->
                urbanAjmanEntry
            normalized.contains("sharjah") || normalized.contains("ajman") ->
                urbanSharjahEntry
            normalized.contains("abu dhabi") ->
                urbanAbuDhabiEntry
            corridorClass == SmoothDriveScoring.CorridorClass.URBAN_WEAVE ||
                normalized.contains("dubai") ->
                urbanDubaiEntry
            else -> unknownEntry
        }
    }

    fun unknown(): CanonEntry = unknownEntry
}
