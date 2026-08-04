package com.clearroad.app.triphistory

import com.clearroad.app.domain.PreferenceMode

/** Compact local trip record — not a full route payload. */
data class TripHistoryEntry(
    val timestamp: Long,
    /** Grid-snapped origin; same O-D pair bucket, not a physical corridor id. */
    val originKey: String,
    /** Grid-snapped destination; same O-D pair bucket, not a physical corridor id. */
    val destinationKey: String,
    val durationSeconds: Int,
    val mode: PreferenceMode,
    val hasSalik: Boolean,
    val tollAed: Double?,
)
// Follow-up: optional `corridorKey: String?` if comparing same physical corridor (E11 vs E311).
