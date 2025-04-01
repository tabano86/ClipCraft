File: ClipCraftAction.kt | Path: C:
/Users/Taban/IdeaProjects/ClipCraft/src/main/kotlin/com/clipcraft/actions/ClipCraftAction.kt | Size: 1531 bytes

File: ClipCraftAction.kt | Path: C:
/Users/Taban/IdeaProjects/ClipCraft/src/main/kotlin/com/clipcraft/actions/ClipCraftAction.kt

```kotlin
package com.clipcraft.actions

import com.clipcraft.concurrency.ClipCraftFileCopyService
import com.clipcraft.services.ClipCraftSettingsState
import com.clipcraft.util.ClipCraftNotificationCenter
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import kotlinx.coroutines.runBlocking

class ClipCraftAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return

        if (vFiles.isEmpty()) {
            ClipCraftNotificationCenter.warn("No files selected to copy.")
            return
        }

        val globalState = ClipCraftSettingsState.getInstance()
        val options = globalState.advancedOptions
        options.resolveConflicts()

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "ClipCraft Copy", true) {
            override fun run(indicator: ProgressIndicator) = runBlocking {
                ClipCraftFileCopyService().copyFiles(
                    project = project,
                    files = vFiles.toList(),
                    options = options,
                    indicator = indicator
                )
            }
        })
    }
}

```