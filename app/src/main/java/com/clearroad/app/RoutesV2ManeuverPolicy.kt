package com.clearroad.app

/**
 * Policy B maneuver counting for Routes v2 step records (Stage C.1 Path B).
 *
 * Hybrid rules map V2 enums + instruction text to Legacy-equivalent critical maneuvers
 * without asymmetric inflation on alternate routes (difc-marina false override class).
 */
internal object RoutesV2ManeuverPolicy {

    /** Short-loop UTURN proxy — tightened vs research prototype (250m). */
    const val SHORT_LOOP_METERS = 120

    /** Highway keep-lane bias minimum step length — raised to avoid urban approach steps. */
    private const val HIGHWAY_LANE_BIAS_MIN_METERS = 900

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

    private val KEEP_LANE_INSTRUCTION =
        Regex(
            "(?i)(keep\\s+(left|right)|continue to (stay|follow)|stay on|continue on|" +
                "تابع.{0,80}(اليسار|اليمين)|استمر.{0,80}(الطريق|في)|للاستمرار|" +
                "ØªØ§Ø¨Ø¹.{0,80}(Ø§Ù„ÙŠØ³Ø§Ø±|Ø§Ù„ÙŠÙ…ÙŠÙ†)|Ø§Ø³ØªÙ…Ø±|Ù„Ù„Ø§Ø³ØªÙ…Ø±Ø§Ø±)",
        )

    private val HIGHWAY_CORRIDOR_MARKERS =
        Regex("(?i)(E11|E311|E44|E66|E10|D71|Sheikh|Ittihad|برسوم|Ø¥11|Ø¥311|Ø´Ø§Ø±Ø¹)")

    private val SIMPLE_TURN_INSTRUCTION =
        Regex("(?i)(^Turn left\\.?$|^Turn right\\.?$|الاتجاه يسار|الاتجاه يمين)")

    private val ROUNDABOUT_INSTRUCTION =
        Regex("(?i)(roundabout|rotary|دوار|ميدان)")

    fun countCritical(steps: List<DirectionsStepRecord>): Int =
        steps.count { isCritical(it) }

    /** Policy B stress input — enum + explicit roundabout only (no keep-lane / short-loop proxies). */
    fun countPolicyBStress(steps: List<DirectionsStepRecord>): Int =
        steps.count { isPolicyBStress(it) }

    fun isCritical(step: DirectionsStepRecord): Boolean {
        if (isPolicyBStress(step)) return true
        val maneuver = step.maneuver ?: return false
        val instructions = step.htmlInstructions.orEmpty()
        if (maneuver == "TURN_LEFT" || maneuver == "TURN_RIGHT") {
            if (KEEP_LANE_INSTRUCTION.containsMatchIn(instructions) &&
                step.distanceMeters >= 500 &&
                HIGHWAY_CORRIDOR_MARKERS.containsMatchIn(instructions)
            ) {
                return true
            }
            if (isHighwayLaneBias(step)) return true
        }
        if (maneuver == "UTURN_LEFT" || maneuver == "UTURN_RIGHT") {
            if (step.distanceMeters in 1..SHORT_LOOP_METERS &&
                !HIGHWAY_CORRIDOR_MARKERS.containsMatchIn(instructions)
            ) {
                return true
            }
        }
        return false
    }

    fun isPolicyBStress(step: DirectionsStepRecord): Boolean {
        val maneuver = step.maneuver ?: return false
        if (maneuver in CRITICAL_ENUMS) return true
        val instructions = step.htmlInstructions.orEmpty()
        if (maneuver == "UTURN_LEFT" || maneuver == "UTURN_RIGHT") {
            if (ROUNDABOUT_INSTRUCTION.containsMatchIn(instructions)) return true
        }
        return false
    }

    private fun isHighwayLaneBias(step: DirectionsStepRecord): Boolean {
        val maneuver = step.maneuver ?: return false
        if (maneuver != "TURN_LEFT" && maneuver != "TURN_RIGHT") return false
        val instructions = step.htmlInstructions.orEmpty()
        if (SIMPLE_TURN_INSTRUCTION.containsMatchIn(instructions.trim())) return false
        return step.distanceMeters >= HIGHWAY_LANE_BIAS_MIN_METERS &&
            HIGHWAY_CORRIDOR_MARKERS.containsMatchIn(instructions)
    }
}
