package com.clipcraft.services

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications

/**
 * Centralized place for showing notifications in IntelliJ.
 */
object ClipCraftNotificationCenter {
    fun info(message: String) {
        Notifications.Bus.notify(
            Notification("ClipCraft", "ClipCraft Info", message, NotificationType.INFORMATION)
        )
    }

    fun warn(message: String) {
        Notifications.Bus.notify(
            Notification("ClipCraft", "ClipCraft Warning", message, NotificationType.WARNING)
        )
    }

    fun error(message: String) {
        Notifications.Bus.notify(
            Notification("ClipCraft", "ClipCraft Error", message, NotificationType.ERROR)
        )
    }
}
