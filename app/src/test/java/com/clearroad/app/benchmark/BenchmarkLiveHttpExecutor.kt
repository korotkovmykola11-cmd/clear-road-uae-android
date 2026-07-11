package com.clearroad.app.benchmark

import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Stage 0B — single network boundary for live benchmark providers.
 *
 * Consumes an already-granted [BenchmarkLiveBudget] permit; never reserves budget.
 */
class BenchmarkLiveHttpExecutor(
    private val openConnection: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as HttpURLConnection
    },
) {

    enum class HttpMethod {
        GET,
        POST,
    }

    enum class ErrorCategory {
        DNS_FAILURE,
        CONNECTION_TIMEOUT,
        READ_TIMEOUT,
        TLS_FAILURE,
        IO_FAILURE,
        REDIRECT_REJECTED,
        HTTP_ERROR,
        INVALID_REQUEST,
    }

    data class HttpRequest(
        val provider: BenchmarkLiveBudget.LiveProvider,
        val caseId: String,
        val permit: BenchmarkLiveBudget.ReservationResult.Granted,
        val method: HttpMethod,
        val url: String,
        val headers: Map<String, String> = emptyMap(),
        val body: String? = null,
        val connectTimeoutMs: Int = DEFAULT_CONNECT_TIMEOUT_MS,
        val readTimeoutMs: Int = DEFAULT_READ_TIMEOUT_MS,
    ) {
        init {
            require(connectTimeoutMs > 0) { "connectTimeoutMs must be positive" }
            require(readTimeoutMs > 0) { "readTimeoutMs must be positive" }
        }

        override fun toString(): String =
            "HttpRequest(provider=$provider, caseId=$caseId, requestId=${permit.requestId}, " +
                "method=$method, url=${CredentialRedactor.redactUrl(url)}, headers=[redacted], " +
                "bodyPresent=${body != null}, connectTimeoutMs=$connectTimeoutMs, readTimeoutMs=$readTimeoutMs)"
    }

    data class HttpResult(
        val success: Boolean,
        val httpStatus: Int?,
        val body: String?,
        val latencyMs: Long,
        val requestId: String,
        val errorCategory: ErrorCategory?,
        val message: String,
    ) {
        override fun toString(): String =
            "HttpResult(success=$success, httpStatus=$httpStatus, latencyMs=$latencyMs, " +
                "requestId=$requestId, errorCategory=$errorCategory, message=$message)"
    }

    fun execute(request: HttpRequest): HttpResult {
        val permitError = validatePermit(request.permit)
        if (permitError != null) {
            return permitError.copy(requestId = request.permit.requestId)
        }

        val startedAt = System.nanoTime()
        return try {
            val url = URL(request.url)
            val connection = openConnection(url)
            connection.instanceFollowRedirects = false
            connection.connectTimeout = request.connectTimeoutMs
            connection.readTimeout = request.readTimeoutMs
            connection.requestMethod = request.method.name

            request.headers.forEach { (name, value) ->
                connection.setRequestProperty(name, value)
            }

            if (request.method == HttpMethod.POST) {
                connection.doOutput = true
                val payload = request.body.orEmpty()
                connection.outputStream.use { output ->
                    output.write(payload.toByteArray(Charsets.UTF_8))
                }
            }

            val status = connection.responseCode
            val responseBody = readResponseBody(connection, status)
            val latencyMs = elapsedMillis(startedAt)

            when (status) {
                in 200..299 ->
                    HttpResult(
                        success = true,
                        httpStatus = status,
                        body = responseBody,
                        latencyMs = latencyMs,
                        requestId = request.permit.requestId,
                        errorCategory = null,
                        message = "HTTP $status",
                    )
                in 300..399 ->
                    HttpResult(
                        success = false,
                        httpStatus = status,
                        body = responseBody,
                        latencyMs = latencyMs,
                        requestId = request.permit.requestId,
                        errorCategory = ErrorCategory.REDIRECT_REJECTED,
                        message = CredentialRedactor.redact("Redirect rejected (HTTP $status)"),
                    )
                else ->
                    HttpResult(
                        success = false,
                        httpStatus = status,
                        body = responseBody,
                        latencyMs = latencyMs,
                        requestId = request.permit.requestId,
                        errorCategory = ErrorCategory.HTTP_ERROR,
                        message = CredentialRedactor.redact("HTTP error $status"),
                    )
            }
        } catch (error: Exception) {
            val latencyMs = elapsedMillis(startedAt)
            mapException(
                error = error,
                requestId = request.permit.requestId,
                latencyMs = latencyMs,
            )
        }
    }

    companion object {
        const val DEFAULT_CONNECT_TIMEOUT_MS = 10_000
        const val DEFAULT_READ_TIMEOUT_MS = 30_000

        fun stage0B(
            openConnection: (URL) -> HttpURLConnection = { url ->
                url.openConnection() as HttpURLConnection
            },
        ): BenchmarkLiveHttpExecutor = BenchmarkLiveHttpExecutor(openConnection)
    }

    private fun validatePermit(permit: BenchmarkLiveBudget.ReservationResult.Granted): HttpResult? {
        if (permit.requestId.isBlank()) {
            return HttpResult(
                success = false,
                httpStatus = null,
                body = null,
                latencyMs = 0L,
                requestId = permit.requestId,
                errorCategory = ErrorCategory.INVALID_REQUEST,
                message = "Missing or invalid budget permit",
            )
        }
        return null
    }

    private fun readResponseBody(connection: HttpURLConnection, status: Int): String? {
        val stream: InputStream? =
            if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }
        return stream?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        }
    }

    private fun mapException(
        error: Exception,
        requestId: String,
        latencyMs: Long,
    ): HttpResult {
        val category =
            when (error) {
                is MalformedURLException,
                is IllegalArgumentException,
                -> ErrorCategory.INVALID_REQUEST
                is UnknownHostException -> ErrorCategory.DNS_FAILURE
                is SSLException -> ErrorCategory.TLS_FAILURE
                is SocketTimeoutException ->
                    if (error.message?.contains("connect", ignoreCase = true) == true) {
                        ErrorCategory.CONNECTION_TIMEOUT
                    } else {
                        ErrorCategory.READ_TIMEOUT
                    }
                is ConnectException -> ErrorCategory.CONNECTION_TIMEOUT
                is IOException -> ErrorCategory.IO_FAILURE
                else -> ErrorCategory.IO_FAILURE
            }

        return HttpResult(
            success = false,
            httpStatus = null,
            body = null,
            latencyMs = latencyMs,
            requestId = requestId,
            errorCategory = category,
            message = CredentialRedactor.redact(error.message ?: error::class.simpleName.orEmpty()),
        )
    }

    private fun elapsedMillis(startedAtNanos: Long): Long =
        (System.nanoTime() - startedAtNanos) / 1_000_000L
}

internal object CredentialRedactor {
    private val queryCredentialPattern =
        Regex("""(?i)(key|api_key|apiKey)=([^&\s"']+)""")
    private val headerCredentialPattern =
        Regex("""(?i)(X-Goog-Api-Key)\s*[:=]\s*([^,\s;]+)""")

    fun redactUrl(url: String): String = redact(url)

    fun redact(text: String): String {
        var sanitized = text
        sanitized = queryCredentialPattern.replace(sanitized) { match ->
            "${match.groupValues[1]}=[REDACTED]"
        }
        sanitized = headerCredentialPattern.replace(sanitized) { match ->
            "${match.groupValues[1]}: [REDACTED]"
        }
        return sanitized
    }
}
