package com.clearroad.app.benchmark

import java.util.concurrent.atomic.AtomicInteger

/**
 * Stage 0B — authoritative live request attempt budget.
 *
 * Reservations are consumed before any future HTTP call and are never refunded.
 */
class BenchmarkLiveBudget private constructor() {

    enum class LiveProvider {
        GOOGLE,
        GRAPHHOPPER,
    }

    enum class RejectReason {
        TOTAL_CAP_EXHAUSTED,
        PROVIDER_CAP_EXHAUSTED,
    }

    sealed class ReservationResult {
        data class Granted(val requestId: String) : ReservationResult()
        data class Rejected(val reason: RejectReason) : ReservationResult()
    }

    data class BudgetSnapshot(
        val totalReserved: Int,
        val googleReserved: Int,
        val graphHopperReserved: Int,
        val totalRemaining: Int,
        val googleRemaining: Int,
        val graphHopperRemaining: Int,
    )

    private val totalReserved = AtomicInteger(0)
    private val googleReserved = AtomicInteger(0)
    private val graphHopperReserved = AtomicInteger(0)
    private val requestSequence = AtomicInteger(0)

    fun reserve(provider: LiveProvider, caseId: String): ReservationResult =
        synchronized(this) {
            if (totalReserved.get() >= TOTAL_CAP) {
                return ReservationResult.Rejected(RejectReason.TOTAL_CAP_EXHAUSTED)
            }

            val providerReserved =
                when (provider) {
                    LiveProvider.GOOGLE -> googleReserved
                    LiveProvider.GRAPHHOPPER -> graphHopperReserved
                }
            val providerCap =
                when (provider) {
                    LiveProvider.GOOGLE -> GOOGLE_CAP
                    LiveProvider.GRAPHHOPPER -> GRAPHHOPPER_CAP
                }

            if (providerReserved.get() >= providerCap) {
                return ReservationResult.Rejected(RejectReason.PROVIDER_CAP_EXHAUSTED)
            }

            providerReserved.incrementAndGet()
            totalReserved.incrementAndGet()

            val requestId =
                buildRequestId(
                    provider = provider,
                    caseId = sanitizeCaseId(caseId),
                    sequence = requestSequence.incrementAndGet(),
                )
            ReservationResult.Granted(requestId)
        }

    fun snapshot(): BudgetSnapshot =
        synchronized(this) {
            val total = totalReserved.get()
            val google = googleReserved.get()
            val graphHopper = graphHopperReserved.get()
            BudgetSnapshot(
                totalReserved = total,
                googleReserved = google,
                graphHopperReserved = graphHopper,
                totalRemaining = (TOTAL_CAP - total).coerceAtLeast(0),
                googleRemaining = (GOOGLE_CAP - google).coerceAtLeast(0),
                graphHopperRemaining = (GRAPHHOPPER_CAP - graphHopper).coerceAtLeast(0),
            )
        }

    companion object {
        const val TOTAL_CAP = 20
        const val GOOGLE_CAP = 10
        const val GRAPHHOPPER_CAP = 10

        fun stage0B(): BenchmarkLiveBudget = BenchmarkLiveBudget()
    }

    private fun buildRequestId(
        provider: LiveProvider,
        caseId: String,
        sequence: Int,
    ): String =
        when (provider) {
            LiveProvider.GOOGLE -> "google-$caseId-$sequence"
            LiveProvider.GRAPHHOPPER -> "graphhopper-$caseId-$sequence"
        }

    private fun sanitizeCaseId(caseId: String): String {
        val sanitized =
            caseId
                .lowercase()
                .replace(Regex("[^a-z0-9._-]"), "-")
                .trim('-', '.', '_')
        return sanitized.ifBlank { "case" }
    }
}
