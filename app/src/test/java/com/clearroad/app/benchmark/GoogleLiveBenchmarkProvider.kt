package com.clearroad.app.benchmark

import org.json.JSONArray
import org.json.JSONObject

/**
 * Stage 0B — Google Routes API v2 live benchmark provider.
 *
 * One [BenchmarkLiveHttpExecutor.execute] call per [fetch]; never reserves budget.
 */
class GoogleLiveBenchmarkProvider(
    private val apiKey: String,
    private val httpExecutor: BenchmarkLiveHttpExecutor,
    private val parser: GoogleRoutesResponseParser = GoogleRoutesResponseParser,
    private val endpointUrl: String = COMPUTE_ROUTES_URL,
) : BenchmarkLiveProvider {

    override val providerId: BenchmarkProviderId = BenchmarkProviderId.GOOGLE
    override val displayName: String = "Google Maps"

    override fun fetch(
        case: UaeRouteBenchmarkCase,
        permit: BenchmarkLiveBudget.ReservationResult.Granted,
    ): ProviderFetchResult {
        if (apiKey.isBlank()) {
            return ProviderFetchResult.Skipped(
                providerId = providerId,
                caseId = case.caseId,
                requestId = permit.requestId,
                reason = SkipReason.MISSING_KEY,
                message = "Google Maps API key not configured",
            )
        }

        val httpResult =
            httpExecutor.execute(
                BenchmarkLiveHttpExecutor.HttpRequest(
                    provider = BenchmarkLiveBudget.LiveProvider.GOOGLE,
                    caseId = case.caseId,
                    permit = permit,
                    method = BenchmarkLiveHttpExecutor.HttpMethod.POST,
                    url = endpointUrl,
                    headers = buildHeaders(),
                    body = buildRequestBody(case),
                ),
            )

        if (!httpResult.success) {
            return ProviderFetchResult.Failed(
                providerId = providerId,
                caseId = case.caseId,
                requestId = permit.requestId,
                errorCategory = mapTransportFailure(httpResult.errorCategory),
                httpStatus = httpResult.httpStatus,
                latencyMs = httpResult.latencyMs,
                message = httpResult.message,
            )
        }

        return when (
            val parseResult =
                parser.parse(
                    responseJson = httpResult.body.orEmpty(),
                    caseId = case.caseId,
                    requestId = permit.requestId,
                )
        ) {
            is GoogleRoutesResponseParser.ParseResult.Failure ->
                ProviderFetchResult.Failed(
                    providerId = providerId,
                    caseId = case.caseId,
                    requestId = permit.requestId,
                    errorCategory = categorizeParseFailure(parseResult.message),
                    httpStatus = httpResult.httpStatus,
                    latencyMs = httpResult.latencyMs,
                    message = parseResult.message,
                )
            is GoogleRoutesResponseParser.ParseResult.Success ->
                ProviderFetchResult.buildSuccess(
                    providerId = providerId,
                    caseId = case.caseId,
                    requestId = permit.requestId,
                    candidates = parseResult.candidates,
                    httpStatus = httpResult.httpStatus ?: 0,
                    latencyMs = httpResult.latencyMs,
                )
        }
    }

    companion object {
        const val COMPUTE_ROUTES_URL =
            "https://routes.googleapis.com/directions/v2:computeRoutes"

        val FIELD_MASK: String = GoogleRoutesResponseParser.PARSED_FIELD_MASK
    }

    private fun buildHeaders(): Map<String, String> =
        mapOf(
            "Content-Type" to "application/json",
            "X-Goog-Api-Key" to apiKey,
            "X-Goog-FieldMask" to FIELD_MASK,
        )

    private fun buildRequestBody(case: UaeRouteBenchmarkCase): String =
        JSONObject()
            .apply {
                put("origin", latLngObject(case.originLat, case.originLng))
                put("destination", latLngObject(case.destinationLat, case.destinationLng))
                put("travelMode", "DRIVE")
                put("routingPreference", "TRAFFIC_AWARE")
                put("computeAlternativeRoutes", true)
                put("extraComputations", JSONArray(listOf("TOLLS")))
            }.toString()

    private fun latLngObject(latitude: Double, longitude: Double): JSONObject =
        JSONObject().apply {
            put(
                "location",
                JSONObject().put(
                    "latLng",
                    JSONObject()
                        .put("latitude", latitude)
                        .put("longitude", longitude),
                ),
            )
        }

    private fun mapTransportFailure(
        category: BenchmarkLiveHttpExecutor.ErrorCategory?,
    ): FailureCategory =
        when (category) {
            BenchmarkLiveHttpExecutor.ErrorCategory.HTTP_ERROR -> FailureCategory.HTTP
            BenchmarkLiveHttpExecutor.ErrorCategory.INVALID_REQUEST -> FailureCategory.INTERNAL
            else -> FailureCategory.NETWORK
        }

    private fun categorizeParseFailure(message: String): FailureCategory =
        when {
            message.contains("No routes") || message.contains("No valid route candidates") ->
                FailureCategory.INVALID_RESPONSE
            else -> FailureCategory.PARSE
        }
}
