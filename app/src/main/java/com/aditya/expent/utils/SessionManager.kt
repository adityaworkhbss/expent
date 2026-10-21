package com.aditya.expent.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
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
        private const val PREFS_FILE_NAME = "expent_prefs"
        private const val KEY_LAST_SYNC_TIME = "key_last_sync_time"
    }

    private val prefs: SharedPreferences = createEncryptedPrefs()

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
            AppLogger.setUser(user.id)

        } catch (e: Exception) {

            AppLogger.error(TAG, "Failed to save user", e)
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

            AppLogger.error(TAG, "Failed to parse user", e)

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

            AppLogger.error(TAG, "Failed to save customization", e)
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

            AppLogger.error(TAG, "Failed to parse customization", e)

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
        val step = getOnboardingStep()
        Log.d(TAG, "isOnboardingComplete() = $value, step = $step")
        return value || step >= 4
    }

    fun getOnboardingStep(): Int {
        val step = prefs.getInt("onboarding_step", 0)
        Log.d(TAG, "getOnboardingStep() = $step")
        return step
    }

    fun setOnboardingStep(step: Int) {
        Log.d(TAG, "setOnboardingStep() called with = $step")

        val updatedUserJson = getUser()?.let { gson.toJson(it.copy(onboardingStep = step)) }
        prefs.edit(commit = true) {
            putInt("onboarding_step", step)
            if (updatedUserJson != null) {
                putString("user_data", updatedUserJson)
            }
            if (step >= 4) {
                putBoolean("onboarding_complete", true)
            }
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
        AppLogger.setUser(null)
    }

    fun logout() {

        Log.e(TAG, "logout() called")
        Log.e(TAG, "Clearing ALL preferences")

        prefs.edit(commit = true) {
            clear()
        }

        Log.d(TAG, "All preferences cleared successfully")
        AppLogger.setUser(null)
    }

    fun printAllPrefs() {

        Log.d(TAG, "ALL PREFS = ${prefs.all}")
    }

    fun getLastSyncTime(): Long = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)

    fun setLastSyncTime(time: Long) {
        prefs.edit { putLong(KEY_LAST_SYNC_TIME, time) }
    }

    fun markReminderDone(sourceId: String, cycle: String) {
        prefs.edit { putBoolean("reminder_done_${sourceId}_$cycle", true) }
    }

    fun isReminderDone(sourceId: String, cycle: String): Boolean =
        prefs.getBoolean("reminder_done_${sourceId}_$cycle", false)

    fun saveFcmToken(token: String) {
        prefs.edit(commit = true) {
            putString("fcm_token", token)
        }
    }

    fun getFcmToken(): String? {
        return prefs.getString("fcm_token", null)
    }

    fun clearFcmToken() {
        prefs.edit(commit = true) {
            remove("fcm_token")
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_FILE_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            AppLogger.error(TAG, "Failed to create EncryptedSharedPreferences, clearing and retrying", e)

            context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).edit(commit = true) { clear() }
            try {
                context.deleteSharedPreferences(PREFS_FILE_NAME)
            } catch (_: Exception) { }
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_FILE_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}