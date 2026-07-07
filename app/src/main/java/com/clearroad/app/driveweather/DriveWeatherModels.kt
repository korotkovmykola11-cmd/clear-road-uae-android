package com.clearroad.app.ui.driveweather

/** Screen + hero temperature — felt before reading copy. */
enum class DriveMood {
    CALM,
    BUSY,
    HEAVY,
}

/**
 * Drive Signature — MARSHIO visual fingerprint of a trip.
 * Not a map line; an atmospheric state glyph.
 */
enum class DriveSignature {
    SMOOTH,
    BUSY,
    CONGESTED,
    STOP_START,
}

/** Finer-grained traffic story when mood alone is not enough. */
enum class TripTrafficCharacter {
    FLOWING,
    DETOUR,
    CONGESTED,
    STOP_START,
}

/** Human confidence language — no gauges, no percentages. */
enum class DecisionConfidence {
    STRONG,
    STRONG_CHOICE,
    WORTH_CONSIDERING,
    CLOSE_CALL,
}

val DecisionConfidence.displayLabel: String
    get() =
        when (this) {
            DecisionConfidence.STRONG -> "🟢 Strong"
            DecisionConfidence.STRONG_CHOICE -> "🟢 Strong choice"
            DecisionConfidence.WORTH_CONSIDERING -> "🟡 Worth considering"
            DecisionConfidence.CLOSE_CALL -> "🔴 Close call"
        }

fun DriveMood.toDefaultDriveSignature(): DriveSignature =
    when (this) {
        DriveMood.CALM -> DriveSignature.SMOOTH
        DriveMood.BUSY -> DriveSignature.BUSY
        DriveMood.HEAVY -> DriveSignature.CONGESTED
    }

data class DriveWeatherHero(
    val mood: DriveMood,
    val moodEmoji: String,
    val moodCaption: String,
    val adviceLine1: String,
    val adviceLine2: String,
    val etaDeltaValue: String?,
    val etaDeltaUnit: String?,
    val savingsValue: String?,
    val savingsUnit: String?,
    val decisionConfidence: DecisionConfidence,
    val routeStartLabel: String = "Start",
    val routeEndLabel: String = "Destination",
    val trafficCharacter: TripTrafficCharacter? = null,
)

data class DriveWeatherChip(
    val emoji: String?,
    val text: String,
)

data class DriveWeatherUiModel(
    val hero: DriveWeatherHero,
    val liveUpdateLabel: String,
    val chips: List<DriveWeatherChip>,
    val storySteps: List<String>,
    val animationKey: String,
)
