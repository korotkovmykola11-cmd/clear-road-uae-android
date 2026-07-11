package com.clearroad.app.benchmark

/**
 * Stage 0B — provider-neutral live benchmark contract.
 *
 * Providers receive an already-granted budget permit and must never call [BenchmarkLiveBudget.reserve].
 */
interface BenchmarkLiveProvider {
    val providerId: BenchmarkProviderId
    val displayName: String

    fun fetch(
        case: UaeRouteBenchmarkCase,
        permit: BenchmarkLiveBudget.ReservationResult.Granted,
    ): ProviderFetchResult
}

enum class BenchmarkProviderId {
    GOOGLE,
    GRAPHHOPPER,
}

enum class SkipReason {
    MISSING_KEY,
    BUDGET_EXHAUSTED,
    DISABLED,
    UNSUPPORTED_CASE,
}

enum class FailureCategory {
    NETWORK,
    HTTP,
    PARSE,
    INVALID_RESPONSE,
    INTERNAL,
}

/**
 * Structured provider fetch envelope.
 *
 * Success policy: at least one candidate is required. An empty candidate list must be represented
 * as [Failed] with [FailureCategory.INVALID_RESPONSE], not as [Success].
 */
sealed class ProviderFetchResult {

    abstract val providerId: BenchmarkProviderId
    abstract val caseId: String

    /**
     * Successful provider response with parsed route candidates.
     */
    data class Success(
        override val providerId: BenchmarkProviderId,
        override val caseId: String,
        val requestId: String,
        val candidates: List<BenchmarkRouteCandidate>,
        val httpStatus: Int,
        val latencyMs: Long,
    ) : ProviderFetchResult() {
        init {
            require(requestId.isNotBlank()) { "requestId must not be blank" }
            require(candidates.isNotEmpty()) {
                "Success requires at least one candidate; use Failed(INVALID_RESPONSE) for empty results"
            }
            require(latencyMs >= 0) { "latencyMs must be >= 0" }
            require(httpStatus in VALID_HTTP_STATUS_RANGE) {
                "httpStatus must be in $VALID_HTTP_STATUS_RANGE"
            }
        }

        override fun toString(): String =
            "Success(providerId=$providerId, caseId=$caseId, requestId=$requestId, " +
                "candidateCount=${candidates.size}, httpStatus=$httpStatus, latencyMs=$latencyMs)"
    }

    /**
     * Pre-network skip. [requestId] is optional because no reservation may exist yet.
     */
    data class Skipped(
        override val providerId: BenchmarkProviderId,
        override val caseId: String,
        val requestId: String? = null,
        val reason: SkipReason,
        val message: String,
    ) : ProviderFetchResult() {
        init {
            if (requestId == null) {
                require(reason in PRE_NETWORK_SKIP_REASONS) {
                    "requestId is required for skip reason $reason"
                }
            }
        }

        override fun toString(): String =
            "Skipped(providerId=$providerId, caseId=$caseId, requestId=$requestId, " +
                "reason=$reason, message=${sanitizeMessage(message)})"
    }

    /**
     * Post-reservation failure. [requestId] is always required.
     */
    data class Failed(
        override val providerId: BenchmarkProviderId,
        override val caseId: String,
        val requestId: String,
        val errorCategory: FailureCategory,
        val httpStatus: Int? = null,
        val latencyMs: Long? = null,
        val message: String,
    ) : ProviderFetchResult() {
        init {
            require(requestId.isNotBlank()) { "requestId must not be blank for Failed results" }
            httpStatus?.let { status ->
                require(status in VALID_HTTP_STATUS_RANGE) {
                    "httpStatus must be in $VALID_HTTP_STATUS_RANGE"
                }
            }
            latencyMs?.let { latency ->
                require(latency >= 0) { "latencyMs must be >= 0" }
            }
        }

        override fun toString(): String =
            "Failed(providerId=$providerId, caseId=$caseId, requestId=$requestId, " +
                "errorCategory=$errorCategory, httpStatus=$httpStatus, latencyMs=$latencyMs, " +
                "message=${sanitizeMessage(message)})"
    }

    companion object {
        private val VALID_HTTP_STATUS_RANGE = 100..599
        private val PRE_NETWORK_SKIP_REASONS =
            setOf(
                SkipReason.MISSING_KEY,
                SkipReason.BUDGET_EXHAUSTED,
                SkipReason.DISABLED,
                SkipReason.UNSUPPORTED_CASE,
            )

        /**
         * Builds [Success] when candidates are present; otherwise returns [Failed] with
         * [FailureCategory.INVALID_RESPONSE].
         */
        fun buildSuccess(
            providerId: BenchmarkProviderId,
            caseId: String,
            requestId: String,
            candidates: List<BenchmarkRouteCandidate>,
            httpStatus: Int,
            latencyMs: Long,
        ): ProviderFetchResult {
            if (candidates.isEmpty()) {
                return Failed(
                    providerId = providerId,
                    caseId = caseId,
                    requestId = requestId,
                    errorCategory = FailureCategory.INVALID_RESPONSE,
                    httpStatus = httpStatus,
                    latencyMs = latencyMs,
                    message = "Provider returned no route candidates",
                )
            }
            return Success(
                providerId = providerId,
                caseId = caseId,
                requestId = requestId,
                candidates = candidates,
                httpStatus = httpStatus,
                latencyMs = latencyMs,
            )
        }
    }
}

internal fun sanitizeMessage(message: String): String = CredentialRedactor.redact(message)
