package com.clearroad.app

import android.content.Context

/**
 * Resolves active directions fetch provider: Routes v2 vs Legacy Directions.
 *
 * Prod default follows [ArchitectureValidation.USE_ROUTES_V2_FETCH]. Debug builds (and any
 * install) may force Legacy via SharedPreferences `debug_fetch_provider=legacy` without rebuild.
 */
internal object RouteFetchProvider {

    const val DEBUG_PREF_NAME = "clearroad_debug"
    const val KEY_DEBUG_FETCH_PROVIDER = "debug_fetch_provider"

    fun useRoutesV2Fetch(context: Context): Boolean {
        if (!ArchitectureValidation.USE_ROUTES_V2_FETCH) return false
        return !isLegacyDebugOverride(context)
    }

    fun activeProviderLabel(context: Context): String =
        if (useRoutesV2Fetch(context)) "RoutesV2" else "LegacyDirections"

    private fun isLegacyDebugOverride(context: Context): Boolean {
        val override =
            context
                .getSharedPreferences(DEBUG_PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_DEBUG_FETCH_PROVIDER, null)
                ?.trim()
                ?.lowercase()
        return override in LEGACY_OVERRIDE_VALUES
    }

    private val LEGACY_OVERRIDE_VALUES =
        setOf(
            "legacy",
            "directions",
            "directions_json",
            "directions-json",
        )
}
