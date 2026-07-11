package com.clearroad.app.benchmark

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BenchmarkLiveProviderTest {

    @Test
    fun fakeProvider_returnsSuccess() {
        val provider = FakeGoogleProvider()
        val case = sampleCase()
        val permit = grantPermit()

        val result = provider.fetch(case, permit)

        assertTrue(result is ProviderFetchResult.Success)
        val success = result as ProviderFetchResult.Success
        assertEquals(1, success.candidates.size)
        assertEquals(200, success.httpStatus)
    }

    @Test
    fun success_preservesCaseId() {
        val provider = FakeGoogleProvider()
        val case = sampleCase(caseId = "difc-marina")
        val permit = grantPermit()

        val result = provider.fetch(case, permit) as ProviderFetchResult.Success

        assertEquals("difc-marina", result.caseId)
    }

    @Test
    fun success_preservesRequestId() {
        val provider = FakeGoogleProvider()
        val case = sampleCase()
        val permit = grantPermit(caseId = "preserve-request")

        val result = provider.fetch(case, permit) as ProviderFetchResult.Success

        assertEquals(permit.requestId, result.requestId)
    }

    @Test
    fun success_preservesProviderId() {
        val google = FakeGoogleProvider()
        val graphHopper = FakeGraphHopperProvider()

        val googleResult = google.fetch(sampleCase(), grantPermit()) as ProviderFetchResult.Success
        val graphHopperResult =
            graphHopper.fetch(sampleCase(), grantPermit(provider = BenchmarkLiveBudget.LiveProvider.GRAPHHOPPER))
                as ProviderFetchResult.Success

        assertEquals(BenchmarkProviderId.GOOGLE, googleResult.providerId)
        assertEquals(BenchmarkProviderId.GRAPHHOPPER, graphHopperResult.providerId)
    }

    @Test
    fun success_preservesCandidates() {
        val candidate = sampleCandidate()
        val provider = FakeGoogleProvider(candidates = listOf(candidate))
        val result = provider.fetch(sampleCase(), grantPermit()) as ProviderFetchResult.Success

        assertEquals(listOf(candidate), result.candidates)
    }

    @Test
    fun emptyCandidates_convertedToInvalidResponseFailure() {
        val result =
            ProviderFetchResult.buildSuccess(
                providerId = BenchmarkProviderId.GOOGLE,
                caseId = "case-1",
                requestId = "google-case-1-1",
                candidates = emptyList(),
                httpStatus = 200,
                latencyMs = 120L,
            )

        assertTrue(result is ProviderFetchResult.Failed)
        val failed = result as ProviderFetchResult.Failed
        assertEquals(FailureCategory.INVALID_RESPONSE, failed.errorCategory)
        assertEquals(200, failed.httpStatus)
    }

    @Test(expected = IllegalArgumentException::class)
    fun success_rejectsEmptyCandidatesDirectly() {
        ProviderFetchResult.Success(
            providerId = BenchmarkProviderId.GOOGLE,
            caseId = "case-1",
            requestId = "google-case-1-1",
            candidates = emptyList(),
            httpStatus = 200,
            latencyMs = 120L,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun success_rejectsNegativeLatency() {
        ProviderFetchResult.Success(
            providerId = BenchmarkProviderId.GOOGLE,
            caseId = "case-1",
            requestId = "google-case-1-1",
            candidates = listOf(sampleCandidate()),
            httpStatus = 200,
            latencyMs = -1L,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun success_rejectsInvalidHttpStatus() {
        ProviderFetchResult.Success(
            providerId = BenchmarkProviderId.GOOGLE,
            caseId = "case-1",
            requestId = "google-case-1-1",
            candidates = listOf(sampleCandidate()),
            httpStatus = 99,
            latencyMs = 120L,
        )
    }

    @Test
    fun skipped_allowsMissingRequestIdForPreNetworkReasons() {
        val skipped =
            ProviderFetchResult.Skipped(
                providerId = BenchmarkProviderId.GOOGLE,
                caseId = "case-1",
                requestId = null,
                reason = SkipReason.MISSING_KEY,
                message = "API key not configured",
            )

        assertEquals(null, skipped.requestId)
        assertEquals(SkipReason.MISSING_KEY, skipped.reason)
    }

    @Test
    fun skipped_allowsMissingRequestIdForAllPreNetworkReasons() {
        SkipReason.entries.forEach { reason ->
            val skipped =
                ProviderFetchResult.Skipped(
                    providerId = BenchmarkProviderId.GOOGLE,
                    caseId = "case-1",
                    requestId = null,
                    reason = reason,
                    message = "skipped before network for $reason",
                )
            assertEquals(null, skipped.requestId)
            assertEquals(reason, skipped.reason)
        }
    }

    @Test
    fun failed_requiresRequestId() {
        val failed =
            ProviderFetchResult.Failed(
                providerId = BenchmarkProviderId.GRAPHHOPPER,
                caseId = "case-1",
                requestId = "graphhopper-case-1-1",
                errorCategory = FailureCategory.HTTP,
                httpStatus = 500,
                latencyMs = 80L,
                message = "Upstream HTTP error",
            )

        assertEquals("graphhopper-case-1-1", failed.requestId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun failed_rejectsBlankRequestId() {
        ProviderFetchResult.Failed(
            providerId = BenchmarkProviderId.GOOGLE,
            caseId = "case-1",
            requestId = "   ",
            errorCategory = FailureCategory.NETWORK,
            message = "network down",
        )
    }

    @Test
    fun resultToString_doesNotLeakSecrets() {
        val secret = "super-secret-api-key"
        val failed =
            ProviderFetchResult.Failed(
                providerId = BenchmarkProviderId.GOOGLE,
                caseId = "case-1",
                requestId = "google-case-1-1",
                errorCategory = FailureCategory.HTTP,
                httpStatus = 401,
                latencyMs = 50L,
                message = "Unauthorized api_key=$secret",
            )

        val text = failed.toString()
        assertFalse(text.contains(secret))
        assertTrue(text.contains("[REDACTED]"))
    }

    @Test
    fun fakeGoogleProvider_implementsContract() {
        val provider = FakeGoogleProvider()
        assertEquals(BenchmarkProviderId.GOOGLE, provider.providerId)
        assertEquals("Google Maps", provider.displayName)
        assertTrue(provider.fetch(sampleCase(), grantPermit()) is ProviderFetchResult.Success)
    }

    @Test
    fun fakeGraphHopperProvider_implementsContract() {
        val provider = FakeGraphHopperProvider()
        assertEquals(BenchmarkProviderId.GRAPHHOPPER, provider.providerId)
        assertEquals("GraphHopper", provider.displayName)
        assertTrue(
            provider.fetch(
                sampleCase(),
                grantPermit(provider = BenchmarkLiveBudget.LiveProvider.GRAPHHOPPER),
            ) is ProviderFetchResult.Success,
        )
    }

    @Test
    fun provider_doesNotReserveBudget() {
        val budget = BenchmarkLiveBudget.stage0B()
        val permit =
            budget.reserve(BenchmarkLiveBudget.LiveProvider.GOOGLE, "budget-case")
                as BenchmarkLiveBudget.ReservationResult.Granted
        val snapshotAfterReserve = budget.snapshot()

        FakeGoogleProvider().fetch(sampleCase(), permit)

        assertEquals(snapshotAfterReserve, budget.snapshot())
    }

    private fun grantPermit(
        provider: BenchmarkLiveBudget.LiveProvider = BenchmarkLiveBudget.LiveProvider.GOOGLE,
        caseId: String = "case-1",
    ): BenchmarkLiveBudget.ReservationResult.Granted {
        val budget = BenchmarkLiveBudget.stage0B()
        val granted = budget.reserve(provider, caseId)
        assertTrue(granted is BenchmarkLiveBudget.ReservationResult.Granted)
        return granted as BenchmarkLiveBudget.ReservationResult.Granted
    }

    private fun sampleCase(caseId: String = "case-1"): UaeRouteBenchmarkCase =
        UaeRouteBenchmarkCase(
            caseId = caseId,
            originLabel = "DIFC",
            destinationLabel = "Marina",
            originLat = 25.2100,
            originLng = 55.2750,
            destinationLat = 25.0800,
            destinationLng = 55.1400,
            expectedMajorCorridors = listOf("E11"),
            tags = listOf("urban"),
            notes = "fixture case",
        )

    private fun sampleCandidate(caseId: String = "case-1"): BenchmarkRouteCandidate =
        BenchmarkRouteCandidate(
            candidateId = "$caseId-google-0",
            caseId = caseId,
            provider = "google",
            routeIndex = 0,
            routeSummary = "Sheikh Zayed Rd/E11",
            corridorScanText = "Merge onto Sheikh Zayed Rd / E11",
            routePathPoints =
                listOf(
                    LatLng(25.2100, 55.2750),
                    LatLng(25.0800, 55.1400),
                ),
            distanceMeters = 18_000,
            durationSeconds = 1_200,
            tollAed = 4,
            sourceFixture = "live",
        )

    private class FakeGoogleProvider(
        private val candidates: List<BenchmarkRouteCandidate> = listOf(
            BenchmarkRouteCandidate(
                candidateId = "case-1-google-0",
                caseId = "case-1",
                provider = "google",
                routeIndex = 0,
                routeSummary = "Sheikh Zayed Rd/E11",
                corridorScanText = "Merge onto Sheikh Zayed Rd / E11",
                routePathPoints =
                    listOf(
                        LatLng(25.2100, 55.2750),
                        LatLng(25.0800, 55.1400),
                    ),
                distanceMeters = 18_000,
                durationSeconds = 1_200,
                tollAed = 4,
                sourceFixture = "live",
            ),
        ),
    ) : BenchmarkLiveProvider {
        override val providerId: BenchmarkProviderId = BenchmarkProviderId.GOOGLE
        override val displayName: String = "Google Maps"

        override fun fetch(
            case: UaeRouteBenchmarkCase,
            permit: BenchmarkLiveBudget.ReservationResult.Granted,
        ): ProviderFetchResult =
            ProviderFetchResult.buildSuccess(
                providerId = providerId,
                caseId = case.caseId,
                requestId = permit.requestId,
                candidates = candidates.map { it.copy(caseId = case.caseId) },
                httpStatus = 200,
                latencyMs = 150L,
            )
    }

    private class FakeGraphHopperProvider : BenchmarkLiveProvider {
        override val providerId: BenchmarkProviderId = BenchmarkProviderId.GRAPHHOPPER
        override val displayName: String = "GraphHopper"

        override fun fetch(
            case: UaeRouteBenchmarkCase,
            permit: BenchmarkLiveBudget.ReservationResult.Granted,
        ): ProviderFetchResult =
            ProviderFetchResult.buildSuccess(
                providerId = providerId,
                caseId = case.caseId,
                requestId = permit.requestId,
                candidates =
                    listOf(
                        BenchmarkRouteCandidate(
                            candidateId = "${case.caseId}-graphhopper-0",
                            caseId = case.caseId,
                            provider = "graphhopper",
                            routeIndex = 0,
                            routeSummary = "Sheikh Zayed Rd/E11",
                            corridorScanText = "E11 corridor",
                            routePathPoints =
                                listOf(
                                    LatLng(case.originLat, case.originLng),
                                    LatLng(case.destinationLat, case.destinationLng),
                                ),
                            distanceMeters = 17_500,
                            durationSeconds = 1_150,
                            tollAed = 0,
                            sourceFixture = "live",
                        ),
                    ),
                httpStatus = 200,
                latencyMs = 180L,
            )
    }
}
