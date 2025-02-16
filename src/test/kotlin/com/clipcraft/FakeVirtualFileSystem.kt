package com.clipcraft

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileSystem

object FakeVirtualFileSystem : VirtualFileSystem() {
    override fun getProtocol() = "fake"
    override fun findFileByPath(path: String): VirtualFile? = null
    override fun refreshAndFindFileByPath(path: String): VirtualFile? = null
    override fun refresh(asynchronous: Boolean) {}
    override fun addVirtualFileListener(listener: VirtualFileListener) {}
    override fun removeVirtualFileListener(listener: VirtualFileListener) {}
    override fun deleteFile(requestor: Any?, vFile: VirtualFile) =
        throw UnsupportedOperationException()

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
