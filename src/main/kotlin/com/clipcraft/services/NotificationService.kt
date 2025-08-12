package com.clipcraft.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object NotificationService {
    private const val GROUP_ID = "ClipCraft notifications"

    fun showSuccess(project: Project, content: String) =
        show(project, content, NotificationType.INFORMATION)

    fun showWarning(project: Project, content: String) =
        show(project, content, NotificationType.WARNING)

    private fun show(project: Project, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(content, type)
            .notify(project)
    }
}