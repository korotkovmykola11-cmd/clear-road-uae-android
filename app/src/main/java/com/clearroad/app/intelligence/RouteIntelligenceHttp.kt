package com.clearroad.app.intelligence

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.text.Charsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface RouteIntelligenceHttpClient {
    suspend fun get(url: String): Result<String>
}

internal object RouteIntelligenceHttp : RouteIntelligenceHttpClient {
    private const val CONNECT_TIMEOUT_MS = 20_000
    private const val READ_TIMEOUT_MS = 30_000

    override suspend fun get(url: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    requestMethod = "GET"
                }
                try {
                    val code = connection.responseCode
                    val stream =
                        if (code in 200..299) {
                            connection.inputStream
                        } else {
                            connection.errorStream ?: connection.inputStream
                        }
                    val body = stream.bufferedReader().use { it.readText() }
                    if (code !in 200..299) {
                        error("HTTP $code: ${body.take(200)}")
                    }
                    body
                } finally {
                    connection.disconnect()
                }
            }
        }

    fun buildOverpassUrl(
        endpoint: String,
        query: String,
    ): String =
        "$endpoint?data=${URLEncoder.encode(query, Charsets.UTF_8.name())}"
}
