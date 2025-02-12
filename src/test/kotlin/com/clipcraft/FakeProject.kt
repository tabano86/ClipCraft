package com.clipcraft

import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope

class FakeProject(private val path: String) : Project {
    override fun getBasePath() = path
    override fun getName() = "FakeProject"
    override fun getBaseDir(): VirtualFile = throw UnsupportedOperationException()
    override fun isInitialized() = true
    override fun getCoroutineScope(): CoroutineScope = throw UnsupportedOperationException()
    override fun isOpen() = true
    override fun isDisposed() = false
    override fun getProjectFile(): VirtualFile = throw UnsupportedOperationException()
    override fun getProjectFilePath() = throw UnsupportedOperationException()
    override fun getWorkspaceFile() = throw UnsupportedOperationException()
    override fun getLocationHash() = throw UnsupportedOperationException()
    override fun save() = throw UnsupportedOperationException()
    override fun getDisposed() = throw UnsupportedOperationException()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> getService(serviceClass: Class<T>): T? = null
    override fun <T : Any?> instantiateClass(aClass: Class<T>, pluginId: PluginId): T =
        throw UnsupportedOperationException()

    override fun <T : Any?> instantiateClass(
        className: String,
        pluginDescriptor: com.intellij.openapi.extensions.PluginDescriptor,
    ): T & Any = throw UnsupportedOperationException()

    override fun <T : Any?> instantiateClassWithConstructorInjection(
        aClass: Class<T>,
        key: Any,
        pluginId: PluginId,
    ): T = throw UnsupportedOperationException()

    override fun createError(error: Throwable, pluginId: PluginId) = throw UnsupportedOperationException()
    override fun createError(message: String, pluginId: PluginId) = throw UnsupportedOperationException()
    override fun createError(
        message: String,
        error: Throwable?,
        pluginId: PluginId,
        attachments: MutableMap<String, String>?,
    ): RuntimeException = throw UnsupportedOperationException()

    override fun <T : Any?> loadClass(
        className: String,
        pluginDescriptor: com.intellij.openapi.extensions.PluginDescriptor,
    ): Class<T> = throw UnsupportedOperationException()

    override fun getActivityCategory(isExtension: Boolean) = throw UnsupportedOperationException()
    override fun <T : Any?> getComponent(interfaceClass: Class<T>): T = throw UnsupportedOperationException()
    override fun hasComponent(interfaceClass: Class<*>): Boolean = throw UnsupportedOperationException()
    override fun isInjectionForExtensionSupported() = throw UnsupportedOperationException()
    override fun getMessageBus() = throw UnsupportedOperationException()
    override fun dispose() {}
    override fun getExtensionArea() = throw UnsupportedOperationException()
    override fun <T : Any?> getUserData(key: com.intellij.openapi.util.Key<T>) = null
    override fun <T : Any?> putUserData(key: com.intellij.openapi.util.Key<T>, value: T?) {}
}
