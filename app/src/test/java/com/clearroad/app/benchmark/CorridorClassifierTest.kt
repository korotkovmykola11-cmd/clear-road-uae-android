package com.clearroad.app.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CorridorClassifierTest {

    @Test
    fun classifiesE11Only() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "Sheikh Zayed Rd/E11",
                corridorScanText = "Merge onto Sheikh Zayed Rd / E11 Toll road",
            )

        assertEquals("E11", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("E11"))
        assertEquals(listOf("E11"), result.allMatchedStableKeys)
        assertFalse(result.secondaryConnectors.contains("E11"))
    }

    @Test
    fun classifiesE11WithD59Connector() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "Sheikh Zayed Rd/E11 and Al Marsa St",
                corridorScanText =
                    "Merge onto Sheikh Zayed Rd / E11 Take exit 29 for Garn Al Sabkha St / D59 " +
                        "Merge onto Al Jamayel St / D59 Continue onto Al Marsa St",
                distanceMeters = 28_000,
            )

        assertEquals("E11", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("E11"))
        assertTrue(result.allMatchedStableKeys.contains("D59"))
        assertTrue(result.matchedRoadEvidence.contains("D59"))
        assertTrue(result.matchedRoadEvidence.contains("Al Marsa"))
        assertTrue(result.secondaryConnectors.contains("D59"))
        assertTrue(result.secondaryConnectors.contains("Al Marsa"))
    }

    @Test
    fun classifiesE11WithAlMarsaNamedConnector() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "Sheikh Zayed Rd/E11",
                corridorScanText = "Take the Al Marsa St ramp to Jumeira Continue onto Al Marsa St",
            )

        assertEquals("E11", result.primaryStableKey)
        assertTrue(result.matchedRoadEvidence.contains("Al Marsa"))
        assertTrue(result.secondaryConnectors.contains("Al Marsa"))
    }

    @Test
    fun classifiesE311FromSummary() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "Sheikh Mohammed Bin Zayed Rd/E311",
                corridorScanText = "Take the ramp onto Sheikh Mohammed Bin Zayed Rd / E311",
            )

        assertEquals("E311", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("E311"))
    }

    @Test
    fun classifiesE611FromScan() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "Emirates Rd",
                corridorScanText = "Continue on Emirates Rd / E611",
            )

        assertEquals("E611", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("E611"))
    }

    @Test
    fun classifiesE44FromSummary() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "Ras Al Khor Rd/E44",
                corridorScanText = "Merge onto Ras Al Khor Rd / E44",
            )

        assertEquals("E44", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("E44"))
    }

    @Test
    fun classifiesE10FromSummary() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "Al Shahama - Abu Dhabi Rd/Sheikh Zayed Bin Sultan St/E10",
                corridorScanText = "Continue on Al Shahama - Abu Dhabi Rd / E10",
            )

        assertEquals("E10", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("E10"))
    }

    @Test
    fun classifiesE66FromSummary() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "Dubai–Al Ain Rd/E66",
                corridorScanText = "Merge onto Dubai-Al Ain Rd / E66",
            )

        assertEquals("E66", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("E66"))
    }

    @Test
    fun classifiesE77FromSummary() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "Expo Rd/E77",
                corridorScanText = "Continue on Expo Rd / E77",
            )

        assertEquals("E77", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("E77"))
    }

    @Test
    fun classifiesS120FromScan() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "University City route",
                corridorScanText = "Merge onto University City Rd / S120",
            )

        assertEquals("S120", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("S120"))
    }

    @Test
    fun unknownRoute_fallsBackWithoutCrash() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "Local neighborhood streets",
                corridorScanText = "Turn left onto Side Street",
            )

        assertTrue(result.primaryStableKey.isNotBlank())
        assertTrue(result.corridorLabel.isNotBlank())
        assertTrue(result.allMatchedStableKeys.isEmpty())
    }

    @Test
    fun allMatchedStableKeys_containsEveryRecognizedCodeInText() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "Sheikh Mohammed Bin Zayed Rd/E311 and Ras Al Khor Rd/E44",
                corridorScanText =
                    "Merge onto Sheikh Mohammed Bin Zayed Rd / E311 " +
                        "Keep left on Ras Al Khor Rd / E44 via D59 Garn Al Sabkha St / D59",
            )

        assertEquals("E311", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.containsAll(listOf("E311", "E44", "D59")))
        assertTrue(result.matchedRoadEvidence.containsAll(listOf("E311", "E44", "D59")))
    }
}
