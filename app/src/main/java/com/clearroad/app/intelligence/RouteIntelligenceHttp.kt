package com.clearroad.app.intelligence

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import android.util.Log
import kotlin.text.Charsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface RouteIntelligenceHttpClient {
    suspend fun get(
        url: String,
        attempt: RouteIntelligenceDiag.HttpAttemptContext? = null,
    ): Result<String>
}

internal object RouteIntelligenceHttp : RouteIntelligenceHttpClient {
    private const val CONNECT_TIMEOUT_MS = 20_000
    private const val READ_TIMEOUT_MS = 30_000

    override suspend fun get(
        url: String,
        attempt: RouteIntelligenceDiag.HttpAttemptContext?,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            attempt?.let(RouteIntelligenceDiag::logHttpStart)
            val startedAtMs = System.currentTimeMillis()
            var phase = RouteIntelligenceDiag.HttpPhase.CONNECT_RESPONSE
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    requestMethod = "GET"
                }
                try {
                    val code = connection.responseCode
                    phase = RouteIntelligenceDiag.HttpPhase.RESPONSE_BODY
                    val stream =
                        if (code in 200..299) {
                            connection.inputStream
                        } else {
                            connection.errorStream ?: connection.inputStream
                        }
                    val body = stream.bufferedReader().use { it.readText() }
                    if (code !in 200..299) {
                        throw RouteIntelligenceHttpStatusException(code)
                    }
                    val durationMs = System.currentTimeMillis() - startedAtMs
                    attempt?.let {
                        RouteIntelligenceDiag.logHttpOutcome(
                            attempt = it,
                            outcome = RouteIntelligenceDiag.HttpOutcome.SUCCESS,
                            durationMs = durationMs,
                            statusCode = code,
                        )
                    }
                    Result.success(body)
                } finally {
                    connection.disconnect()
                }
            } catch (throwable: Throwable) {
                val durationMs = System.currentTimeMillis() - startedAtMs
                when (throwable) {
                    is RouteIntelligenceHttpStatusException -> {
                        attempt?.let {
                            RouteIntelligenceDiag.logHttpOutcome(
                                attempt = it,
                                outcome = RouteIntelligenceDiag.HttpOutcome.HTTP_ERROR,
                                durationMs = durationMs,
                                statusCode = throwable.statusCode,
                            )
                        }
                        Result.failure(throwable)
                    }
                    else -> {
                        attempt?.let {
                            RouteIntelligenceDiag.logHttpOutcome(
                                attempt = it,
                                outcome = RouteIntelligenceDiag.HttpOutcome.EXCEPTION,
                                durationMs = durationMs,
                                exceptionClass = RouteIntelligenceDiag.exceptionSimpleName(throwable),
                                phase = phase,
                            )
                        }
                        Result.failure(throwable)
                    }
                }
            }
        }

    fun buildOverpassUrl(
        endpoint: String,
        query: String,
    ): String =
        "$endpoint?data=${URLEncoder.encode(query, Charsets.UTF_8.name())}"
}

internal object RouteIntelligenceDiag {

    const val TAG = "MARSHIO_INTELLIGENCE_DIAG"

    enum class EndpointRole {
        PRIMARY,
        FALLBACK,
    }

    enum class HttpOutcome {
        SUCCESS,
        HTTP_ERROR,
        EXCEPTION,
    }

    enum class HttpPhase {
        CONNECT_RESPONSE,
        RESPONSE_BODY,
        UNKNOWN,
    }

    enum class ParseErrorKind {
        MalformedJson,
    }

    enum class PresentationCardResult {
        CARD_AVAILABLE,
        CARD_PARTIAL,
        CARD_UNAVAILABLE,
    }

    data class HttpAttemptContext(
        val routeIndex: Int,
        val endpoint: EndpointRole,
    )

    internal var lineLogger: (String) -> Unit = { line ->
        runCatching { Log.d(TAG, line) }
    }

    private fun emit(line: String) {
        lineLogger(line)
    }

    fun logHttpStart(attempt: HttpAttemptContext) {
        emit("RI_HTTP route=${attempt.routeIndex} endpoint=${attempt.endpoint.name} event=START")
    }

    fun logHttpOutcome(
        attempt: HttpAttemptContext,
        outcome: HttpOutcome,
        durationMs: Long,
        statusCode: Int? = null,
        exceptionClass: String? = null,
        phase: HttpPhase = HttpPhase.UNKNOWN,
    ) {
        emit(formatHttpOutcome(attempt, outcome, durationMs, statusCode, exceptionClass, phase))
    }

    fun formatHttpOutcome(
        attempt: HttpAttemptContext,
        outcome: HttpOutcome,
        durationMs: Long,
        statusCode: Int? = null,
        exceptionClass: String? = null,
        phase: HttpPhase = HttpPhase.UNKNOWN,
    ): String =
        buildString {
            append("RI_HTTP route=${attempt.routeIndex}")
            append(" endpoint=${attempt.endpoint.name}")
            append(" outcome=${outcome.name}")
            statusCode?.let { append(" status=$it") }
            exceptionClass?.let { append(" exception=$it") }
            if (outcome == HttpOutcome.EXCEPTION) {
                append(" phase=${phase.name}")
            }
            append(" durationMs=$durationMs")
        }

    fun logParse(
        routeIndex: Int,
        error: ParseErrorKind,
    ) {
        emit("RI_PARSE route=$routeIndex outcome=PARSE_ERROR error=${error.name}")
    }

    fun logOsmResult(
        routeIndex: Int,
        status: SourceStatus,
    ) {
        emit("RI_OSM_RESULT route=$routeIndex status=${status.name}")
    }

    fun logPresentation(
        marshioRouteIndex: Int,
        marshioStatus: SourceStatus,
        alternativeRouteIndex: Int?,
        alternativeStatus: SourceStatus?,
        result: PresentationCardResult,
    ) {
        emit(
            buildString {
                append("RI_PRESENTATION marshioRoute=$marshioRouteIndex")
                append(" marshioStatus=${marshioStatus.name}")
                alternativeRouteIndex?.let { append(" alternativeRoute=$it") }
                alternativeStatus?.let { append(" alternativeStatus=${it.name}") }
                append(" result=${result.name}")
            },
        )
    }

    internal fun classifyPresentationResult(
        marshioStatus: SourceStatus,
        alternativeStatus: SourceStatus?,
    ): PresentationCardResult =
        when {
            marshioStatus == SourceStatus.UNAVAILABLE &&
                (alternativeStatus == null || alternativeStatus == SourceStatus.UNAVAILABLE) ->
                PresentationCardResult.CARD_UNAVAILABLE
            marshioStatus == SourceStatus.OK &&
                alternativeStatus == SourceStatus.OK ->
                PresentationCardResult.CARD_AVAILABLE
            else ->
                PresentationCardResult.CARD_PARTIAL
        }

    internal fun exceptionSimpleName(throwable: Throwable): String =
        throwable.javaClass.simpleName.ifBlank { throwable.javaClass.name.substringAfterLast('.') }
}

internal class RouteIntelligenceHttpStatusException(
    val statusCode: Int,
) : Exception()
