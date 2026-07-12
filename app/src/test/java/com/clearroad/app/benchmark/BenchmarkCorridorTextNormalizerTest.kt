package com.clearroad.app.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BenchmarkCorridorTextNormalizerTest {

    @Test
    fun normalize_mapsArabicE11Code() {
        assertEquals("شارع الشيخ زايد/e11", BenchmarkCorridorTextNormalizer.normalize("شارع الشيخ زايد/إ11"))
    }

    @Test
    fun normalize_mapsArabicE44Code() {
        assertEquals("شارع الخيل/e44", BenchmarkCorridorTextNormalizer.normalize("شارع الخيل/إ44"))
    }

    @Test
    fun normalize_mapsArabicE311Code() {
        assertEquals("mbz/e311", BenchmarkCorridorTextNormalizer.normalize("mbz/إ311"))
    }

    @Test
    fun normalize_mapsSpacedArabicCode() {
        assertEquals("route/e11 end", BenchmarkCorridorTextNormalizer.normalize("route/إ 11 end"))
    }

    @Test
    fun normalize_mapsSpacedLatinCode() {
        assertEquals("route/e11 end", BenchmarkCorridorTextNormalizer.normalize("route/E 11 end"))
    }

    @Test
    fun normalize_mapsArabicIndicDigitsInCode() {
        assertEquals("route/e44", BenchmarkCorridorTextNormalizer.normalize("route/إ٤٤"))
    }

    @Test
    fun normalize_preservesOrdinaryArabicRoadNameWithoutCode() {
        val input = "شارع المستقبل"
        assertEquals(input, BenchmarkCorridorTextNormalizer.normalize(input))
    }

    @Test
    fun normalize_mapsLatinE11() {
        assertEquals("e11", BenchmarkCorridorTextNormalizer.normalize("E11"))
    }

    @Test
    fun normalize_preservesLowercaseE11() {
        assertEquals("e11", BenchmarkCorridorTextNormalizer.normalize("e11"))
    }

    @Test
    fun normalize_mapsSpacedArabicE311WithIndicDigits() {
        assertEquals("e311", BenchmarkCorridorTextNormalizer.normalize("إ ٣١١"))
    }

    @Test
    fun normalize_sheikhZayedSummary_containsE11() {
        val normalized = BenchmarkCorridorTextNormalizer.normalize("شارع الشيخ زايد/إ11")
        assertTrue(normalized.contains("e11"))
    }

    @Test
    fun normalize_doesNotExtractFromE1100() {
        assertEquals("E1100", BenchmarkCorridorTextNormalizer.normalize("E1100"))
    }

    @Test
    fun normalize_doesNotExtractFromArabicE1234() {
        assertEquals("إ1234", BenchmarkCorridorTextNormalizer.normalize("إ1234"))
    }

    @Test
    fun normalize_doesNotExtractFromE3115() {
        assertEquals("E3115", BenchmarkCorridorTextNormalizer.normalize("E3115"))
    }

    @Test
    fun normalize_doesNotExtractFromEmbeddedE11InIdentifier() {
        assertEquals("someE11value", BenchmarkCorridorTextNormalizer.normalize("someE11value"))
    }

    @Test
    fun buildCanonSafeText_doesNotCreateSheikhZayedFromEmbeddedInvalidRun() {
        val input = "sheikhE1100zayed road"
        val safe = BenchmarkCorridorTextNormalizer.buildCanonSafeText(input)
        assertEquals(
            "${BenchmarkCorridorTextNormalizer.INVALID_ROAD_CODE_BARRIER} road",
            safe,
        )
    }

    @Test
    fun buildCanonSafeText_doesNotCreateAlKhailFromEmbeddedInvalidRun() {
        val input = "alE3115khail road"
        val safe = BenchmarkCorridorTextNormalizer.buildCanonSafeText(input)
        assertEquals(
            "${BenchmarkCorridorTextNormalizer.INVALID_ROAD_CODE_BARRIER} road",
            safe,
        )
    }

    @Test
    fun buildCanonSafeText_doesNotJoinFragmentsIntoKnownRoadPhrase() {
        val input = "roadE1100sheikh zayed road"
        val safe = BenchmarkCorridorTextNormalizer.buildCanonSafeText(input)
        assertEquals(
            "${BenchmarkCorridorTextNormalizer.INVALID_ROAD_CODE_BARRIER} zayed road",
            safe,
        )
    }

    @Test
    fun buildCanonSafeText_spacedInvalidToken_insertsBarrierBetweenNeighbors() {
        val input = "sheikh E1100 zayed road"
        val safe = BenchmarkCorridorTextNormalizer.buildCanonSafeText(input)
        val barrier = BenchmarkCorridorTextNormalizer.INVALID_ROAD_CODE_BARRIER

        assertFalse(safe == "sheikh zayed road")
        assertEquals("sheikh $barrier zayed road", safe)
        assertTrue(safe.contains("sheikh $barrier zayed"))
    }

    @Test
    fun buildCanonSafeText_spacedInvalidToken_doesNotCreateAlKhailPhrase() {
        val input = "al E3115 khail road"
        val safe = BenchmarkCorridorTextNormalizer.buildCanonSafeText(input)
        val barrier = BenchmarkCorridorTextNormalizer.INVALID_ROAD_CODE_BARRIER

        assertFalse(safe == "al khail road")
        assertEquals("al $barrier khail road", safe)
    }

    @Test
    fun buildCanonSafeText_spacedInvalidToken_doesNotCreateEmiratesRoadPhrase() {
        val input = "emirates E6115 road"
        val safe = BenchmarkCorridorTextNormalizer.buildCanonSafeText(input)
        val barrier = BenchmarkCorridorTextNormalizer.INVALID_ROAD_CODE_BARRIER

        assertFalse(safe == "emirates road")
        assertEquals("emirates $barrier road", safe)
    }

    @Test
    fun buildCanonSafeText_mixedInvalidAndValidToken_reappendsValidCode() {
        val safe = BenchmarkCorridorTextNormalizer.buildCanonSafeText("E1100/E11")
        assertEquals(
            "${BenchmarkCorridorTextNormalizer.INVALID_ROAD_CODE_BARRIER} e11",
            safe,
        )
    }

    @Test
    fun buildCanonSafeText_mixedArabicInvalidAndValidToken_reappendsValidCode() {
        val safe = BenchmarkCorridorTextNormalizer.buildCanonSafeText("إ1234/إ11")
        assertEquals(
            "${BenchmarkCorridorTextNormalizer.INVALID_ROAD_CODE_BARRIER} e11",
            safe,
        )
    }
}

