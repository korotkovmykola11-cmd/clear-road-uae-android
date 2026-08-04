package com.clearroad.app.triphistory

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.clearroad.app.domain.PreferenceMode

@Entity(
    tableName = "trip_history",
    indices = [
        Index(value = ["originKey", "destinationKey", "mode"]),
        Index(value = ["timestamp"]),
    ],
)
internal data class TripHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val originKey: String,
    val destinationKey: String,
    val durationSeconds: Int,
    val mode: String,
    val hasSalik: Boolean,
    val tollAed: Double?,
)

internal fun TripHistoryEntry.toEntity(): TripHistoryEntity =
    TripHistoryEntity(
        timestamp = timestamp,
        originKey = originKey,
        destinationKey = destinationKey,
        durationSeconds = durationSeconds,
        mode = mode.name,
        hasSalik = hasSalik,
        tollAed = tollAed,
    )

internal fun TripHistoryEntity.toEntry(): TripHistoryEntry =
    TripHistoryEntry(
        timestamp = timestamp,
        originKey = originKey,
        destinationKey = destinationKey,
        durationSeconds = durationSeconds,
        mode = PreferenceMode.valueOf(mode),
        hasSalik = hasSalik,
        tollAed = tollAed,
    )
