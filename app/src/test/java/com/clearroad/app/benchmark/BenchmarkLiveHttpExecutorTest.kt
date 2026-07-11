package com.clearroad.app.benchmark

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

class BenchmarkLiveHttpExecutorTest {

    private lateinit var server: HttpServer
    private var port: Int = 0
    private val attemptCounter = AtomicInteger(0)

    @Before
    fun setUp() {
        attemptCounter.set(0)
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/get") { exchange ->
            attemptCounter.incrementAndGet()
            val response = """{"status":"ok"}"""
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, response.length.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
            exchange.close()
        }
        server.createContext("/post") { exchange ->
            attemptCounter.incrementAndGet()
            val requestBody = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
            val response = """{"received":${requestBody.length}}"""
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, response.length.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
            exchange.close()
        }
        server.createContext("/missing") { exchange ->
            attemptCounter.incrementAndGet()
            val body = "not found"
            exchange.sendResponseHeaders(404, body.length.toLong())
            exchange.responseBody.use { it.write(body.toByteArray()) }
            exchange.close()
        }
        server.createContext("/error") { exchange ->
            attemptCounter.incrementAndGet()
            val body = "server error"
            exchange.sendResponseHeaders(500, body.length.toLong())
            exchange.responseBody.use { it.write(body.toByteArray()) }
            exchange.close()
        }
        server.createContext("/redirect") { exchange ->
            attemptCounter.incrementAndGet()
            exchange.responseHeaders.add("Location", "http://127.0.0.1:$port/get")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        server.createContext("/slow") { exchange ->
            attemptCounter.incrementAndGet()
            Thread.sleep(250)
            val response = "late"
            exchange.sendResponseHeaders(200, response.length.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
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
    fun successfulGet_returnsBodyAndSuccess() {
        val executor = BenchmarkLiveHttpExecutor.stage0B()
        val permit = grantPermit()

        val result =
            executor.execute(
                request(
                    permit = permit,
                    method = BenchmarkLiveHttpExecutor.HttpMethod.GET,
                    url = "http://127.0.0.1:$port/get",
                ),
            )

        assertTrue(result.success)
        assertEquals(200, result.httpStatus)
        assertEquals("""{"status":"ok"}""", result.body)
        assertEquals(null, result.errorCategory)
    }

    @Test
    fun successfulPost_sendsBody() {
        val executor = BenchmarkLiveHttpExecutor.stage0B()
        val permit = grantPermit()

        val result =
            executor.execute(
                request(
                    permit = permit,
                    method = BenchmarkLiveHttpExecutor.HttpMethod.POST,
                    url = "http://127.0.0.1:$port/post",
                    body = """{"origin":"difc"}""",
                ),
            )

        assertTrue(result.success)
        assertEquals(200, result.httpStatus)
        assertEquals("""{"received":17}""", result.body)
    }

    @Test
    fun http404_mapsToHttpError() {
        val executor = BenchmarkLiveHttpExecutor.stage0B()
        val result =
            executor.execute(
                request(
                    permit = grantPermit(),
                    url = "http://127.0.0.1:$port/missing",
                ),
            )

        assertFalse(result.success)
        assertEquals(404, result.httpStatus)
        assertEquals(BenchmarkLiveHttpExecutor.ErrorCategory.HTTP_ERROR, result.errorCategory)
    }

    @Test
    fun http500_mapsToHttpError() {
        val executor = BenchmarkLiveHttpExecutor.stage0B()
        val result =
            executor.execute(
                request(
                    permit = grantPermit(),
                    url = "http://127.0.0.1:$port/error",
                ),
            )

        assertFalse(result.success)
        assertEquals(500, result.httpStatus)
        assertEquals(BenchmarkLiveHttpExecutor.ErrorCategory.HTTP_ERROR, result.errorCategory)
    }

    @Test
    fun redirect_isRejectedAndNotFollowed() {
        val executor = BenchmarkLiveHttpExecutor.stage0B()
        val result =
            executor.execute(
                request(
                    permit = grantPermit(),
                    url = "http://127.0.0.1:$port/redirect",
                ),
            )

        assertFalse(result.success)
        assertEquals(302, result.httpStatus)
        assertEquals(BenchmarkLiveHttpExecutor.ErrorCategory.REDIRECT_REJECTED, result.errorCategory)
        assertEquals(1, attemptCounter.get())
    }

    @Test
    fun connectionTimeout_mapsToConnectionTimeout() {
        val executor =
            BenchmarkLiveHttpExecutor.stage0B { _: URL ->
                throw SocketTimeoutException("connect timed out")
            }

        val result =
            executor.execute(
                request(
                    permit = grantPermit(),
                    url = "http://127.0.0.1:$port/get",
                ),
            )

        assertFalse(result.success)
        assertEquals(BenchmarkLiveHttpExecutor.ErrorCategory.CONNECTION_TIMEOUT, result.errorCategory)
    }

    @Test
    fun readTimeout_mapsToReadTimeout() {
        val executor =
            BenchmarkLiveHttpExecutor.stage0B { _: URL ->
                throw SocketTimeoutException("Read timed out")
            }

        val result =
            executor.execute(
                request(
                    permit = grantPermit(),
                    url = "http://127.0.0.1:$port/get",
                ),
            )

        assertFalse(result.success)
        assertEquals(BenchmarkLiveHttpExecutor.ErrorCategory.READ_TIMEOUT, result.errorCategory)
    }

    @Test
    fun malformedUrl_mapsToInvalidRequest() {
        val executor = BenchmarkLiveHttpExecutor.stage0B()
        val result =
            executor.execute(
                request(
                    permit = grantPermit(),
                    url = "http://[::1",
                ),
            )

        assertFalse(result.success)
        assertEquals(BenchmarkLiveHttpExecutor.ErrorCategory.INVALID_REQUEST, result.errorCategory)
    }

    @Test
    fun requestId_isPreservedInResult() {
        val executor = BenchmarkLiveHttpExecutor.stage0B()
        val permit = grantPermit(caseId = "preserve-case")

        val result =
            executor.execute(
                request(
                    permit = permit,
                    url = "http://127.0.0.1:$port/get",
                ),
            )

        assertEquals(permit.requestId, result.requestId)
    }

    @Test
    fun latency_isRecorded() {
        val executor = BenchmarkLiveHttpExecutor.stage0B()
        val result =
            executor.execute(
                request(
                    permit = grantPermit(),
                    url = "http://127.0.0.1:$port/slow",
                    readTimeoutMs = 2_000,
                ),
            )

        assertTrue(result.latencyMs >= 200L)
    }

    @Test
    fun credentials_areRedactedFromDiagnostics() {
        val secret = "super-secret-key-value"
        val executor = BenchmarkLiveHttpExecutor.stage0B()
        val request =
            request(
                permit = grantPermit(),
                url = "http://127.0.0.1:$port/get?key=$secret&api_key=$secret",
                headers =
                    mapOf(
                        "X-Goog-Api-Key" to secret,
                        "Content-Type" to "application/json",
                    ),
            )

        val result =
            executor.execute(
                request.copy(
                    url = "http://127.0.0.1:$port/get?key=$secret&apiKey=$secret",
                ),
            )

        val diagnostics =
            listOf(
                request.toString(),
                result.toString(),
                result.message,
            ).joinToString(" ")

        assertFalse(diagnostics.contains(secret))
        assertTrue(diagnostics.contains("[REDACTED]"))
    }

    @Test
    fun execute_withoutValidPermit_isRejected() {
        val executor = BenchmarkLiveHttpExecutor.stage0B()
        val invalidPermit = BenchmarkLiveBudget.ReservationResult.Granted(requestId = "   ")

        val result =
            executor.execute(
                request(
                    permit = invalidPermit,
                    url = "http://127.0.0.1:$port/get",
                ),
            )

        assertFalse(result.success)
        assertEquals(BenchmarkLiveHttpExecutor.ErrorCategory.INVALID_REQUEST, result.errorCategory)
        assertEquals(0, attemptCounter.get())
    }

    @Test
    fun execute_makesExactlyOneHttpAttempt() {
        val executor = BenchmarkLiveHttpExecutor.stage0B()
        executor.execute(
            request(
                permit = grantPermit(),
                url = "http://127.0.0.1:$port/get",
            ),
        )

        assertEquals(1, attemptCounter.get())
    }

    @Test
    fun execute_doesNotModifyBudgetCounters() {
        val budget = BenchmarkLiveBudget.stage0B()
        val permit =
            budget.reserve(BenchmarkLiveBudget.LiveProvider.GOOGLE, "budget-case")
                as BenchmarkLiveBudget.ReservationResult.Granted
        val snapshotAfterReserve = budget.snapshot()

        val executor = BenchmarkLiveHttpExecutor.stage0B()
        executor.execute(
            request(
                permit = permit,
                url = "http://127.0.0.1:$port/get",
            ),
        )

        assertEquals(snapshotAfterReserve, budget.snapshot())
    }

    private fun grantPermit(
        provider: BenchmarkLiveBudget.LiveProvider = BenchmarkLiveBudget.LiveProvider.GOOGLE,
        caseId: String = "case-1",
    ): BenchmarkLiveBudget.ReservationResult.Granted {
        val budget = BenchmarkLiveBudget.stage0B()
        val granted =
            budget.reserve(provider, caseId)
        assertTrue(granted is BenchmarkLiveBudget.ReservationResult.Granted)
        return granted as BenchmarkLiveBudget.ReservationResult.Granted
    }

    private fun request(
        permit: BenchmarkLiveBudget.ReservationResult.Granted,
        method: BenchmarkLiveHttpExecutor.HttpMethod = BenchmarkLiveHttpExecutor.HttpMethod.GET,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        connectTimeoutMs: Int = 2_000,
        readTimeoutMs: Int = 2_000,
    ): BenchmarkLiveHttpExecutor.HttpRequest =
        BenchmarkLiveHttpExecutor.HttpRequest(
            provider = BenchmarkLiveBudget.LiveProvider.GOOGLE,
            caseId = "case-1",
            permit = permit,
            method = method,
            url = url,
            headers = headers,
            body = body,
            connectTimeoutMs = connectTimeoutMs,
            readTimeoutMs = readTimeoutMs,
        )
}
