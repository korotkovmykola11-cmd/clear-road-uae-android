package com.clearroad.app

import android.util.Log
import com.clearroad.app.domain.RouteIdentityPresentationPolicy
import com.clearroad.app.domain.RouteIdentityResolver
import com.clearroad.app.domain.PreferenceMode

private const val SAVE_AED_AUDIT_TAG = "SaveAedAudit"

internal fun logSaveAedAuditRoutes(routes: List<RealRouteDebugData>) {
    if (routes.isEmpty()) {
        Log.d(SAVE_AED_AUDIT_TAG, "SAVE_AED_AUDIT routeCount=0")
        return
    }

    val identities = RouteIdentityResolver.resolveAll(routes)
    val winnerIndex =
        RouteRecommendationSelection.pickRecommendedRouteIndex(
            routes,
            PreferenceMode.NO_TOLLS,
        )
    val allTollFree = RouteIdentityPresentationPolicy.allTollZero(routes)

    Log.d(
        SAVE_AED_AUDIT_TAG,
        "SAVE_AED_AUDIT routeCount=${routes.size} winner=$winnerIndex allTollFree=$allTollFree",
    )

    routes.forEachIndexed { index, route ->
        val identity = identities.getOrNull(index)
        Log.d(
            SAVE_AED_AUDIT_TAG,
            "SAVE_AED_AUDIT route[$index] durationSeconds=${route.durationSeconds} " +
                "distanceMeters=${route.distanceMeters} tollAED=${route.tollAED} " +
                "tollCount=${RouteIdentityPresentationPolicy.presentationTollCount(route)} " +
                "identity=${identity?.primaryName.orEmpty()} stableKey=${identity?.stableKey.orEmpty()}",
        )
    }
}
