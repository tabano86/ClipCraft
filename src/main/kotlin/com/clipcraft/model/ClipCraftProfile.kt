package com.clipcraft.model

/**
 * A named profile that holds a set of ClipCraftOptions.
 */
data class ClipCraftProfile(
    val profileName: String,
    val options: ClipCraftOptions = ClipCraftOptions()
)
