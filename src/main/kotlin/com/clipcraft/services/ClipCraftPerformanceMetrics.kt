package com.clipcraft.services

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

class ClipCraftPerformanceMetrics(project: Project) {
    private val logger = Logger.getInstance(ClipCraftPerformanceMetrics::class.java)
    private var startTime: Long = 0
    fun startProcessing() {
        startTime = System.currentTimeMillis()
    }

    fun stopProcessingAndLog(operationName: String) {
        val elapsed = System.currentTimeMillis() - startTime
        logger.info("$operationName took $elapsed ms")
    }
}
