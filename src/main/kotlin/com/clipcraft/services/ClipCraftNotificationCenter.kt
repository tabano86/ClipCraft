package com.clipcraft.services

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project

object ClipCraftNotificationCenter {
    fun notifyInfo(message: String, project: Project?) {
        val notification = Notification("ClipCraft", "ClipCraft", message, NotificationType.INFORMATION)
        Notifications.Bus.notify(notification, project)
    }

    fun notifyError(message: String, project: Project?) {
        val notification = Notification("ClipCraft", "ClipCraft Error", message, NotificationType.ERROR)
        Notifications.Bus.notify(notification, project)
    }
}
