package com.clipcraft.services

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import org.slf4j.LoggerFactory

object ClipCraftNotificationCenter {

    private val log = LoggerFactory.getLogger(ClipCraftNotificationCenter::class.java)

    fun notifyInfo(message: String, project: Project?) {
        Notifications.Bus.notify(Notification("ClipCraft", "ClipCraft", message, NotificationType.INFORMATION), project)
        log.info("ClipCraft INFO: $message")
    }

    fun notifyError(message: String, project: Project?) {
        Notifications.Bus.notify(Notification("ClipCraft", "ClipCraft Error", message, NotificationType.ERROR), project)
        log.error("ClipCraft ERROR: $message")
    }
}
