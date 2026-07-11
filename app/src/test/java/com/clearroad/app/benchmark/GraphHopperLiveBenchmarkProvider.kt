package com.clearroad.app.benchmark

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Stage 0B — GraphHopper Cloud live benchmark provider.
 *
 * Request shape (official GraphHopper Routing API):
 * - Method: POST
 * - URL: https://graphhopper.com/api/1/route?key={apiKey}  (key is query parameter)
 * - Body (application/json):
 *   - profile = "car"
 *   - points = [[originLng, originLat], [destinationLng, destinationLat]]
 *   - instructions = true
 *   - calc_points = true
 *   - points_encoded = true
 *   - algorithm = "alternative_route"
 *   - alternative_route.max_paths = 3
 *
 * One [BenchmarkLiveHttpExecutor.execute] call per [fetch]; never reserves budget.
 */
class GraphHopperLiveBenchmarkProvider(
    private val apiKey: String,
    private val httpExecutor: BenchmarkLiveHttpExecutor,
    private val parser: GraphHopperResponseParser = GraphHopperResponseParser,
    private val endpointUrl: String? = null,
) : BenchmarkLiveProvider {

    override val providerId: BenchmarkProviderId = BenchmarkProviderId.GRAPHHOPPER
    override val displayName: String = "GraphHopper"

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
                message = "GraphHopper API key not configured",
            )
        }

        val httpResult =
            httpExecutor.execute(
                BenchmarkLiveHttpExecutor.HttpRequest(
                    provider = BenchmarkLiveBudget.LiveProvider.GRAPHHOPPER,
                    caseId = case.caseId,
                    permit = permit,
                    method = BenchmarkLiveHttpExecutor.HttpMethod.POST,
                    url = resolveEndpointUrl(),
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
            is GraphHopperResponseParser.ParseResult.Failure ->
                ProviderFetchResult.Failed(
                    providerId = providerId,
                    caseId = case.caseId,
                    requestId = permit.requestId,
                    errorCategory = categorizeParseFailure(parseResult.message),
                    httpStatus = httpResult.httpStatus,
                    latencyMs = httpResult.latencyMs,
                    message = parseResult.message,
                )
            is GraphHopperResponseParser.ParseResult.Success ->
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
        const val ROUTE_BASE_URL = "https://graphhopper.com/api/1/route"
        const val MAX_ALTERNATIVE_PATHS = 3

        fun buildEndpointUrl(apiKey: String): String {
            val encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
            return "$ROUTE_BASE_URL?key=$encodedKey"
        }
    }

    private fun resolveEndpointUrl(): String = endpointUrl ?: buildEndpointUrl(apiKey)

    private fun buildHeaders(): Map<String, String> =
        mapOf(
            "Content-Type" to "application/json",
        )

    private fun buildRequestBody(case: UaeRouteBenchmarkCase): String =
        JSONObject()
            .apply {
                put("profile", "car")
                put(
                    "points",
                    JSONArray()
                        .put(pointArray(case.originLng, case.originLat))
                        .put(pointArray(case.destinationLng, case.destinationLat)),
                )
                put("instructions", true)
                put("calc_points", true)
                put("points_encoded", true)
                put("algorithm", "alternative_route")
                put("alternative_route.max_paths", MAX_ALTERNATIVE_PATHS)
            }.toString()

    private fun pointArray(longitude: Double, latitude: Double): JSONArray =
        JSONArray().put(longitude).put(latitude)

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
            message.contains("No paths") ||
                message.contains("No valid route candidates") ||
                message.contains("GraphHopper error") ->
                FailureCategory.INVALID_RESPONSE
            else -> FailureCategory.PARSE
        }
}
