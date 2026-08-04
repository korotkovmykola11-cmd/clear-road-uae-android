package com.clearroad.app.triphistory

import android.content.Context
import com.clearroad.app.domain.PreferenceMode
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Records a trip when the user explicitly starts navigation (Google Maps / Waze handoff).
 *
 * Not recorded on passive route fetches — handoff means the user intends to drive.
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
    ) {
        scope.launch {
            TripHistoryRepository.get(context).recordHandoffTrip(
                buildTripHistoryEntry(
                    origin = origin,
                    destination = destination,
                    durationSeconds = durationSeconds,
                    mode = mode,
                    hasSalik = hasSalik,
                    tollAed = tollAed,
                ),
            )
        }
    }
}
