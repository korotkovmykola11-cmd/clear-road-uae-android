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

class GoogleLiveBenchmarkProviderTest {

    private lateinit var server: HttpServer
    private var port: Int = 0
    private val attemptCounter = AtomicInteger(0)

    private var responseStatus: Int = 200
    private var responseBody: String = validRoutesJson()
    private var lastRequestMethod: String? = null
    private var lastRequestPath: String? = null
    private var lastRequestHeaders: Map<String, List<String>> = emptyMap()
    private var lastRequestBody: String? = null

    private val apiKey = "test-google-api-key-secret"

    @Before
    fun setUp() {
        attemptCounter.set(0)
        responseStatus = 200
        responseBody = validRoutesJson()

        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            attemptCounter.incrementAndGet()
            lastRequestMethod = exchange.requestMethod
            lastRequestPath = exchange.requestURI.path
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
    fun fetch_buildsPostRequest() {
        fetchWithLoopback()

        assertEquals("POST", lastRequestMethod)
    }

    @Test
    fun fetch_usesComputeRoutesEndpoint() {
        fetchWithLoopback()

        assertEquals("/directions/v2:computeRoutes", lastRequestPath)
    }

    @Test
    fun fetch_setsRequiredHeaders() {
        fetchWithLoopback()

        assertEquals("application/json", lastRequestHeaders["Content-Type"]?.first())
        assertEquals(GoogleLiveBenchmarkProvider.FIELD_MASK, lastRequestHeaders["X-Goog-FieldMask"]?.first())
        assertEquals(apiKey, lastRequestHeaders["X-Goog-Api-Key"]?.first())
    }

    @Test
    fun fetch_keepsApiKeyOnlyInHeader_notDiagnostics() {
        val result = fetchWithLoopback()

        assertEquals(apiKey, lastRequestHeaders["X-Goog-Api-Key"]?.first())
        assertFalse(result.toString().contains(apiKey))
    }

    @Test
    fun fieldMask_containsOnlyParsedFields() {
        fetchWithLoopback()

        assertEquals(GoogleRoutesResponseParser.PARSED_FIELD_MASK, GoogleLiveBenchmarkProvider.FIELD_MASK)
        assertEquals(GoogleRoutesResponseParser.PARSED_FIELD_MASK, lastRequestHeaders["X-Goog-FieldMask"]?.first())
    }

    @Test
    fun fetch_makesExactlyOneExecuteCall() {
        fetchWithLoopback()
        assertEquals(1, attemptCounter.get())
    }

    @Test
    fun fetch_preservesPermitRequestId() {
        val permit = grantPermit(caseId = "preserve-id")
        val result = fetchWithLoopback(permit = permit) as ProviderFetchResult.Success

        assertEquals(permit.requestId, result.requestId)
    }

    @Test
    fun fetch_validBody_returnsSuccess() {
        val result = fetchWithLoopback()

        assertTrue(result is ProviderFetchResult.Success)
        val success = result as ProviderFetchResult.Success
        assertEquals(BenchmarkProviderId.GOOGLE, success.providerId)
        assertEquals(1, success.candidates.size)
        assertEquals(200, success.httpStatus)
    }

    @Test
    fun fetch_http404_returnsFailedHttp() {
        responseStatus = 404
        responseBody = """{"error":"not found"}"""

        val result = fetchWithLoopback()

        assertTrue(result is ProviderFetchResult.Failed)
        val failed = result as ProviderFetchResult.Failed
        assertEquals(FailureCategory.HTTP, failed.errorCategory)
        assertEquals(404, failed.httpStatus)
    }

    @Test
    fun fetch_http500_returnsFailedHttp() {
        responseStatus = 500
        responseBody = """{"error":"server error"}"""

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
            provider(
                httpExecutor = executor,
                endpointUrl = loopbackEndpoint(),
            ).fetch(sampleCase(), grantPermit())

        assertTrue(result is ProviderFetchResult.Failed)
        assertEquals(FailureCategory.NETWORK, (result as ProviderFetchResult.Failed).errorCategory)
        assertEquals(0, attemptCounter.get())
    }

    @Test
    fun fetch_malformedJson_returnsFailedParse() {
        responseBody = """{ "routes": ["""

        val result = fetchWithLoopback()

        assertTrue(result is ProviderFetchResult.Failed)
        assertEquals(FailureCategory.PARSE, (result as ProviderFetchResult.Failed).errorCategory)
    }

    @Test
    fun fetch_emptyRoutes_returnsInvalidResponse() {
        responseBody = """{ "routes": [] }"""

        val result = fetchWithLoopback()

        assertTrue(result is ProviderFetchResult.Failed)
        assertEquals(FailureCategory.INVALID_RESPONSE, (result as ProviderFetchResult.Failed).errorCategory)
    }

    @Test
    fun providerId_isGoogle() {
        assertEquals(BenchmarkProviderId.GOOGLE, provider().providerId)
    }

    @Test
    fun fetch_doesNotReserveBudget() {
        val budget = BenchmarkLiveBudget.stage0B()
        val permit =
            budget.reserve(BenchmarkLiveBudget.LiveProvider.GOOGLE, "budget-case")
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
    fun fetch_requestBodyUsesCaseCoordinates() {
        val case = sampleCase()
        fetchWithLoopback(case = case)

        assertTrue(lastRequestBody!!.contains("\"latitude\":${case.originLat}"))
        assertTrue(lastRequestBody!!.contains("\"latitude\":${case.destinationLat}"))
        assertTrue(lastRequestBody!!.contains("\"travelMode\":\"DRIVE\""))
        assertTrue(lastRequestBody!!.contains("\"routingPreference\":\"TRAFFIC_AWARE\""))
        assertTrue(lastRequestBody!!.contains("\"polylineQuality\":\"HIGH_QUALITY\""))
        assertTrue(lastRequestBody!!.contains("\"computeAlternativeRoutes\":true"))
        assertTrue(lastRequestBody!!.contains("\"TRAFFIC_ON_POLYLINE\""))
        assertTrue(lastRequestBody!!.contains("\"TOLLS\""))
    }

    @Test
    fun fetch_resultToString_doesNotLeakSecrets() {
        val result = fetchWithLoopback()
        val diagnostics = listOf(result.toString(), lastRequestBody.orEmpty()).joinToString(" ")

        assertFalse(diagnostics.contains(apiKey))
    }

    private fun fetchWithLoopback(
        case: UaeRouteBenchmarkCase = sampleCase(),
        permit: BenchmarkLiveBudget.ReservationResult.Granted = grantPermit(),
    ): ProviderFetchResult =
        provider(endpointUrl = loopbackEndpoint()).fetch(case, permit)

    private fun provider(
        httpExecutor: BenchmarkLiveHttpExecutor = BenchmarkLiveHttpExecutor.stage0B(),
        endpointUrl: String = GoogleLiveBenchmarkProvider.COMPUTE_ROUTES_URL,
    ): GoogleLiveBenchmarkProvider =
        GoogleLiveBenchmarkProvider(
            apiKey = apiKey,
            httpExecutor = httpExecutor,
            endpointUrl = endpointUrl,
        )

    private fun loopbackEndpoint(): String =
        "http://127.0.0.1:$port/directions/v2:computeRoutes"

    private fun grantPermit(
        caseId: String = "difc-marina",
    ): BenchmarkLiveBudget.ReservationResult.Granted {
        val budget = BenchmarkLiveBudget.stage0B()
        val granted = budget.reserve(BenchmarkLiveBudget.LiveProvider.GOOGLE, caseId)
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

    private fun validRoutesJson(): String {
        val encoded =
            PolyUtil.encode(
                listOf(
                    LatLng(25.2100, 55.2750),
                    LatLng(25.0800, 55.1400),
                ),
            )
        return """
            {
              "routes": [
                {
                  "distanceMeters": 18000,
                  "duration": "1200s",
                  "description": "Sheikh Zayed Rd/E11",
                  "polyline": { "encodedPolyline": "$encoded" }
                }
              ]
            }
            """.trimIndent()
    }
}
