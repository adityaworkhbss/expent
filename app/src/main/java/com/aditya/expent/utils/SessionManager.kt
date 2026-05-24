package com.aditya.expent.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.aditya.expent.data.remote.dto.UserCustomizationResponseDto
import com.aditya.expent.domain.model.User
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "SessionManager"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("expent_prefs", Context.MODE_PRIVATE)

    private val gson = Gson()

    fun saveUser(user: User) {

        try {

            val json = gson.toJson(user)

            Log.d(TAG, "saveUser() called")
            Log.d(TAG, "Saving user json = $json")

            prefs.edit(commit = true) {
                putString("user_data", json)
            }

            Log.d(TAG, "User saved successfully")

        } catch (e: Exception) {

            Log.e(TAG, "Failed to save user", e)
        }
    }

    fun getUser(): User? {

        return try {

            Log.d(TAG, "getUser() called")

            val json = prefs.getString("user_data", null)

            Log.d(TAG, "Retrieved user json = $json")

            if (json.isNullOrBlank()) {

                Log.w(TAG, "User json is null or blank")

                null

            } else {

                val user = gson.fromJson(json, User::class.java)

                Log.d(TAG, "Parsed user = $user")

                user
            }

        } catch (e: Exception) {

            Log.e(TAG, "Failed to parse user", e)

            null
        }
    }

    fun saveCustomization(customization: UserCustomizationResponseDto) {

        try {

            val json = gson.toJson(customization)

            Log.d(TAG, "saveCustomization() called")
            Log.d(TAG, "Saving customization = $json")

            prefs.edit(commit = true) {
                putString("user_customization", json)
            }

            Log.d(TAG, "Customization saved successfully")

        } catch (e: Exception) {

            Log.e(TAG, "Failed to save customization", e)
        }
    }

    fun getCustomization(): UserCustomizationResponseDto? {

        return try {

            Log.d(TAG, "getCustomization() called")

            val json = prefs.getString("user_customization", null)

            Log.d(TAG, "Retrieved customization json = $json")

            if (json.isNullOrBlank()) {

                Log.w(TAG, "Customization json is null or blank")

                null

            } else {

                val customization =
                    gson.fromJson(json, UserCustomizationResponseDto::class.java)

                Log.d(TAG, "Parsed customization = $customization")

                customization
            }

        } catch (e: Exception) {

            Log.e(TAG, "Failed to parse customization", e)

            null
        }
    }

    fun setOnboardingComplete(complete: Boolean) {

        Log.d(TAG, "setOnboardingComplete() called with = $complete")

        prefs.edit(commit = true) {
            putBoolean("onboarding_complete", complete)
        }

        Log.d(TAG, "Onboarding complete flag saved")
    }

    fun isOnboardingComplete(): Boolean {

        val value = prefs.getBoolean("onboarding_complete", false)

        Log.d(TAG, "isOnboardingComplete() = $value")

        return value
    }

    fun getOnboardingStep(): Int {

        val step = prefs.getInt("onboarding_step", 0)

        Log.d(TAG, "getOnboardingStep() = $step")

        return step
    }

    fun setOnboardingStep(step: Int) {

        Log.d(TAG, "setOnboardingStep() called with = $step")

        prefs.edit(commit = true) {
            putInt("onboarding_step", step)
        }

        Log.d(TAG, "Onboarding step saved")
    }

    fun clearSession() {

        Log.e(TAG, "clearSession() called")
        Log.e(TAG, "Removing user_data from prefs")

        prefs.edit(commit = true) {
            remove("user_data")
        }

        Log.d(TAG, "Session cleared successfully")
    }

    fun logout() {

        Log.e(TAG, "logout() called")
        Log.e(TAG, "Clearing ALL preferences")

        prefs.edit(commit = true) {
            clear()
        }

        Log.d(TAG, "All preferences cleared successfully")
    }

    fun printAllPrefs() {

        Log.d(TAG, "ALL PREFS = ${prefs.all}")
    }
}