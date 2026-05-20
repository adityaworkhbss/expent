package com.aditya.expent.utils

import android.content.Context
import android.content.SharedPreferences
import com.aditya.expent.domain.model.User
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("expent_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveUser(user: User) {
        prefs.edit().putString("user_data", gson.toJson(user)).apply()
    }

    fun getUser(): User? {
        val json = prefs.getString("user_data", null)
        return if (json != null) gson.fromJson(json, User::class.java) else null
    }

    fun saveCustomization(customization: com.aditya.expent.data.remote.dto.UserCustomizationResponseDto) {
        prefs.edit().putString("user_customization", gson.toJson(customization)).apply()
    }

    fun getCustomization(): com.aditya.expent.data.remote.dto.UserCustomizationResponseDto? {
        val json = prefs.getString("user_customization", null)
        return if (json != null) gson.fromJson(json, com.aditya.expent.data.remote.dto.UserCustomizationResponseDto::class.java) else null
    }

    fun setOnboardingComplete(complete: Boolean) {
        prefs.edit().putBoolean("onboarding_complete", complete).apply()
    }

    fun isOnboardingComplete(): Boolean {
        return prefs.getBoolean("onboarding_complete", false)
    }

    fun getOnboardingStep(): Int {
        return prefs.getInt("onboarding_step", 0)
    }

    fun setOnboardingStep(step: Int) {
        prefs.edit().putInt("onboarding_step", step).apply()
    }

    fun clearSession() {
        prefs.edit().remove("user_data").apply()
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}