package com.clipcraft

import com.intellij.openapi.vfs.VirtualFile
import java.io.File

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
