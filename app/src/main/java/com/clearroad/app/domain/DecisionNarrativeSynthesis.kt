package com.clearroad.app.domain

/**
 * Stage 31.5 decision narrative — presentation-only prose from existing route metrics.
 * Does not alter scoring, selection, or recommendation calculations.
 */
internal object DecisionNarrativeSynthesis {

    fun narrative(
        mode: PreferenceMode,
        recommendedTollAed: Int,
        highConfidence: Boolean,
    ): String {
        val trafficQualifier =
            if (highConfidence) "stable traffic" else "predictable traffic"
        return when (mode) {
            PreferenceMode.FASTEST -> {
                val salikPhrase =
                    if (recommendedTollAed == 0) "no Salik charges" else "low Salik impact"
                "Fastest route with $trafficQualifier and $salikPhrase."
            }
            PreferenceMode.NO_TOLLS ->
                if (recommendedTollAed == 0) {
                    "Avoids Salik charges with $trafficQualifier."
                } else {
                    "Best Salik balance with $trafficQualifier."
                }
            PreferenceMode.CALM ->
                "Smoother drive with $trafficQualifier."
        }
    }

    fun confidenceDisplayLabel(highConfidence: Boolean): String =
        if (highConfidence) "Confidence 92%" else "Confidence 78%"
}
