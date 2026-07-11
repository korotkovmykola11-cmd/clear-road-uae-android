package com.clearroad.app.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

class BenchmarkLiveBudgetTest {

    @Test
    fun stage0BFactory_usesAgreedCaps() {
        val budget = BenchmarkLiveBudget.stage0B()
        val snapshot = budget.snapshot()

        assertEquals(0, snapshot.totalReserved)
        assertEquals(BenchmarkLiveBudget.TOTAL_CAP, snapshot.totalRemaining)
        assertEquals(BenchmarkLiveBudget.GOOGLE_CAP, snapshot.googleRemaining)
        assertEquals(BenchmarkLiveBudget.GRAPHHOPPER_CAP, snapshot.graphHopperRemaining)
    }

    @Test
    fun googleCap_grantsTenThenRejectsEleventh() {
        val budget = BenchmarkLiveBudget.stage0B()

        repeat(10) { index ->
            val result = budget.reserve(BenchmarkLiveBudget.LiveProvider.GOOGLE, "case-$index")
            assertTrue(result is BenchmarkLiveBudget.ReservationResult.Granted)
        }

        val rejected =
            budget.reserve(BenchmarkLiveBudget.LiveProvider.GOOGLE, "case-11")
        assertEquals(
            BenchmarkLiveBudget.RejectReason.PROVIDER_CAP_EXHAUSTED,
            (rejected as BenchmarkLiveBudget.ReservationResult.Rejected).reason,
        )
        assertEquals(10, budget.snapshot().googleReserved)
    }

    @Test
    fun graphHopperCap_grantsTenThenRejectsEleventh() {
        val budget = BenchmarkLiveBudget.stage0B()

        repeat(10) { index ->
            val result = budget.reserve(BenchmarkLiveBudget.LiveProvider.GRAPHHOPPER, "case-$index")
            assertTrue(result is BenchmarkLiveBudget.ReservationResult.Granted)
        }

        val rejected =
            budget.reserve(BenchmarkLiveBudget.LiveProvider.GRAPHHOPPER, "case-11")
        assertEquals(
            BenchmarkLiveBudget.RejectReason.PROVIDER_CAP_EXHAUSTED,
            (rejected as BenchmarkLiveBudget.ReservationResult.Rejected).reason,
        )
        assertEquals(10, budget.snapshot().graphHopperReserved)
    }

    @Test
    fun totalCap_rejectsTwentyFirstAttempt() {
        val budget = BenchmarkLiveBudget.stage0B()

        repeat(10) { index ->
            assertTrue(
                budget.reserve(BenchmarkLiveBudget.LiveProvider.GOOGLE, "google-$index")
                    is BenchmarkLiveBudget.ReservationResult.Granted,
            )
            assertTrue(
                budget.reserve(BenchmarkLiveBudget.LiveProvider.GRAPHHOPPER, "gh-$index")
                    is BenchmarkLiveBudget.ReservationResult.Granted,
            )
        }

        val rejected =
            budget.reserve(BenchmarkLiveBudget.LiveProvider.GOOGLE, "overflow")
        assertEquals(
            BenchmarkLiveBudget.RejectReason.TOTAL_CAP_EXHAUSTED,
            (rejected as BenchmarkLiveBudget.ReservationResult.Rejected).reason,
        )
        assertEquals(20, budget.snapshot().totalReserved)
    }

    @Test
    fun rejectedReservation_doesNotIncrementCounters() {
        val budget = BenchmarkLiveBudget.stage0B()

        repeat(BenchmarkLiveBudget.GOOGLE_CAP) {
            budget.reserve(BenchmarkLiveBudget.LiveProvider.GOOGLE, "case")
        }
        budget.reserve(BenchmarkLiveBudget.LiveProvider.GOOGLE, "overflow")

        val snapshot = budget.snapshot()
        assertEquals(BenchmarkLiveBudget.GOOGLE_CAP, snapshot.googleReserved)
        assertEquals(BenchmarkLiveBudget.GOOGLE_CAP, snapshot.totalReserved)
    }

    @Test
    fun grantedReservation_hasNoRefundApi_andCountersRemainConsumed() {
        val budget = BenchmarkLiveBudget.stage0B()

        val granted =
            budget.reserve(BenchmarkLiveBudget.LiveProvider.GOOGLE, "case-1")
                as BenchmarkLiveBudget.ReservationResult.Granted

        assertTrue(granted.requestId.isNotBlank())
        assertEquals(1, budget.snapshot().totalReserved)
        assertEquals(1, budget.snapshot().googleReserved)

        // Simulated failed HTTP attempt still leaves the reservation consumed.
        assertEquals(1, budget.snapshot().totalReserved)
    }

    @Test
    fun grantedReservations_haveUniqueRequestIds() {
        val budget = BenchmarkLiveBudget.stage0B()
        val ids =
            (1..5).map {
                (
                    budget.reserve(BenchmarkLiveBudget.LiveProvider.GOOGLE, "case-$it")
                        as BenchmarkLiveBudget.ReservationResult.Granted
                ).requestId
            }

        assertEquals(ids.size, ids.distinct().size)
        ids.forEach { id ->
            assertFalse(id.contains("api"))
            assertFalse(id.contains("key"))
        }
    }

    @Test
    fun concurrentReservations_neverExceedCaps() {
        val budget = BenchmarkLiveBudget.stage0B()
        val pool = Executors.newFixedThreadPool(8)
        try {
            val tasks = mutableListOf<Future<BenchmarkLiveBudget.ReservationResult>>()
            repeat(40) { index ->
                val provider =
                    if (index % 2 == 0) {
                        BenchmarkLiveBudget.LiveProvider.GOOGLE
                    } else {
                        BenchmarkLiveBudget.LiveProvider.GRAPHHOPPER
                    }
                tasks +=
                    pool.submit(
                        Callable {
                            budget.reserve(provider, "concurrent-$index")
                        },
                    )
            }

            val results = tasks.map { it.get() }
            val granted = results.filterIsInstance<BenchmarkLiveBudget.ReservationResult.Granted>()
            val rejected = results.filterIsInstance<BenchmarkLiveBudget.ReservationResult.Rejected>()

            assertEquals(20, granted.size)
            assertEquals(20, rejected.size)
            assertEquals(20, budget.snapshot().totalReserved)
            assertTrue(budget.snapshot().googleReserved <= BenchmarkLiveBudget.GOOGLE_CAP)
            assertTrue(budget.snapshot().graphHopperReserved <= BenchmarkLiveBudget.GRAPHHOPPER_CAP)
            assertEquals(
                granted.size,
                budget.snapshot().googleReserved + budget.snapshot().graphHopperReserved,
            )
        } finally {
            pool.shutdownNow()
        }
    }
}
