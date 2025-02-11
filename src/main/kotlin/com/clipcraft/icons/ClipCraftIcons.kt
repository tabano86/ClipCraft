package com.clipcraft.icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object ClipCraftIcons {

    @JvmField
    val CLIPCRAFT_16: Icon =
        IconLoader.getIcon("/icons/clipcraft_16.svg", ClipCraftIcons::class.java)

    @JvmField
    val CLIPCRAFT_24: Icon =
        IconLoader.getIcon("/icons/clipcraft_24.svg", ClipCraftIcons::class.java)

    @JvmField
    val CLIPCRAFT_32: Icon =
        IconLoader.getIcon("/icons/clipcraft_32.svg", ClipCraftIcons::class.java)

    /**
     * For convenience, we can define a default icon to use if no size specified.
     * Usually 16×16 is standard for toolbar actions.
     */
    @JvmField
    val CLIPCRAFT_DEFAULT: Icon = CLIPCRAFT_16
}
