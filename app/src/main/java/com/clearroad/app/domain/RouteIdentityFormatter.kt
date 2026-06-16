package com.clearroad.app.domain

/**
 * Formats Route Identity strings for Home and Details (presentation only).
 */
internal object RouteIdentityFormatter {

    const val DIRECT_DISAMBIGUATOR = "Direct"

    fun formatPrimary(primaryName: String): String = primaryName.trim()

    fun humanizeDisambiguator(disambiguator: String?): String? {
        val trimmed = disambiguator?.trim().orEmpty()
        if (trimmed.isBlank() || trimmed.equals(DIRECT_DISAMBIGUATOR, ignoreCase = true)) {
            return null
        }
        return when (trimmed.lowercase()) {
            "shorter run" -> "shorter distance"
            "longer run" -> "slightly longer route"
            else -> trimmed
        }
    }

    fun formatFull(
        primaryName: String,
        disambiguator: String?,
    ): String {
        val primary = formatPrimary(primaryName)
        val suffix = humanizeDisambiguator(disambiguator).orEmpty()
        if (suffix.isBlank()) {
            return primary
        }
        return "$primary · $suffix"
    }
}
