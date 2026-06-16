package com.clearroad.app.domain

import com.clearroad.app.RealRouteDebugData

/**
 * Presentation-only route naming derived from Google Directions data.
 * Does not affect route selection, scoring, or winner index.
 */
internal data class RouteIdentity(
    val primaryName: String,
    val fullName: String,
    val stableKey: String,
    val disambiguator: String?,
    val tollExposure: TollExposure,
)

internal enum class TollExposure {
    NO_SALIK,
    SALIK_AED,
    SALIK_ESTIMATED,
}

internal object RouteIdentityResolver {

    fun resolve(route: RealRouteDebugData): RouteIdentity =
        resolveAll(listOf(route)).first()

    fun resolveAll(routes: List<RealRouteDebugData>): List<RouteIdentity> {
        if (routes.isEmpty()) return emptyList()

        val spines =
            routes.map { route -> RouteIdentityExtractor.extractSpine(route) }.toMutableList()

        spines
            .mapIndexed { index, spine -> spine.stableKey to index }
            .groupBy({ it.first }, { it.second })
            .filter { (_, indices) -> indices.size > 1 }
            .values
            .forEach { indices ->
                RouteIdentityDisambiguator.assignDisambiguators(spines, routes, indices)
            }

        return routes.indices.map { index ->
            val spine = spines[index]
            val disambiguator = spine.disambiguator
            RouteIdentity(
                primaryName = RouteIdentityFormatter.formatPrimary(spine.primaryName),
                fullName =
                    RouteIdentityFormatter.formatFull(
                        primaryName = spine.primaryName,
                        disambiguator = disambiguator,
                    ),
                stableKey = spine.stableKey,
                disambiguator = disambiguator,
                tollExposure = RouteIdentityExtractor.tollExposure(routes[index]),
            )
        }
    }
}
