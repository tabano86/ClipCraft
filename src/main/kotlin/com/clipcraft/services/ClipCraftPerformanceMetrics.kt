package com.clipcraft.services

object ClipCraftPerformanceMetrics {
    fun <T> measure(label: String, block: () -> T): T {
        val startNs = System.nanoTime()
        val result = block()
        val durationNs = System.nanoTime() - startNs
        println("Performance [$label]: ${durationNs / 1_000_000} ms")
        return result
    }
}
