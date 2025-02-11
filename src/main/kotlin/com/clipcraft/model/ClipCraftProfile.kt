package com.clipcraft.model

data class ClipCraftProfile(
    val profileName: String,
    val options: ClipCraftOptions = ClipCraftOptions()
)
