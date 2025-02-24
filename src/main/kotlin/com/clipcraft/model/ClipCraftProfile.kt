package com.clipcraft.model

import kotlinx.serialization.Serializable

/**
 * A named profile that holds a set of options (ClipCraftOptions).
 * For advanced usage, each project can have multiple named profiles.
 */
@Serializable
data class ClipCraftProfile(
    var profileName: String,
    val options: ClipCraftOptions,
)
