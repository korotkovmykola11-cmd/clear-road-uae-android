package com.clearroad.app

import com.clearroad.app.ui.model.GoogleMarshioDecisionState
import com.clearroad.app.ui.model.RouteMapEvidenceUiModel

/** Builds map evidence for Route Details — presentation only. */
internal object RouteMapEvidencePresentation {

    fun build(
        routes: List<RealRouteDebugData>,
        recommendedIndex: Int,
        decisionState: GoogleMarshioDecisionState?,
    ): RouteMapEvidenceUiModel? {
        if (decisionState != GoogleMarshioDecisionState.DISAGREES || routes.size < 2) {
            return null
        }

        val googlePath = routes.firstOrNull()?.routePathPoints.orEmpty()
        val marshioPath =
            routes.getOrNull(recommendedIndex.coerceIn(0, routes.lastIndex))
                ?.routePathPoints
                .orEmpty()
        if (googlePath.size < 2 || marshioPath.size < 2) {
            return null
        }

        val divergence = RoutePathDivergence.analyze(googlePath, marshioPath)
        if (!divergence.hasDivergence) {
            return RouteMapEvidenceUiModel(
                enabled = true,
                strategicCaption = "Full trip — see whether Google and MARSHIO use the same corridor.",
                localCaption = "Google and MARSHIO take different routes on this trip.",
                googleComparisonPath = googlePath,
                marshioComparisonPath = marshioPath,
            )
        }

        return RouteMapEvidenceUiModel(
            enabled = true,
            strategicCaption = "Full trip — see how different the corridors are.",
            localCaption = "After this point Google and MARSHIO take different roads.",
            splitPoint = divergence.splitPoint,
            sharedPath = divergence.sharedPath,
            googleDivergentPath = divergence.googleDivergentPath,
            marshioDivergentPath = divergence.marshioDivergentPath,
            googleComparisonPath = googlePath,
            marshioComparisonPath = marshioPath,
        )
    }
}
