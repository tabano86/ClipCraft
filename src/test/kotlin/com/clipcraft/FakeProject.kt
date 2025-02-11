package com.clipcraft

import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import kotlinx.coroutines.CoroutineScope
import java.io.File

/**
 * A minimal fake implementation of Project for testing,
 * removing the deprecated getCoroutineScope() override.
 */
class FakeProject(private val basePath: String) : Project {
    override fun getBasePath(): String = basePath
    override fun getName(): String = "FakeProject"

    override fun getBaseDir(): VirtualFile {
        TODO("Not yet implemented")
    }

    override fun isInitialized(): Boolean = true
    override fun getCoroutineScope(): CoroutineScope {
        TODO("Not yet implemented")
    }

    // If you need coroutines, provide your own scope or remove references in tests.
    // override fun getCoroutineScope(): CoroutineScope = ... (Removed as it's deprecated)

    override fun isOpen(): Boolean = true
    override fun isDisposed(): Boolean = false
    override fun getProjectFile(): VirtualFile = throw UnsupportedOperationException()
    override fun getProjectFilePath(): String = throw UnsupportedOperationException()
    override fun getWorkspaceFile(): VirtualFile = throw UnsupportedOperationException()
    override fun getLocationHash(): String = throw UnsupportedOperationException()
    override fun save() {
        throw UnsupportedOperationException()
    }

    override fun getDisposed() = throw UnsupportedOperationException()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> getService(serviceClass: Class<T>): T? = null

    override fun <T : Any?> instantiateClass(aClass: Class<T>, pluginId: PluginId): T {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> instantiateClass(
        className: String,
        pluginDescriptor: com.intellij.openapi.extensions.PluginDescriptor
    ): T & Any {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> instantiateClassWithConstructorInjection(
        aClass: Class<T>,
        key: Any,
        pluginId: PluginId
    ): T {
        TODO("Not yet implemented")
    }

    override fun createError(error: Throwable, pluginId: PluginId): RuntimeException {
        TODO("Not yet implemented")
    }

    override fun createError(message: String, pluginId: PluginId): RuntimeException {
        TODO("Not yet implemented")
    }

    override fun createError(
        message: String,
        error: Throwable?,
        pluginId: PluginId,
        attachments: MutableMap<String, String>?
    ): RuntimeException {
        throw UnsupportedOperationException()
    }

    override fun <T : Any?> loadClass(
        className: String,
        pluginDescriptor: com.intellij.openapi.extensions.PluginDescriptor
    ): Class<T> {
        TODO("Not yet implemented")
    }

    override fun getActivityCategory(isExtension: Boolean): com.intellij.diagnostic.ActivityCategory {
        throw UnsupportedOperationException()
    }

    override fun <T : Any?> getComponent(interfaceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }

    override fun hasComponent(interfaceClass: Class<*>): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isInjectionForExtensionSupported(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getMessageBus(): com.intellij.util.messages.MessageBus = throw UnsupportedOperationException()

    override fun dispose() {}
    override fun getExtensionArea(): com.intellij.openapi.extensions.ExtensionsArea =
        throw UnsupportedOperationException()

    override fun <T : Any?> getUserData(key: com.intellij.openapi.util.Key<T>): T? = null
    override fun <T : Any?> putUserData(key: com.intellij.openapi.util.Key<T>, value: T?) {}
}

open class FakeVirtualFile(
    private val filePath: String,
    private val fileContent: String,
    private val isDir: Boolean = false
) : VirtualFile() {
    override fun getName(): String = File(filePath).name
    override fun getFileSystem() = FakeVirtualFileSystem
    override fun getPath(): String = filePath
    override fun isWritable(): Boolean = false
    override fun isDirectory(): Boolean = isDir
    override fun isValid(): Boolean = true
    override fun getParent(): VirtualFile? = null
    override fun getChildren(): Array<VirtualFile> = emptyArray()
    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): java.io.OutputStream =
        throw UnsupportedOperationException("Not supported")
    override fun contentsToByteArray(): ByteArray = fileContent.toByteArray(Charsets.UTF_8)
    override fun getTimeStamp(): Long = 0L
    override fun getLength(): Long = fileContent.length.toLong()
    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}
    override fun getInputStream(): java.io.InputStream = fileContent.byteInputStream()
    override fun getExtension(): String = File(filePath).extension
}

object FakeVirtualFileSystem : VirtualFileSystem() {
    override fun getProtocol(): String = "fake"
    override fun findFileByPath(path: String): VirtualFile? = null
    override fun refreshAndFindFileByPath(path: String): VirtualFile? = null
    override fun refresh(asynchronous: Boolean) {}
    override fun addVirtualFileListener(listener: com.intellij.openapi.vfs.VirtualFileListener) {}
    override fun removeVirtualFileListener(listener: com.intellij.openapi.vfs.VirtualFileListener) {}
    override fun deleteFile(requestor: Any?, vFile: VirtualFile) = throw UnsupportedOperationException()
    override fun moveFile(requestor: Any?, vFile: VirtualFile, newParent: VirtualFile) {
        TODO("Not yet implemented")
    }

    override fun renameFile(requestor: Any?, vFile: VirtualFile, newName: String) {
        TODO("Not yet implemented")
    }

    override fun createChildFile(requestor: Any?, vDir: VirtualFile, fileName: String): VirtualFile {
        TODO("Not yet implemented")
    }

    override fun createChildDirectory(requestor: Any?, vDir: VirtualFile, dirName: String): VirtualFile {
        TODO("Not yet implemented")
    }

    override fun copyFile(
        requestor: Any?,
        virtualFile: VirtualFile,
        newParent: VirtualFile,
        copyName: String
    ): VirtualFile {
        TODO("Not yet implemented")
    }

    override fun isReadOnly(): Boolean {
        TODO("Not yet implemented")
    }
}
