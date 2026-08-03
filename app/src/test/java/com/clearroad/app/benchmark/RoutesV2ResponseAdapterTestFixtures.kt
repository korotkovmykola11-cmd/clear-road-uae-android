package com.clearroad.app.benchmark

import java.nio.charset.StandardCharsets

internal object RoutesV2ResponseAdapterTestFixtures {
    private const val ROOT = "benchmark/routes-v2-traffic"

    fun read(fileName: String): String {
        val path = "$ROOT/$fileName"
        val stream =
            checkNotNull(javaClass.classLoader.getResourceAsStream(path)) {
                "Missing test resource: $path"
            }
        return stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }
}
