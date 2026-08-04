package com.clearroad.app.triphistory

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TripHistoryKeyTest {

    @Test
    fun fromLatLng_isDeterministic() {
        val point = LatLng(25.2138142, 55.2820336)

        assertEquals(TripHistoryKey.fromLatLng(point), TripHistoryKey.fromLatLng(point))
    }

    @Test
    fun fromLatLng_differentEndpointsProduceDifferentKeys() {
        val origin = LatLng(25.2138142, 55.2820336)
        val destination = LatLng(25.0784811, 55.1375463)

        assertNotEquals(
            TripHistoryKey.fromLatLng(origin),
            TripHistoryKey.fromLatLng(destination),
        )
    }

    @Test
    fun pairKeys_ordersOriginAndDestination() {
        val origin = LatLng(25.2138142, 55.2820336)
        val destination = LatLng(25.0784811, 55.1375463)

        val (originKey, destinationKey) = TripHistoryKey.pairKeys(origin, destination)

        assertEquals(TripHistoryKey.fromLatLng(origin), originKey)
        assertEquals(TripHistoryKey.fromLatLng(destination), destinationKey)
        assertNotEquals(originKey, destinationKey)
    }
}
