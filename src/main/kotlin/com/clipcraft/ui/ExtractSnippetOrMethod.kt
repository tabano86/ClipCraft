package com.clipcraft.ui

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UastFacade

fun Editor.extractSnippetOrMethod(psiFile: PsiFile): String {
    selectionModel.selectedText?.let { return it }
    val elem = psiFile.findElementAt(caretModel.offset) ?: return ""
    val method = UastFacade.convertElementWithParent(elem, UMethod::class.java) as? UMethod
    return method?.sourcePsi?.text.orEmpty()
}
