package com.clearroad.app.benchmark

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

class GraphHopperLiveBenchmarkProviderTest {

    private lateinit var server: HttpServer
    private var port: Int = 0
    private val attemptCounter = AtomicInteger(0)

    private var responseStatus: Int = 200
    private var responseBody: String = validPathsJson()
    private var lastRequestMethod: String? = null
    private var lastRequestPath: String? = null
    private var lastRequestQuery: String? = null
    private var lastRequestHeaders: Map<String, List<String>> = emptyMap()
    private var lastRequestBody: String? = null

    private val apiKey = "test-graphhopper-api-key-secret"

    @Before
    fun setUp() {
        attemptCounter.set(0)
        responseStatus = 200
        responseBody = validPathsJson()

        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            attemptCounter.incrementAndGet()
            lastRequestMethod = exchange.requestMethod
            lastRequestPath = exchange.requestURI.path
            lastRequestQuery = exchange.requestURI.query
            lastRequestHeaders = exchange.requestHeaders
            lastRequestBody = exchange.requestBody.readBytes().toString(Charsets.UTF_8)

            val bodyBytes = responseBody.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(responseStatus, bodyBytes.size.toLong())
            exchange.responseBody.use { it.write(bodyBytes) }
            exchange.close()
        }
        server.start()
        port = server.address.port
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun fetch_makesExactlyOneRequest() {
        fetchWithLoopback()
        assertEquals(1, attemptCounter.get())
    }

    @Test
    fun fetch_usesRouteEndpoint() {
        fetchWithLoopback()
        assertEquals("/api/1/route", lastRequestPath)
    }

    @Test
    fun fetch_putsApiKeyInQueryParameter() {
        fetchWithLoopback()
        assertTrue(lastRequestQuery!!.contains("key=$apiKey"))
    }

    @Test
    fun fetch_usesPostMethod() {
        fetchWithLoopback()
        assertEquals("POST", lastRequestMethod)
    }

    @Test
    fun fetch_requestsAlternativeRoutes() {
        fetchWithLoopback()

        assertTrue(lastRequestBody!!.contains("\"algorithm\":\"alternative_route\""))
        assertTrue(
            lastRequestBody!!.contains(
                "\"alternative_route.max_paths\":${GraphHopperLiveBenchmarkProvider.MAX_ALTERNATIVE_PATHS}",
            ),
        )
        assertTrue(lastRequestBody!!.contains("\"points_encoded\":true"))
        assertTrue(lastRequestBody!!.contains("\"instructions\":true"))
        assertTrue(lastRequestBody!!.contains("\"calc_points\":true"))
    }

    @Test
    fun fetch_keyNeverAppearsInDiagnostics() {
        val result = fetchWithLoopback()
        assertFalse(result.toString().contains(apiKey))
    }

    @Test
    fun fetch_preservesRequestId() {
        val permit = grantPermit(caseId = "preserve-id")
        val result = fetchWithLoopback(permit = permit) as ProviderFetchResult.Success
        assertEquals(permit.requestId, result.requestId)
    }

    @Test
    fun fetch_validBody_returnsSuccess() {
        val result = fetchWithLoopback()
        assertTrue(result is ProviderFetchResult.Success)
        val success = result as ProviderFetchResult.Success
        assertEquals(1, success.candidates.size)
        assertEquals(200, success.httpStatus)
    }

    @Test
    fun fetch_http404_returnsFailedHttp() {
        responseStatus = 404
        responseBody = """{"message":"not found"}"""

        val result = fetchWithLoopback()
        assertTrue(result is ProviderFetchResult.Failed)
        val failed = result as ProviderFetchResult.Failed
        assertEquals(FailureCategory.HTTP, failed.errorCategory)
        assertEquals(404, failed.httpStatus)
    }

    @Test
    fun fetch_http500_returnsFailedHttp() {
        responseStatus = 500
        responseBody = """{"message":"server error"}"""

        val result = fetchWithLoopback()
        assertTrue(result is ProviderFetchResult.Failed)
        assertEquals(FailureCategory.HTTP, (result as ProviderFetchResult.Failed).errorCategory)
    }

    @Test
    fun fetch_timeout_returnsFailedNetwork() {
        val executor =
            BenchmarkLiveHttpExecutor.stage0B { _: URL ->
                throw SocketTimeoutException("Read timed out")
            }

        val result =
            provider(httpExecutor = executor).fetch(sampleCase(), grantPermit())

        assertTrue(result is ProviderFetchResult.Failed)
        assertEquals(FailureCategory.NETWORK, (result as ProviderFetchResult.Failed).errorCategory)
        assertEquals(0, attemptCounter.get())
    }

