package com.clipcraft.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType

/**
 * Central location for sending notifications to the user.
 */
object ClipCraftNotificationCenter {

    fun info(message: String) {
        NotificationGroupManager.getInstance().getNotificationGroup("ClipCraft Notifications")
            .createNotification(message, NotificationType.INFORMATION)
            .notify(null)
    }

    fun warn(message: String) {
        NotificationGroupManager.getInstance().getNotificationGroup("ClipCraft Notifications")
            .createNotification(message, NotificationType.WARNING)
            .notify(null)
    }

    fun error(message: String) {
        NotificationGroupManager.getInstance().getNotificationGroup("ClipCraft Notifications")
            .createNotification(message, NotificationType.ERROR)
            .notify(null)
    }
}
