package com.clipcraft.services

import com.clipcraft.model.ClipCraftOptions
import com.clipcraft.model.ClipCraftProfile

class ClipCraftSettings private constructor() {
    private val fallback = ClipCraftProfile("Global Default", ClipCraftOptions())
    private val profiles = mutableListOf<ClipCraftProfile>()
    private var currentName: String

    companion object {
        private val instance = ClipCraftSettings()

        @JvmStatic
        fun getInstance(): ClipCraftSettings = instance
    }

    init {
        profiles += fallback
        currentName = fallback.profileName
    }

    fun getCurrentProfile() = profiles.find { it.profileName == currentName } ?: fallback
    fun setCurrentProfile(name: String) {
        if (profiles.any { it.profileName == name }) currentName = name
    }

    fun getAllProfiles() = profiles.toList()
    fun addProfile(profile: ClipCraftProfile) {
        val idx = profiles.indexOfFirst { it.profileName == profile.profileName }
        if (idx >= 0) profiles[idx] = profile else profiles += profile
        if (getCurrentProfile() == fallback && currentName == fallback.profileName) currentName = profile.profileName
    }

    fun removeProfile(name: String) {
        if (name == fallback.profileName) return
        if (profiles.removeIf { it.profileName == name } && currentName == name) currentName = fallback.profileName
    }

    fun getSnippetPrefix() = getCurrentProfile().options.snippetHeaderText ?: "/* Default Header */"
    fun getSnippetSuffix() = getCurrentProfile().options.snippetFooterText ?: "/* Default Footer */"
}
