package com.clearroad.app

import org.junit.Assert.assertEquals
import org.junit.Test

class DecisionLabelLayerTest {

    @Test
    fun decisionLabelIsUppercaseDecision() {
        assertEquals("DECISION", DecisionLabelLayer.label())
    }
}
