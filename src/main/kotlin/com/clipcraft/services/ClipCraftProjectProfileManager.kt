package com.clipcraft.services

import com.clipcraft.model.ClipCraftProfile

class ClipCraftProfileManager {
    private val settings = ClipCraftSettings.getInstance()

    fun currentProfile(): ClipCraftProfile = settings.getCurrentProfile()

    fun switchProfile(profileName: String) {
        settings.setCurrentProfile(profileName)
    }

    fun listProfiles(): List<ClipCraftProfile> = settings.getAllProfiles()

    fun addProfile(profile: ClipCraftProfile) {
        settings.addProfile(profile)
    }

    fun deleteProfile(profileName: String) {
        settings.removeProfile(profileName)
    }
}
