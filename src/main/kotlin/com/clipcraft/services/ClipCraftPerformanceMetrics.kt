package com.clipcraft.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ClipCraftPerformanceMetrics(private val project: Project) {
    private var startTime: Long = 0

    fun startProcessing() {
        startTime = System.currentTimeMillis()
    }

    fun stopProcessingAndLog(taskName: String) {
        val elapsed = System.currentTimeMillis() - startTime
        // In a real scenario, log via Logger or store in a metrics system
        println("[$taskName] Elapsed: ${elapsed}ms")
    }
}
