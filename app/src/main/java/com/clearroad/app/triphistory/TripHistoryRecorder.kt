package com.clearroad.app.triphistory

import android.content.Context
import com.clearroad.app.domain.PreferenceMode
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Records a trip when the user explicitly starts navigation (Google Maps / Waze handoff).
 *
 * Persists v1 legacy summary and optional v2 decision snapshot via [TripHandoffRepository]
 * (single dedup gate + Room transaction).
 */
object TripHistoryRecorder {

    fun recordHandoffAsync(
        scope: CoroutineScope,
        context: Context,
        origin: LatLng,
        destination: LatLng,
        durationSeconds: Int,
        mode: PreferenceMode,
        hasSalik: Boolean,
        tollAed: Double?,
        snapshot: TripHandoffSnapshotUiModel? = null,
    ) {
        scope.launch {
            val v1Entry =
                buildTripHistoryEntry(
                    origin = origin,
                    destination = destination,
                    durationSeconds = durationSeconds,
                    mode = mode,
                    hasSalik = hasSalik,
                    tollAed = tollAed,
                    timestamp = snapshot?.timestamp ?: System.currentTimeMillis(),
                )
            TripHandoffRepository.get(context).recordHandoff(
                v1Entry = v1Entry,
                snapshot = snapshot,
            )
        }
    }
}
