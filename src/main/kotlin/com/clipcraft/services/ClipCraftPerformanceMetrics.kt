package com.clipcraft.services

import kotlin.system.measureNanoTime

object ClipCraftPerformanceMetrics {
    fun <T> measure(label: String, block: () -> T): T {
        val start = System.nanoTime()
        val result = block()
        val durationNs = System.nanoTime() - start
        println("Performance [$label]: ${durationNs / 1_000_000} ms")
        return result
    }
}
