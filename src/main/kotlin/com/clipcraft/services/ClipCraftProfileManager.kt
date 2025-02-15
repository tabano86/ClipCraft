package com.clipcraft.services

import com.clipcraft.model.ClipCraftProfile

class ClipCraftProfileManager {
    private val settings = ClipCraftSettings.getInstance()

    fun currentProfile(): ClipCraftProfile = settings.getCurrentProfile()
    fun switchProfile(name: String) = settings.setCurrentProfile(name)
    fun listProfiles(): List<ClipCraftProfile> = settings.getAllProfiles()
    fun addProfile(profile: ClipCraftProfile) = settings.addProfile(profile)
    fun deleteProfile(name: String) = settings.removeProfile(name)
    fun getProfile(name: String): ClipCraftProfile? = settings.getAllProfiles().find { it.profileName == name }
}
