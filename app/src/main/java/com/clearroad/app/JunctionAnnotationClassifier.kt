package com.clearroad.app

internal object JunctionAnnotationClassifier {
    internal const val MAX_ANNOTATIONS = 4

    fun classify(step: DirectionsStepRecord): JunctionType? =
        classify(
            maneuver = step.maneuver,
            htmlInstructions = step.htmlInstructions,
        )

    internal fun classify(
        maneuver: String?,
        htmlInstructions: String?,
    ): JunctionType? {
        val normalizedManeuver = maneuver?.lowercase()?.replace('_', '-')?.trim().orEmpty()
        val instructionText = stripHtml(htmlInstructions).lowercase()

        return when {
            indicatesToll(instructionText, normalizedManeuver) -> JunctionType.TOLL
            indicatesTunnel(instructionText) -> JunctionType.TUNNEL
            indicatesOverpass(instructionText) -> JunctionType.OVERPASS
            isSimpleManeuver(normalizedManeuver) -> null
            indicatesRoundabout(normalizedManeuver, instructionText) -> JunctionType.ROUNDABOUT
            indicatesHighwayExit(normalizedManeuver, instructionText) -> JunctionType.HIGHWAY_EXIT
            indicatesHighwayEntry(normalizedManeuver, instructionText) -> JunctionType.HIGHWAY_ENTRY
            indicatesComplexTurn(normalizedManeuver) -> JunctionType.COMPLEX_TURN
            else -> null
        }
    }

    internal fun complexityScore(type: JunctionType): Int =
        when (type) {
            JunctionType.ROUNDABOUT -> 90
            JunctionType.TOLL -> 85
            JunctionType.TUNNEL -> 80
            JunctionType.OVERPASS -> 75
            JunctionType.HIGHWAY_EXIT -> 70
            JunctionType.HIGHWAY_ENTRY -> 65
            JunctionType.COMPLEX_TURN -> 60
        }

    internal const val MAX_LABEL_CHARS = 10

    internal fun iconGlyph(type: JunctionType): String =
        when (type) {
            JunctionType.HIGHWAY_ENTRY -> "🔀"
            JunctionType.HIGHWAY_EXIT -> "↗"
            JunctionType.ROUNDABOUT -> "⭕"
            JunctionType.COMPLEX_TURN -> "↩"
            JunctionType.TOLL -> "💰"
            JunctionType.TUNNEL -> "🚇"
            JunctionType.OVERPASS -> "🌉"
        }

    internal fun iconColor(type: JunctionType): Int =
        when (type) {
            JunctionType.HIGHWAY_EXIT -> GreyIconColor
            JunctionType.TOLL -> AmberIconColor
            JunctionType.ROUNDABOUT -> BlueIconColor
            JunctionType.HIGHWAY_ENTRY -> GreyIconColor
            JunctionType.COMPLEX_TURN -> GreyIconColor
            JunctionType.TUNNEL -> GreyIconColor
            JunctionType.OVERPASS -> GreyIconColor
        }

    internal fun usesColoredVectorIcon(type: JunctionType): Boolean =
        type == JunctionType.ROUNDABOUT ||
            type == JunctionType.HIGHWAY_EXIT ||
            type == JunctionType.HIGHWAY_ENTRY ||
            type == JunctionType.COMPLEX_TURN

    internal fun vectorIconGlyph(type: JunctionType): String =
        when (type) {
            JunctionType.HIGHWAY_EXIT -> "↗"
            JunctionType.HIGHWAY_ENTRY -> "⇄"
            JunctionType.COMPLEX_TURN -> "↩"
            else -> "○"
        }

    internal fun displayLabel(type: JunctionType): String = shortLabel(type, "")

    internal fun shortLabel(
        type: JunctionType,
        instruction: String,
    ): String =
        when (type) {
            JunctionType.HIGHWAY_ENTRY -> "Merge"
            JunctionType.HIGHWAY_EXIT -> "Exit"
            JunctionType.ROUNDABOUT -> "Roundabout"
            JunctionType.COMPLEX_TURN -> "Turn"
            JunctionType.TOLL -> "Salik"
            JunctionType.TUNNEL -> "Tunnel"
            JunctionType.OVERPASS -> "Flyover"
        }.take(MAX_LABEL_CHARS)

    private const val GreyIconColor = 0xFF757575.toInt()
    private const val AmberIconColor = 0xFFF59E0B.toInt()
    private const val BlueIconColor = 0xFF2196F3.toInt()

    private fun isSimpleManeuver(maneuver: String): Boolean =
        maneuver.isBlank() ||
            maneuver == "straight" ||
            maneuver.contains("turn-slight") ||
            maneuver.contains("slight-left") ||
            maneuver.contains("slight-right") ||
            maneuver == "continue" ||
            maneuver == "depart" ||
            maneuver == "name-change" ||
            maneuver == "new-name"

    private fun indicatesToll(
        instruction: String,
        maneuver: String,
    ): Boolean =
        instruction.contains("salik") ||
            instruction.contains("toll") ||
            maneuver.contains("toll")

    private fun indicatesTunnel(instruction: String): Boolean =
        instruction.contains("tunnel")

    private fun indicatesOverpass(instruction: String): Boolean =
        instruction.contains("flyover") ||
            instruction.contains("overpass") ||
            instruction.contains("elevated")

    private fun indicatesRoundabout(
        maneuver: String,
        instruction: String,
    ): Boolean =
        maneuver.contains("roundabout") ||
            maneuver.contains("rotary") ||
            instruction.contains("roundabout") ||
            instruction.contains("rotary")

    private fun indicatesHighwayExit(
        maneuver: String,
        instruction: String,
    ): Boolean =
        maneuver.startsWith("ramp-") ||
            maneuver.contains("exit") ||
            instruction.contains("take exit") ||
            instruction.contains("take the exit") ||
            instruction.contains("exit toward")

    private fun indicatesHighwayEntry(
        maneuver: String,
        instruction: String,
    ): Boolean =
        maneuver.contains("merge") ||
            instruction.contains("merge onto") ||
            instruction.contains("merge onto the") ||
            (maneuver.startsWith("ramp-") && instruction.contains(" onto "))

    private fun indicatesComplexTurn(maneuver: String): Boolean =
        maneuver.contains("uturn") ||
            maneuver.contains("sharp") ||
            maneuver == "turn-left" ||
            maneuver == "turn-right"

    internal fun stripHtml(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
