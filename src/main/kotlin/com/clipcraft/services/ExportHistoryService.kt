package com.clipcraft.services

import com.clipcraft.model.ExportHistoryEntry
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Service to manage export history
 */
@Service
@State(name = "ClipCraftExportHistory", storages = [Storage("clipcraft-export-history.xml")])
class ExportHistoryService : PersistentStateComponent<ExportHistoryService.State> {

    data class State(
        var historyJson: String = "[]",
        var maxHistorySize: Int = 100
    )

    private var myState = State()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        fun getInstance(): ExportHistoryService {
            return ApplicationManager.getApplication().getService(ExportHistoryService::class.java)
        }
    }

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun addEntry(entry: ExportHistoryEntry) {
        val history = getHistory().toMutableList()
        history.add(0, entry) // Add to beginning

        // Keep only max entries
        if (history.size > myState.maxHistorySize) {
            history.subList(myState.maxHistorySize, history.size).clear()
        }

        saveHistory(history)
    }

    fun getHistory(): List<ExportHistoryEntry> {
        return try {
            json.decodeFromString<List<ExportHistoryEntry>>(myState.historyJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getRecentHistory(limit: Int = 10): List<ExportHistoryEntry> {
        return getHistory().take(limit)
    }

    fun searchHistory(query: String): List<ExportHistoryEntry> {
        val lowerQuery = query.lowercase()
        return getHistory().filter { entry ->
            entry.presetName?.lowercase()?.contains(lowerQuery) == true ||
            entry.exportType.lowercase().contains(lowerQuery) ||
            entry.format.lowercase().contains(lowerQuery) ||
            entry.exportedPaths.any { it.lowercase().contains(lowerQuery) } ||
            entry.preview.lowercase().contains(lowerQuery)
        }
    }

    fun getHistoryByType(exportType: String): List<ExportHistoryEntry> {
        return getHistory().filter { it.exportType == exportType }
    }

    fun getHistoryByPreset(presetName: String): List<ExportHistoryEntry> {
        return getHistory().filter { it.presetName == presetName }
    }

    fun getHistoryInLastDays(days: Int): List<ExportHistoryEntry> {
        val cutoffTime = Instant.now().minus(days.toLong(), ChronoUnit.DAYS).toEpochMilli()
        return getHistory().filter { it.timestamp >= cutoffTime }
    }

    fun deleteEntry(id: String) {
        val history = getHistory().toMutableList()
        history.removeIf { it.id == id }
        saveHistory(history)
    }

    fun clearHistory() {
        myState.historyJson = "[]"
    }

    fun getStatistics(): HistoryStatistics {
        val history = getHistory()
        return HistoryStatistics(
            totalExports = history.size,
            totalFilesExported = history.sumOf { it.filesExported },
            totalSize = history.sumOf { it.totalSize },
            totalTokens = history.sumOf { it.estimatedTokens },
            successfulExports = history.count { it.success },
            failedExports = history.count { !it.success },
            mostUsedFormat = history.groupingBy { it.format }.eachCount().maxByOrNull { it.value }?.key ?: "N/A",
            mostUsedPreset = history.mapNotNull { it.presetName }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "N/A"
        )
    }

    private fun saveHistory(history: List<ExportHistoryEntry>) {
        myState.historyJson = json.encodeToString(history)
    }

    data class HistoryStatistics(
        val totalExports: Int,
        val totalFilesExported: Int,
        val totalSize: Long,
        val totalTokens: Int,
        val successfulExports: Int,
        val failedExports: Int,
        val mostUsedFormat: String,
        val mostUsedPreset: String
    )
}
