package com.clipcraft.services

import org.slf4j.LoggerFactory

object ClipCraftPerformanceMetrics {
    private val log = LoggerFactory.getLogger(ClipCraftPerformanceMetrics::class.java)

    fun <T> measure(label: String, block: () -> T): T {
        val startNs = System.nanoTime()
        val result = block()
        val durationMs = (System.nanoTime() - startNs) / 1_000_000
        log.info("Performance [$label]: ${durationMs} ms")
        return result
    }
}