    @Test
    fun fetch_malformedJson_returnsFailedParse() {
        responseBody = """{ "paths": ["""

        val result = fetchWithLoopback()
        assertTrue(result is ProviderFetchResult.Failed)
        assertEquals(FailureCategory.PARSE, (result as ProviderFetchResult.Failed).errorCategory)
    }

    @Test
    fun fetch_emptyPaths_returnsInvalidResponse() {
        responseBody = """{ "paths": [] }"""

        val result = fetchWithLoopback()
        assertTrue(result is ProviderFetchResult.Failed)
        assertEquals(FailureCategory.INVALID_RESPONSE, (result as ProviderFetchResult.Failed).errorCategory)
    }

    @Test
    fun providerId_isGraphHopper() {
        assertEquals(BenchmarkProviderId.GRAPHHOPPER, provider().providerId)
    }

    @Test
    fun fetch_doesNotReserveBudget() {
        val budget = BenchmarkLiveBudget.stage0B()
        val permit =
            budget.reserve(BenchmarkLiveBudget.LiveProvider.GRAPHHOPPER, "budget-case")
                as BenchmarkLiveBudget.ReservationResult.Granted
        val snapshotAfterReserve = budget.snapshot()

        provider().fetch(sampleCase(), permit)

        assertEquals(snapshotAfterReserve, budget.snapshot())
    }

    @Test
    fun fetch_doesNotRetryOnFailure() {
        responseStatus = 500
        responseBody = "error"
        fetchWithLoopback()
        assertEquals(1, attemptCounter.get())
    }

    @Test
    fun fetch_requestHasNoCorridorViaPoints() {
        fetchWithLoopback()
        val body = lastRequestBody!!
        val pointsCount = "\"points\"".toRegex().findAll(body).count()
        assertEquals(1, pointsCount)
        assertFalse(body.contains("via"))
        assertEquals(2, Regex("""\[[\d.-]+,[\d.-]+\]""").findAll(body).count())
    }

    @Test
    fun fetch_singlePathReturned_doesNotMakeSecondRequest() {
        responseBody = validPathsJson(pathCount = 1)
        fetchWithLoopback()
        assertEquals(1, attemptCounter.get())
    }

    @Test
    fun fetch_resultToString_doesNotLeakSecrets() {
        val result = fetchWithLoopback()
        assertFalse(result.toString().contains(apiKey))
    }

    private fun fetchWithLoopback(
        case: UaeRouteBenchmarkCase = sampleCase(),
        permit: BenchmarkLiveBudget.ReservationResult.Granted = grantPermit(),
    ): ProviderFetchResult =
        provider(endpointUrl = loopbackEndpoint()).fetch(case, permit)

    private fun provider(
        httpExecutor: BenchmarkLiveHttpExecutor = BenchmarkLiveHttpExecutor.stage0B(),
        endpointUrl: String = GraphHopperLiveBenchmarkProvider.buildEndpointUrl(apiKey),
    ): GraphHopperLiveBenchmarkProvider =
        GraphHopperLiveBenchmarkProvider(
            apiKey = apiKey,
            httpExecutor = httpExecutor,
            endpointUrl = endpointUrl,
        )

    private fun loopbackEndpoint(): String =
        "http://127.0.0.1:$port/api/1/route?key=$apiKey"

    private fun grantPermit(
        caseId: String = "difc-marina",
    ): BenchmarkLiveBudget.ReservationResult.Granted {
        val budget = BenchmarkLiveBudget.stage0B()
        val granted = budget.reserve(BenchmarkLiveBudget.LiveProvider.GRAPHHOPPER, caseId)
        assertTrue(granted is BenchmarkLiveBudget.ReservationResult.Granted)
        return granted as BenchmarkLiveBudget.ReservationResult.Granted
    }

    private fun sampleCase(): UaeRouteBenchmarkCase =
        UaeRouteBenchmarkCase(
            caseId = "difc-marina",
            originLabel = "DIFC",
            destinationLabel = "Marina",
            originLat = 25.2100,
            originLng = 55.2750,
            destinationLat = 25.0800,
            destinationLng = 55.1400,
            expectedMajorCorridors = listOf("E11"),
            tags = listOf("urban"),
            notes = "live benchmark case",
        )

    private fun validPathsJson(pathCount: Int = 1): String {
        val encoded =
            PolyUtil.encode(
                listOf(
                    LatLng(25.2100, 55.2750),
                    LatLng(25.0800, 55.1400),
                ),
            )
        val paths =
            (0 until pathCount).joinToString(",") { index ->
                """
                {
                  "distance": ${18_000 + index * 500},
                  "time": ${1_200_000 + index * 50_000},
                  "points": "$encoded",
                  "points_encoded": true
                }
                """.trimIndent()
            }
        return """{ "paths": [ $paths ] }"""
    }
}
