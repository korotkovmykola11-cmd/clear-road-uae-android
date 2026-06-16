package com.clearroad.app.domain

import com.clearroad.app.domain.SmoothDriveScoring.CorridorClass
import org.junit.Assert.assertEquals
import org.junit.Test

class UaeRoadCanonTest {

    @Test
    fun urbanFallback_prefersSharjahOverAbuDhabiWhenBothMentioned() {
        val entry =
            UaeRoadCanon.urbanFallback(
                corridorClass = CorridorClass.MIXED,
                scan = "Head toward Sharjah then continue toward Abu Dhabi local streets",
            )
        assertEquals("Sharjah Urban Route", entry.primaryName)
    }

    @Test
    fun urbanFallback_ajmanLocalWhenOnlyAjman() {
        val entry =
            UaeRoadCanon.urbanFallback(
                corridorClass = CorridorClass.URBAN_WEAVE,
                scan = "Continue through Ajman local streets",
            )
        assertEquals("Ajman local route", entry.primaryName)
    }
}
