package com.clipcraft.listeners

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.registry.Registry

/**
 * Shows a helpful welcome notification when ClipCraft is first installed
 */
class ClipCraftStartupActivity : StartupActivity {

    override fun runActivity(project: Project) {
        val shownBefore = Registry.`is`("clipcraft.welcome.shown", false)

        if (!shownBefore) {
            showWelcomeNotification(project)
            Registry.get("clipcraft.welcome.shown").setValue(true)
        }
    }

    private fun showWelcomeNotification(project: Project) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("ClipCraft notifications")
            .createNotification(
                "Welcome to ClipCraft!",
                """
                🎉 Thanks for installing ClipCraft!
                
                <b>⚡ Quick Start:</b>
                • Press <b>Ctrl+Alt+C</b> to instantly copy any file for AI
                • Press <b>Ctrl+Alt+Shift+C</b> to copy your entire project
                • Right-click any file → ClipCraft for more options
                
                No configuration needed - it just works! 🚀
                """.trimIndent(),
                NotificationType.INFORMATION,
            )

        notification.addAction(object : NotificationAction("View All Shortcuts") {
            override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                val shortcutsNotification = NotificationGroupManager.getInstance()
                    .getNotificationGroup("ClipCraft notifications")
                    .createNotification(
                        "ClipCraft Keyboard Shortcuts",
                        """
                        <b>⚡ Simple (No Dialogs):</b>
                        • Ctrl+Alt+C - Copy file for AI
                        • Ctrl+Alt+Shift+C - Copy project for AI
                        
                        <b>Standard Actions:</b>
                        • Ctrl+Alt+M - Copy as Markdown
                        • Ctrl+Alt+E - Export current file
                        • Ctrl+Alt+Shift+E - Export project
                        • Ctrl+Alt+P - Export with preset
                        • Ctrl+Alt+F - Export to file
                        • Ctrl+Alt+Z - Export as ZIP
                        """.trimIndent(),
                        NotificationType.INFORMATION,
                    )
                shortcutsNotification.notify(project)
                notification.expire()
            }
        })

        notification.addAction(object : NotificationAction("Got it!") {
            override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                notification.expire()
            }
        })

        notification.notify(project)
    }
}
