package com.clearroad.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteIdentityFormatterTest {

    @Test
    fun humanizeDisambiguator_rewritesRunLabels() {
        assertEquals("shorter distance", RouteIdentityFormatter.humanizeDisambiguator("shorter run"))
        assertEquals(
            "slightly longer route",
            RouteIdentityFormatter.humanizeDisambiguator("longer run"),
        )
    }

    @Test
    fun formatFull_usesHumanizedDisambiguator() {
        assertEquals(
            "Ajman local route · shorter distance",
            RouteIdentityFormatter.formatFull("Ajman local route", "shorter run"),
        )
    }

    @Test
    fun humanizeDisambiguator_directReturnsNull() {
        assertNull(RouteIdentityFormatter.humanizeDisambiguator(RouteIdentityFormatter.DIRECT_DISAMBIGUATOR))
    }
}
