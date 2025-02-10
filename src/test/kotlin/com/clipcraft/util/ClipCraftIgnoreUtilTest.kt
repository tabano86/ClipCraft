package com.clipcraft.util

import com.clipcraft.model.ClipCraftOptions
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.io.InputStream
import java.io.OutputStream

// Minimal fake implementations.

class FakeProject(private val basePath: String) : Project {
 override fun getBasePath(): String? = basePath
 override fun getName(): String = "FakeProject"
 override fun isInitialized(): Boolean = true
 override fun isOpen(): Boolean = true
 override fun isDisposed(): Boolean = false
 // Unused members.
 override fun getProjectFile(): VirtualFile? = throw UnsupportedOperationException()
 override fun getProjectFilePath(): String? = throw UnsupportedOperationException()
 override fun getWorkspaceFile(): VirtualFile? = throw UnsupportedOperationException()
 override fun getLocationHash(): String = throw UnsupportedOperationException()
 override fun save() { throw UnsupportedOperationException() }
 override fun getCoroutineScope() = throw UnsupportedOperationException()
 override fun getDisposed() = throw UnsupportedOperationException()
 override fun <T : Any?> getService(serviceClass: Class<T>): T = throw UnsupportedOperationException()
 override fun <T : Any?> instantiateClass(className: String, pluginDescriptor: com.intellij.openapi.extensions.PluginDescriptor): T = throw UnsupportedOperationException()
 override fun <T : Any?> instantiateClassWithConstructorInjection(aClass: Class<T>, key: Any, pluginId: com.intellij.openapi.extensions.PluginId): T = throw UnsupportedOperationException()
 override fun createError(error: Throwable, pluginId: com.intellij.openapi.extensions.PluginId): RuntimeException = throw UnsupportedOperationException()
 override fun createError(message: String, pluginId: com.intellij.openapi.extensions.PluginId): RuntimeException = throw UnsupportedOperationException()
 override fun createError(message: String, error: Throwable?, pluginId: com.intellij.openapi.extensions.PluginId, attachments: MutableMap<String, String>?): RuntimeException = throw UnsupportedOperationException()
 override fun <T : Any?> loadClass(className: String, pluginDescriptor: com.intellij.openapi.extensions.PluginDescriptor): Class<T> = throw UnsupportedOperationException()
 override fun getActivityCategory(isExtension: Boolean): com.intellij.diagnostic.ActivityCategory = throw UnsupportedOperationException()
 override fun getBaseDir(): VirtualFile? = null
 override fun getComponent(name: String): BaseComponent = throw UnsupportedOperationException()
 override fun <T : Any?> getComponent(interfaceClass: Class<T>): T = throw UnsupportedOperationException()
 override fun hasComponent(interfaceClass: Class<*>): Boolean = throw UnsupportedOperationException()
 override fun getPicoContainer(): org.picocontainer.PicoContainer = throw UnsupportedOperationException()
 override fun isInjectionForExtensionSupported(): Boolean = throw UnsupportedOperationException()
 override fun getMessageBus(): com.intellij.util.messages.MessageBus = throw UnsupportedOperationException()
 override fun dispose() {}
 override fun getExtensionArea(): com.intellij.openapi.extensions.ExtensionsArea = throw UnsupportedOperationException()
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
 override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream =
  throw UnsupportedOperationException("Not supported")
 override fun contentsToByteArray(): ByteArray = fileContent.toByteArray(Charsets.UTF_8)
 override fun getTimeStamp(): Long = 0L
 override fun getLength(): Long = fileContent.length.toLong()
 override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}
 override fun getInputStream(): InputStream = fileContent.byteInputStream()
 override fun getExtension(): String = File(filePath).extension
}

object FakeVirtualFileSystem : com.intellij.openapi.vfs.VirtualFileSystem() {
 override fun getProtocol(): String = "fake"
 override fun findFileByPath(path: String): VirtualFile? = null
 override fun refreshAndFindFileByPath(path: String): VirtualFile? = null
 override fun refresh(asynchronous: Boolean) {}
 override fun addVirtualFileListener(listener: com.intellij.openapi.vfs.VirtualFileListener) {}
 override fun removeVirtualFileListener(listener: com.intellij.openapi.vfs.VirtualFileListener) {}
 override fun deleteFile(requestor: Any?, vFile: VirtualFile) = throw UnsupportedOperationException()
 override fun moveFile(requestor: Any?, vFile: VirtualFile, newParent: VirtualFile) = throw UnsupportedOperationException()
 override fun renameFile(requestor: Any?, vFile: VirtualFile, newName: String) = throw UnsupportedOperationException()
 override fun createChildFile(requestor: Any?, vDir: VirtualFile, fileName: String): VirtualFile = throw UnsupportedOperationException()
 override fun createChildDirectory(requestor: Any?, vDir: VirtualFile, dirName: String): VirtualFile = throw UnsupportedOperationException()
 override fun copyFile(requestor: Any?, virtualFile: VirtualFile, newParent: VirtualFile, copyName: String): VirtualFile = throw UnsupportedOperationException()
 override fun isReadOnly(): Boolean = true
}

class ClipCraftIgnoreUtilTest {

 @Test
 fun `negative pattern overrides positive match`() {
  val opts = ClipCraftOptions(ignorePatterns = listOf(".*\\.txt", "!important\\.txt"))
  val file = FakeVirtualFile("/project/important.txt", "data", isDir = false)
  val project = FakeProject("/project")
  assertFalse(ClipCraftIgnoreUtil.shouldIgnore(file, opts, project))
 }

 @Test
 fun `ignore based on gitignore when useGitIgnore is enabled`() {
  // Create a temporary project directory.
  val tempProjectDir = createTempDir()
  // Write a .gitignore file with patterns: ignore the "secret/" folder and "*.bak" files.
  val gitIgnore = File(tempProjectDir, ".gitignore")
  gitIgnore.writeText("secret/\n*.bak")
  val opts = ClipCraftOptions(useGitIgnore = true)
  val project = FakeProject(tempProjectDir.absolutePath)

  // A file inside "secret" should be ignored.
  val secretFile = FakeVirtualFile(
   File(tempProjectDir, "secret/info.txt").absolutePath, "data"
  )
  assertTrue(ClipCraftIgnoreUtil.shouldIgnore(secretFile, opts, project))

  // A file with .bak extension should be ignored.
  val bakFile = FakeVirtualFile(
   File(tempProjectDir, "backup.bak").absolutePath, "data"
  )
  assertTrue(ClipCraftIgnoreUtil.shouldIgnore(bakFile, opts, project))
 }
}
