package com.clearroad.app

import android.util.Log
import com.clearroad.app.domain.ComparativeEvidence
import com.clearroad.app.domain.RouteIdentity

private const val COMPARATIVE_EVIDENCE_AUDIT_TAG = "ComparativeEvidenceAudit"

internal fun logComparativeEvidenceDetailsAudit(
    routes: List<RealRouteDebugData>,
    identities: List<RouteIdentity>,
    recommendedIndex: Int,
    isEquivalentTrip: Boolean,
    isRecommendedRouteDetails: Boolean,
    detailRouteIndex: Int,
) {
    if (routes.isEmpty()) return

    val recIdx = recommendedIndex.coerceIn(0, routes.lastIndex)
    val result =
        ComparativeEvidence.buildRejectedAlternatives(
            routes = routes,
            identities = identities,
            recommendedIndex = recIdx,
            isEquivalentTrip = isEquivalentTrip,
        )

    Log.d(
        COMPARATIVE_EVIDENCE_AUDIT_TAG,
        "DETAILS_REJECTED_ALTS visible=${result.isVisible} " +
            "lineCount=${result.lines.size} equivalentTrip=$isEquivalentTrip " +
            "isRecommendedDetails=$isRecommendedRouteDetails detailIdx=$detailRouteIndex " +
            "recommendedIdx=$recIdx routeCount=${routes.size}",
    )

    if (!isRecommendedRouteDetails) {
        Log.d(
            COMPARATIVE_EVIDENCE_AUDIT_TAG,
            "DETAILS_REJECTED_ALTS_SKIP reason=NOT_RECOMMENDED_ROUTE_DETAILS",
        )
        return
    }

    if (isEquivalentTrip) {
        Log.d(
            COMPARATIVE_EVIDENCE_AUDIT_TAG,
            "DETAILS_REJECTED_ALTS_SKIP reason=EQUIVALENT_TRIP",
        )
        return
    }

    routes.indices
        .filter { it != recIdx }
        .forEach { altIdx ->
            val alternative = routes[altIdx]
            val selected = routes[recIdx]
            val altIdentity = identities.getOrNull(altIdx)
            val selectedIdentity = identities.getOrNull(recIdx)
            val timeDelta = alternative.durationSeconds - selected.durationSeconds
            val hasTradeoff =
                ComparativeEvidence.hasDetailsTradeoff(
                    selected = selected,
                    alternative = alternative,
                    selectedIdentity = selectedIdentity,
                    alternativeIdentity = altIdentity,
                )
            val matchedLine =
                result.lines.firstOrNull { line ->
                    line.label ==
                        ComparativeEvidence.alternativeLabelForAudit(
                            alternative,
                            altIdentity,
                            altIdx,
                        )
                }
            Log.d(
                COMPARATIVE_EVIDENCE_AUDIT_TAG,
                "ALT[$altIdx] label=${ComparativeEvidence.alternativeLabelForAudit(alternative, altIdentity, altIdx)} " +
                    "timeDeltaSec=$timeDelta salikDiffers=" +
                    "${ComparativeEvidence.salikDiffers(selected, alternative)} " +
                    "hasTradeoff=$hasTradeoff included=${matchedLine != null} " +
                    "detail=${matchedLine?.detail ?: "—"}",
            )
        }

    result.formattedLines.forEachIndexed { index, line ->
        Log.d(COMPARATIVE_EVIDENCE_AUDIT_TAG, "LINE[$index] $line")
    }
}
