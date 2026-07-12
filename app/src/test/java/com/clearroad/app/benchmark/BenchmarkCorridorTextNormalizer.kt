package com.clearroad.app.benchmark

/**
 * Stage 0B — benchmark-only corridor text normalization.
 *
 * Maps Arabic UAE road code spellings to Latin forms understood by [UaeRoadCanon]
 * without modifying production normalization.
 */
object BenchmarkCorridorTextNormalizer {

    /**
     * Benchmark-only adjacency blocker inserted in place of unsafe tokens.
     * Survives production [com.clearroad.app.domain.UaeRoadCanon.normalize] (lowercase + whitespace collapse only)
     * and breaks substring matches such as `sheikh` + `zayed` → `sheikh zayed`.
     */
    internal const val INVALID_ROAD_CODE_BARRIER = "zzinvalidroadcodebarrierzz"

    private const val ARABIC_INDIC_DIGITS = "٠١٢٣٤٥٦٧٨٩"
    private val EMIRATES_ROAD_CODE_REGEX = Regex("""(?<![\p{L}\p{N}_])[eEإ]\s*(\d{2,3})(?!\d)""")
    private val EMIRATES_ROAD_CODE_STRIP_REGEX = Regex("""[eEإ]\s*\d+""")

    fun normalize(text: String): String {
        if (text.isBlank()) {
            return text
        }
        val withAsciiDigits = transliterateArabicIndicDigits(text)
        return withAsciiDigits.replace(EMIRATES_ROAD_CODE_REGEX, "e$1")
    }

    /** Boundary-valid UAE E-road codes (e.g. `e11`, `e311`) for benchmark canon matching. */
    fun extractBoundaryValidRoadCodes(text: String): List<String> {
        if (text.isBlank()) {
            return emptyList()
        }
        return EMIRATES_ROAD_CODE_REGEX
            .findAll(normalize(text))
            .map { match -> "e${match.groupValues[1]}" }
            .distinct()
            .toList()
    }

    /**
     * Text safe for production [UaeRoadCanon] substring matching in benchmark only.
     * Replaces whitespace tokens with invalid embedded E-road runs with [INVALID_ROAD_CODE_BARRIER],
     * strips remaining code fragments from safe tokens, then re-appends only boundary-valid codes.
     */
    fun buildCanonSafeText(text: String): String {
        if (text.isBlank()) {
            return text
        }
        val normalized = normalize(text)
        val validatedCodes = extractBoundaryValidRoadCodes(text)
        val safeTokens =
            normalized
                .split(Regex("""\s+"""))
                .filter { it.isNotEmpty() }
                .map { token ->
                    if (tokenHasInvalidEmbeddedRun(token)) {
                        INVALID_ROAD_CODE_BARRIER
                    } else {
                        EMIRATES_ROAD_CODE_STRIP_REGEX.replace(token, "").trimEnd('/', ' ')
                    }
                }
                .filter { it.isNotEmpty() }
        return buildString {
            if (safeTokens.isNotEmpty()) {
                append(safeTokens.joinToString(" "))
            }
            if (validatedCodes.isNotEmpty()) {
                if (isNotEmpty()) {
                    append(' ')
                }
                append(validatedCodes.joinToString(" "))
            }
        }
    }

    private fun tokenHasInvalidEmbeddedRun(token: String): Boolean {
        if (!EMIRATES_ROAD_CODE_STRIP_REGEX.containsMatchIn(token)) {
            return false
        }
        val validRanges = EMIRATES_ROAD_CODE_REGEX.findAll(token).map { it.range }.toList()
        return EMIRATES_ROAD_CODE_STRIP_REGEX.findAll(token).any { run ->
            validRanges.none { validRange ->
                run.range.first >= validRange.first && run.range.last <= validRange.last
            }
        }
    }

    private fun transliterateArabicIndicDigits(text: String): String =
        buildString(text.length) {
            text.forEach { character ->
                val digitIndex = ARABIC_INDIC_DIGITS.indexOf(character)
                append(
                    if (digitIndex >= 0) {
                        '0' + digitIndex
                    } else {
                        character
                    },
                )
            }
        }
}
