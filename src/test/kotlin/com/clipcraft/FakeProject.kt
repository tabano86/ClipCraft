package com.clipcraft

import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import java.io.File
import kotlinx.coroutines.CoroutineScope

class FakeProject(private val path: String) : Project {
    override fun getBasePath() = path
    override fun getName() = "FakeProject"
    override fun getBaseDir(): VirtualFile {
        throw UnsupportedOperationException()
    }

    override fun isInitialized() = true
    override fun getCoroutineScope(): CoroutineScope {
        throw UnsupportedOperationException()
    }

    override fun isOpen() = true
    override fun isDisposed() = false
    override fun getProjectFile(): VirtualFile {
        throw UnsupportedOperationException()
    }

    override fun getProjectFilePath() = throw UnsupportedOperationException()
    override fun getWorkspaceFile() = throw UnsupportedOperationException()
    override fun getLocationHash() = throw UnsupportedOperationException()
    override fun save() {
        throw UnsupportedOperationException()
    }

    override fun getDisposed() = throw UnsupportedOperationException()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> getService(serviceClass: Class<T>): T? = null
    override fun <T : Any?> instantiateClass(aClass: Class<T>, pluginId: PluginId): T {
        throw UnsupportedOperationException()
    }

    override fun <T : Any?> instantiateClass(
        className: String,
        pluginDescriptor: com.intellij.openapi.extensions.PluginDescriptor
    ): T & Any {
        throw UnsupportedOperationException()
    }

    override fun <T : Any?> instantiateClassWithConstructorInjection(
        aClass: Class<T>,
        key: Any,
        pluginId: PluginId
    ): T {
        throw UnsupportedOperationException()
    }

    override fun createError(error: Throwable, pluginId: PluginId) = throw UnsupportedOperationException()
    override fun createError(message: String, pluginId: PluginId) = throw UnsupportedOperationException()
    override fun createError(
        message: String,
        error: Throwable?,
        pluginId: PluginId,
        attachments: MutableMap<String, String>?
    ) = throw UnsupportedOperationException()

    override fun <T : Any?> loadClass(
        className: String,
        pluginDescriptor: com.intellij.openapi.extensions.PluginDescriptor
    ): Class<T> {
        throw UnsupportedOperationException()
    }

    override fun getActivityCategory(isExtension: Boolean) = throw UnsupportedOperationException()
    override fun <T : Any?> getComponent(interfaceClass: Class<T>) = throw UnsupportedOperationException()
    override fun hasComponent(interfaceClass: Class<*>) = throw UnsupportedOperationException()
    override fun isInjectionForExtensionSupported() = throw UnsupportedOperationException()
    override fun getMessageBus() = throw UnsupportedOperationException()
    override fun dispose() {}
    override fun getExtensionArea() = throw UnsupportedOperationException()
    override fun <T : Any?> getUserData(key: com.intellij.openapi.util.Key<T>) = null
    override fun <T : Any?> putUserData(key: com.intellij.openapi.util.Key<T>, value: T?) {}
}

open class FakeVirtualFile(
    private val filePath: String,
    private val fileContent: String,
    private val isDir: Boolean = false
) : VirtualFile() {
    override fun getName() = File(filePath).name
    override fun getFileSystem() = FakeVirtualFileSystem
    override fun getPath() = filePath
    override fun isWritable() = false
    override fun isDirectory() = isDir
    override fun isValid() = true
    override fun getParent(): VirtualFile? = null
    override fun getChildren() = emptyArray<VirtualFile>()
    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long) =
        throw UnsupportedOperationException()

    override fun contentsToByteArray() = fileContent.toByteArray(Charsets.UTF_8)
    override fun getTimeStamp() = 0L
    override fun getLength() = fileContent.length.toLong()
    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}
    override fun getInputStream() = fileContent.byteInputStream()
    override fun getExtension() = File(filePath).extension
}

object FakeVirtualFileSystem : VirtualFileSystem() {
    override fun getProtocol() = "fake"
    override fun findFileByPath(path: String) = null
    override fun refreshAndFindFileByPath(path: String) = null
    override fun refresh(asynchronous: Boolean) {}
    override fun addVirtualFileListener(listener: com.intellij.openapi.vfs.VirtualFileListener) {}
    override fun removeVirtualFileListener(listener: com.intellij.openapi.vfs.VirtualFileListener) {}
    override fun deleteFile(requestor: Any?, vFile: VirtualFile) = throw UnsupportedOperationException()
    override fun moveFile(requestor: Any?, vFile: VirtualFile, newParent: VirtualFile) =
        throw UnsupportedOperationException()

    override fun renameFile(requestor: Any?, vFile: VirtualFile, newName: String) =
        throw UnsupportedOperationException()

    override fun createChildFile(requestor: Any?, vDir: VirtualFile, fileName: String) =
        throw UnsupportedOperationException()

    override fun createChildDirectory(requestor: Any?, vDir: VirtualFile, dirName: String) =
        throw UnsupportedOperationException()

    override fun copyFile(requestor: Any?, virtualFile: VirtualFile, newParent: VirtualFile, copyName: String) =
        throw UnsupportedOperationException()

    override fun isReadOnly() = true
}
