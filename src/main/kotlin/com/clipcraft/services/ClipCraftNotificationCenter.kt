package com.clipcraft.services

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications

object ClipCraftNotificationCenter {
    fun info(message: String) {
        Notifications.Bus.notify(
            Notification("ClipCraft", "ClipCraft Info", message, NotificationType.INFORMATION)
        )
    }

    fun error(message: String) {
        Notifications.Bus.notify(
            Notification("ClipCraft", "ClipCraft Error", message, NotificationType.ERROR)
        )
    }
}
