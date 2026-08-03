package com.clearroad.app

import java.util.Locale

/**
 * Normalizes Routes v2 navigation text for [com.clearroad.app.domain.SmoothDriveScoring].
 *
 * V2 step instructions are often Arabic-only; Legacy html_instructions use Latin "E11 /
 * Sheikh Zayed Rd". Without enrichment, [SmoothDriveScoring.classifyCorridor] sees zero
 * motorway hits and urban fragments → MIXED instead of MOTORWAY on the same geometry.
 */
internal object RoutesV2CorridorScanSupport {

    private const val ARABIC_INDIC_DIGITS = "٠١٢٣٤٥٦٧٨٩"
    private val EMIRATES_ROAD_CODE_REGEX = Regex("""(?<![\p{L}\p{N}_])[eEإØ¥]\s*(\d{2,3})(?!\d)""")

    private val ARABIC_SHEIKH_ZAYED =
        Regex("(?i)(الشيخ\\s+زايد|Ø§Ù„Ø´ÙŠØ®\\s+Ø²Ø§ÙŠØ¯|sheikh\\s+zayed|szr)")
    private val ARABIC_MBZ =
        Regex(
            "(?i)(محمد\\s+بن\\s+زايد|Ø´ÙŠØ®\\s+Ù…Ø­Ù…Ø¯\\s+Ø¨Ù†\\s+Ø²Ø§ÙŠØ¯|" +
                "mohammed\\s+bin\\s+zayed|sheikh\\s+mohammed\\s+bin\\s+zayed)",
        )
    private val EMIRATES_ROAD =
        Regex("(?i)(emirates\\s+road|emirates\\s+rd|طريق\\s+الإمارات|Ø·Ø±ÙŠÙ‚\\s+Ø§Ù„Ø¥Ù…Ø§Ø±Ø§Øª)")

    /** Canonical ASCII tokens appended for SmoothDrive keyword matching. */
    private val ROAD_CODE_CANONICAL_TOKENS =
        mapOf(
            "e11" to listOf("e11", "sheikh zayed", "szr"),
            "e311" to listOf("e311", "mohammed bin zayed", "sheikh mohammed bin zayed"),
            "e611" to listOf("e611", "emirates road", "emirates rd"),
            "e44" to listOf("e44"),
            "e66" to listOf("e66"),
            "e10" to listOf("e10"),
        )

    fun enrichCorridorScanText(
        routeSummary: String,
        stepInstructions: String,
    ): String {
        val combined = "$routeSummary $stepInstructions".trim()
        if (combined.isEmpty()) return combined

        val normalized = normalizeEmiratesRoadCodes(combined)
        val appended = linkedSetOf<String>()

        ROAD_CODE_CANONICAL_TOKENS.forEach { (code, tokens) ->
            if (containsRoadCode(normalized, code)) {
                appended.addAll(tokens)
            }
        }
        if (ARABIC_SHEIKH_ZAYED.containsMatchIn(combined)) {
            appended.addAll(listOf("sheikh zayed", "e11", "szr"))
        }
        if (ARABIC_MBZ.containsMatchIn(combined)) {
            appended.addAll(listOf("mohammed bin zayed", "e311", "sheikh mohammed bin zayed"))
        }
        if (EMIRATES_ROAD.containsMatchIn(combined)) {
            appended.addAll(listOf("emirates road", "e611"))
        }

        return buildString {
            append(normalized)
            if (appended.isNotEmpty()) {
                append(' ')
                append(appended.joinToString(" "))
            }
        }.trim().take(6000)
    }

    internal fun normalizeEmiratesRoadCodes(text: String): String {
        val withAsciiDigits = transliterateArabicIndicDigits(text)
        return withAsciiDigits.replace(EMIRATES_ROAD_CODE_REGEX) { match ->
            "e${match.groupValues[1]}"
        }
    }

    private fun containsRoadCode(
        normalizedText: String,
        code: String,
    ): Boolean {
        val lc = normalizedText.lowercase(Locale.US)
        return Regex("""(?<![\p{L}\p{N}_])${Regex.escape(code)}(?!\d)""").containsMatchIn(lc)
    }

    private fun transliterateArabicIndicDigits(text: String): String {
        if (text.isEmpty()) return text
        val builder = StringBuilder(text.length)
        text.forEach { ch ->
            val idx = ARABIC_INDIC_DIGITS.indexOf(ch)
            builder.append(if (idx >= 0) '0' + idx else ch)
        }
        return builder.toString()
    }
}
