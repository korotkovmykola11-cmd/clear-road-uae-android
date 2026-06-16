package com.clearroad.app

import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendationTravelTimeFormatterTest {

    @Test
    fun underSixtyMinutes_useMinsSuffix() {
        assertEquals("25 mins", RecommendationTravelTimeFormatter.format(25 * 60))
        assertEquals("36 mins", RecommendationTravelTimeFormatter.format(36 * 60))
        assertEquals("59 mins", RecommendationTravelTimeFormatter.format(59 * 60))
    }

    @Test
    fun sixtyPlusMinutes_useHourMinuteFormat() {
        assertEquals("1h 03m", RecommendationTravelTimeFormatter.format(63 * 60))
        assertEquals("1h 04m", RecommendationTravelTimeFormatter.format(64 * 60))
        assertEquals("1h 18m", RecommendationTravelTimeFormatter.format(78 * 60))
        assertEquals("2h 05m", RecommendationTravelTimeFormatter.format(125 * 60))
    }

    @Test
    fun zeroOrNegativeDuration_returnsEmpty() {
        assertEquals("", RecommendationTravelTimeFormatter.format(0))
        assertEquals("", RecommendationTravelTimeFormatter.format(-60))
    }
}
