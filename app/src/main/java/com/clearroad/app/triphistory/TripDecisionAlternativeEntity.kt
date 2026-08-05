package com.clearroad.app.triphistory

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trip_decision_alternatives",
    foreignKeys = [
        ForeignKey(
            entity = TripDecisionSnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["snapshotId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("snapshotId")],
)
internal data class TripDecisionAlternativeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val snapshotId: Long,
    val routeIndex: Int,
    val routeIdentityKey: String,
    val durationSeconds: Int,
    val hasSalik: Boolean,
    val tollAed: Double?,
    val roleFlags: Int,
)

internal fun TripHandoffAlternativeUiModel.toEntity(snapshotId: Long): TripDecisionAlternativeEntity =
    TripDecisionAlternativeEntity(
        snapshotId = snapshotId,
        routeIndex = routeIndex,
        routeIdentityKey = routeIdentityKey,
        durationSeconds = durationSeconds,
        hasSalik = hasSalik,
        tollAed = tollAed,
        roleFlags = roleFlags,
    )

internal fun TripDecisionAlternativeEntity.toUiModel(): TripHandoffAlternativeUiModel =
    TripHandoffAlternativeUiModel(
        routeIndex = routeIndex,
        routeIdentityKey = routeIdentityKey,
        durationSeconds = durationSeconds,
        hasSalik = hasSalik,
        tollAed = tollAed,
        roleFlags = roleFlags,
    )