class BenchmarkCorridorArabicClassificationTest {

    @Test
    fun classify_arabicE11Summary_recognizesE11() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "شارع الشيخ زايد/إ11",
                corridorScanText = "",
            )

        assertEquals("E11", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("E11"))
        assertEquals("شارع الشيخ زايد/e11", result.evidenceSummary)
    }

    @Test
    fun classify_arabicE44Summary_recognizesE44() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "شارع الخيل/إ44",
                corridorScanText = "",
            )

        assertEquals("E44", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("E44"))
    }

    @Test
    fun classify_arabicE311Summary_recognizesE311() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "طريق الشيخ محمد بن زايد/إ311",
                corridorScanText = "",
            )

        assertEquals("E311", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("E311"))
    }

    @Test
    fun classify_spacedLatinE11Summary_recognizesE11() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "Sheikh Zayed Rd / E 11",
                corridorScanText = "",
            )

        assertEquals("E11", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("E11"))
    }

    /**
     * Offline re-evaluation of Stage 0B re-pilot summaries for live-difc-marina.
     * Geometry verdict is unchanged; only corridor labels improve.
     */
    @Test
    fun rePilotLiveDifcMarina_googleSummaries_reclassifyAfterArabicNormalization() {
        val google0 =
            CorridorClassifier.classify(
                routeSummary = "شارع الشيخ زايد/إ11",
                corridorScanText = "",
                distanceMeters = 24_301,
            )
        val google1 =
            CorridorClassifier.classify(
                routeSummary = "شارع المستقبل وشارع الشيخ زايد/إ11",
                corridorScanText = "",
                distanceMeters = 24_704,
            )
        val google2 =
            CorridorClassifier.classify(
                routeSummary = "شارع الخيل/إ44",
                corridorScanText = "",
                distanceMeters = 29_013,
            )

        assertEquals("E11", google0.primaryStableKey)
        assertEquals("E11", google1.primaryStableKey)
        assertEquals("E44", google2.primaryStableKey)
        assertFalse(google0.primaryStableKey == "UNKNOWN")
        assertFalse(google1.primaryStableKey == "UNKNOWN")
        assertFalse(google2.primaryStableKey == "UNKNOWN")
    }

    @Test
    fun classify_e1100_doesNotRecognizePartialRoadCode() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "E1100",
                corridorScanText = "",
            )

        assertFalse(result.primaryStableKey in listOf("E11", "E123", "E311"))
        assertFalse(result.allMatchedStableKeys.any { it in listOf("E11", "E123", "E311") })
    }

    @Test
    fun classify_arabicE1234_doesNotRecognizePartialRoadCode() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "إ1234",
                corridorScanText = "",
            )

        assertFalse(result.primaryStableKey in listOf("E11", "E123", "E311"))
        assertFalse(result.allMatchedStableKeys.any { it in listOf("E11", "E123", "E311") })
    }

    @Test
    fun classify_e3115_doesNotRecognizePartialRoadCode() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "E3115",
                corridorScanText = "",
            )

        assertFalse(result.primaryStableKey in listOf("E11", "E123", "E311"))
        assertFalse(result.allMatchedStableKeys.any { it in listOf("E11", "E123", "E311") })
    }

    @Test
    fun classify_embeddedE11InIdentifier_doesNotRecognizeRoadCode() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "someE11value",
                corridorScanText = "",
            )

        assertFalse(result.primaryStableKey in listOf("E11", "E123", "E311"))
        assertFalse(result.allMatchedStableKeys.any { it in listOf("E11", "E123", "E311") })
    }

    @Test
    fun classify_underscoreDelimitedE11_doesNotRecognizeRoadCode() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "abc_E11_value",
                corridorScanText = "",
            )

        assertFalse(result.primaryStableKey in listOf("E11", "E123", "E311"))
        assertFalse(result.allMatchedStableKeys.any { it in listOf("E11", "E123", "E311") })
    }

    @Test
    fun classify_latinE11_recognizesE11() {
        val result = CorridorClassifier.classify(routeSummary = "E11", corridorScanText = "")

        assertEquals("E11", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("E11"))
    }

    @Test
    fun classify_lowercaseE11_recognizesE11() {
        val result = CorridorClassifier.classify(routeSummary = "e11", corridorScanText = "")

        assertEquals("E11", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("E11"))
    }

    @Test
    fun classify_spacedLatinE11_recognizesE11() {
        val result = CorridorClassifier.classify(routeSummary = "E 11", corridorScanText = "")

        assertEquals("E11", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("E11"))
    }

    @Test
    fun classify_arabicE11Code_recognizesE11() {
        val result = CorridorClassifier.classify(routeSummary = "إ11", corridorScanText = "")

        assertEquals("E11", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("E11"))
    }

    @Test
    fun classify_spacedArabicE311WithIndicDigits_recognizesE311() {
        val result = CorridorClassifier.classify(routeSummary = "إ ٣١١", corridorScanText = "")

        assertEquals("E311", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("E311"))
    }

    @Test
    fun classify_sheikhZayedRoadSlashE11_recognizesE11() {
        val result =
            CorridorClassifier.classify(
                routeSummary = "Sheikh Zayed Road/E11",
                corridorScanText = "",
            )

        assertEquals("E11", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("E11"))
    }

    @Test
    fun classify_e11SlashE44_recognizesBothCodes() {
        val result = CorridorClassifier.classify(routeSummary = "E11/E44", corridorScanText = "")

        assertEquals("E11", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.containsAll(listOf("E11", "E44")))
    }

    @Test
    fun classify_mixedInvalidAndValidToken_recognizesValidCodeOnly() {
        val result = CorridorClassifier.classify(routeSummary = "E1100/E11", corridorScanText = "")

        assertEquals("E11", result.primaryStableKey)
        assertEquals(listOf("E11"), result.allMatchedStableKeys)
    }

    @Test
    fun classify_mixedArabicInvalidAndValidToken_recognizesValidCodeOnly() {
        val result = CorridorClassifier.classify(routeSummary = "إ1234/إ11", corridorScanText = "")

        assertEquals("E11", result.primaryStableKey)
        assertEquals(listOf("E11"), result.allMatchedStableKeys)
    }

    @Test
    fun classify_arabicE44Code_recognizesE44() {
        val result = CorridorClassifier.classify(routeSummary = "إ٤٤", corridorScanText = "")

        assertEquals("E44", result.primaryStableKey)
        assertTrue(result.allMatchedStableKeys.contains("E44"))
    }

    @Test
    fun classify_spacedInvalidToken_doesNotCreateSheikhZayedRoad() {
        val input = "sheikh E1100 zayed road"
        val result = CorridorClassifier.classify(routeSummary = input, corridorScanText = "")

        assertFalse(result.primaryStableKey == "E11")
        assertFalse(result.allMatchedStableKeys.contains("E11"))
        assertEquals(BenchmarkCorridorTextNormalizer.normalize(input), result.evidenceSummary)
    }

    @Test
    fun classify_spacedInvalidToken_doesNotCreateAlKhailRoad() {
        val input = "al E3115 khail road"
        val result = CorridorClassifier.classify(routeSummary = input, corridorScanText = "")

        assertFalse(result.primaryStableKey in listOf("E44", "E311"))
        assertFalse(result.allMatchedStableKeys.any { it in listOf("E44", "E311") })
        assertEquals(BenchmarkCorridorTextNormalizer.normalize(input), result.evidenceSummary)
    }

    @Test
    fun classify_spacedInvalidToken_doesNotCreateEmiratesRoad() {
        val input = "emirates E6115 road"
        val result = CorridorClassifier.classify(routeSummary = input, corridorScanText = "")

        assertFalse(result.primaryStableKey == "E611")
        assertFalse(result.allMatchedStableKeys.contains("E611"))
        assertEquals(BenchmarkCorridorTextNormalizer.normalize(input), result.evidenceSummary)
    }

    @Test
    fun classify_embeddedInvalidRunInToken_doesNotCreateSheikhZayedRoad() {
        val input = "sheikhE1100zayed road"
        val result = CorridorClassifier.classify(routeSummary = input, corridorScanText = "")

        assertFalse(result.primaryStableKey == "E11")
        assertFalse(result.allMatchedStableKeys.contains("E11"))
        assertEquals(BenchmarkCorridorTextNormalizer.normalize(input), result.evidenceSummary)
        assertFalse(result.evidenceSummary.contains("sheikh zayed"))
    }

    @Test
    fun classify_embeddedInvalidRunInToken_doesNotCreateAlKhailRoad() {
        val input = "alE3115khail road"
        val result = CorridorClassifier.classify(routeSummary = input, corridorScanText = "")

        assertFalse(result.primaryStableKey in listOf("E44", "E311"))
        assertFalse(result.allMatchedStableKeys.any { it in listOf("E44", "E311") })
        assertEquals(BenchmarkCorridorTextNormalizer.normalize(input), result.evidenceSummary)
    }

    @Test
    fun classify_splitInvalidToken_doesNotCreateArtificialSheikhZayedPhrase() {
        val input = "roadE1100sheikh zayed road"
        val result = CorridorClassifier.classify(routeSummary = input, corridorScanText = "")

        assertFalse(result.primaryStableKey == "E11")
        assertFalse(result.allMatchedStableKeys.contains("E11"))
        assertEquals(BenchmarkCorridorTextNormalizer.normalize(input), result.evidenceSummary)
    }

    @Test
    fun classify_digitPrefixedE11_doesNotRecognizeRoadCode() {
        val result = CorridorClassifier.classify(routeSummary = "2E11", corridorScanText = "")

        assertFalse(result.primaryStableKey in listOf("E11", "E123", "E311"))
        assertFalse(result.allMatchedStableKeys.any { it in listOf("E11", "E123", "E311") })
    }

    @Test
    fun classify_letterPrefixedE11_doesNotRecognizeRoadCode() {
        val result = CorridorClassifier.classify(routeSummary = "AE11", corridorScanText = "")

        assertFalse(result.primaryStableKey in listOf("E11", "E123", "E311"))
        assertFalse(result.allMatchedStableKeys.any { it in listOf("E11", "E123", "E311") })
    }

    @Test
    fun classify_underscorePrefixedE11_doesNotRecognizeRoadCode() {
        val result = CorridorClassifier.classify(routeSummary = "_E11", corridorScanText = "")

        assertFalse(result.primaryStableKey in listOf("E11", "E123", "E311"))
        assertFalse(result.allMatchedStableKeys.any { it in listOf("E11", "E123", "E311") })
    }
}
