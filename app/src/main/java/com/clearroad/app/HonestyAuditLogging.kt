package com.clearroad.app

import android.util.Log
import com.clearroad.app.domain.EquivalentTripHonesty
import com.clearroad.app.domain.RouteIdentity
import com.clearroad.app.domain.RouteIdentityResolver

private const val HONESTY_AUDIT_TAG = "HonestyAudit"

internal fun logHonestyAuditRoutes(
    routes: List<RealRouteDebugData>,
    recommendedRouteIndex: Int? = null,
) {
    if (routes.isEmpty()) {
        Log.d(
            HONESTY_AUDIT_TAG,
            "HONESTY_AUDIT equivalentTrip=false routeCount=0",
        )
        Log.d(HONESTY_AUDIT_TAG, "HONESTY_REJECT reason=ROUTE_COUNT_0")
        return
    }

    val identities = RouteIdentityResolver.resolveAll(routes)
    val indicesToLog =
        if (recommendedRouteIndex != null) {
            listOf(recommendedRouteIndex.coerceIn(0, routes.lastIndex))
        } else {
            routes.indices.toList()
        }

    for (index in indicesToLog) {
        logHonestyAuditSnapshot(
            snapshot =
                EquivalentTripHonesty.audit(
                    routes = routes,
                    identities = identities,
                    recommendedIndex = index,
                ),
        )
    }
}

internal fun logHonestyAuditSnapshot(
    snapshot: EquivalentTripHonesty.HonestyAuditSnapshot,
) {
    Log.d(
        HONESTY_AUDIT_TAG,
        "HONESTY_AUDIT equivalentTrip=${snapshot.equivalentTrip} " +
            "recommendedIndex=${snapshot.recommendedIndex} " +
            "durationSpreadSeconds=${snapshot.durationSpreadSeconds} " +
            "allTollFree=${snapshot.allTollFree} " +
            "allTrafficDelayZero=${snapshot.allTrafficDelayZero} " +
            "identityCollapsed=${snapshot.identityCollapsed} " +
            "identitySuppressed=${snapshot.identitySuppressed} " +
            "routeCount=${snapshot.routeCount} " +
            "identityPrimaryNames=${snapshot.identityPrimaryNames} " +
            "identityStableKeys=${snapshot.identityStableKeys}",
    )
    if (snapshot.rejectReasons.isEmpty()) {
        Log.d(HONESTY_AUDIT_TAG, "HONESTY_PASS all conditions met")
    } else {
        snapshot.rejectReasons.forEach { reason ->
            Log.d(HONESTY_AUDIT_TAG, "HONESTY_REJECT reason=$reason")
        }
    }
}

internal fun logHonestyAuditForTrip(
    routes: List<RealRouteDebugData>,
    identities: List<RouteIdentity>,
    recommendedIndex: Int,
) {
    logHonestyAuditSnapshot(
        EquivalentTripHonesty.audit(
            routes = routes,
            identities = identities,
            recommendedIndex = recommendedIndex,
        ),
    )
}
