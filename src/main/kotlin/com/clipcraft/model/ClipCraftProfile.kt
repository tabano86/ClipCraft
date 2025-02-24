package com.clipcraft.model

import kotlinx.serialization.Serializable

@Serializable
data class ClipCraftProfile(var profileName: String, val options: ClipCraftOptions)
