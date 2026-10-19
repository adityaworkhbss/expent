package com.aditya.expent.config

import android.content.Context
import com.aditya.expent.utils.AppLogger
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class RemoteConfigManager private constructor(private val context: Context) {

    private val firebaseRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
    private val gson = Gson()
    private var remoteConfig: RemoteConfig? = null
    private var isInitialized = false

    companion object {
        @Volatile private var INSTANCE: RemoteConfigManager? = null

        fun getInstance(context: Context): RemoteConfigManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RemoteConfigManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .setFetchTimeoutInSeconds(60)
                .build()

            firebaseRemoteConfig.setConfigSettingsAsync(configSettings)

            val defaultsMap = mapOf(
                "remote_config_json" to loadDefaultConfigFromAssets()
            )
            firebaseRemoteConfig.setDefaultsAsync(defaultsMap)

            val fetchResult = firebaseRemoteConfig.fetchAndActivate().await()

            if (fetchResult) {
                AppLogger.error("RemoteConfig", "RemoteConfig fetched and activated successfully")
            } else {
                AppLogger.error("RemoteConfig", "RemoteConfig fetch failed, using defaults")
            }

            parseRemoteConfig()
            isInitialized = true
            true
        } catch (e: Exception) {
            AppLogger.error("RemoteConfig", "Failed to initialize RemoteConfig", e)
            parseRemoteConfig()
            isInitialized = true
            false
        }
    }

    private fun loadDefaultConfigFromAssets(): String {
        return try {
            context.assets.open("remote_config.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            AppLogger.error("RemoteConfig", "Failed to load default config from assets", e)
            "{}"
        }
    }

    private fun parseRemoteConfig() {
        try {
            val jsonString = firebaseRemoteConfig.getString("remote_config_json")
            if (jsonString.isNotBlank()) {
                remoteConfig = gson.fromJson(jsonString, RemoteConfig::class.java)
                AppLogger.error("RemoteConfig", "RemoteConfig parsed successfully")
            }
        } catch (e: Exception) {
            AppLogger.error("RemoteConfig", "Failed to parse RemoteConfig JSON", e)
            remoteConfig = gson.fromJson(loadDefaultConfigFromAssets(), RemoteConfig::class.java)
        }
    }

    fun getConfig(): RemoteConfig {
        return remoteConfig ?: gson.fromJson(loadDefaultConfigFromAssets(), RemoteConfig::class.java)
    }

    fun isConfigLoaded(): Boolean = isInitialized && remoteConfig != null

    fun forceFetch(): Boolean {
        try {
            firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    if (updated) {
                        parseRemoteConfig()
                        AppLogger.error("RemoteConfig", "RemoteConfig force fetch successful")
                    }
                } else {
                    AppLogger.error("RemoteConfig", "RemoteConfig force fetch failed", task.exception)
                }
            }
            return true
        } catch (e: Exception) {
            AppLogger.error("RemoteConfig", "Force fetch error", e)
            return false
        }
    }

    fun getString(key: String): String {
        return firebaseRemoteConfig.getString(key)
    }

    fun getLong(key: String): Long {
        return firebaseRemoteConfig.getLong(key)
    }

    fun getDouble(key: String): Double {
        return firebaseRemoteConfig.getDouble(key)
    }

    fun getBoolean(key: String): Boolean {
        return firebaseRemoteConfig.getBoolean(key)
    }

    fun <T> getObject(key: String, type: Class<T>): T? {
        val json = firebaseRemoteConfig.getString(key)
        return if (json.isNotBlank()) {
            try {
                gson.fromJson(json, type)
            } catch (e: Exception) {
                AppLogger.error("RemoteConfig", "Failed to parse object for key: $key", e)
                null
            }
        } else null
    }

    fun <T> getList(key: String, type: TypeToken<List<T>>): List<T> {
        val json = firebaseRemoteConfig.getString(key)
        return if (json.isNotBlank()) {
            try {
                gson.fromJson(json, type.type)
            } catch (e: Exception) {
                AppLogger.error("RemoteConfig", "Failed to parse list for key: $key", e)
                emptyList()
            }
        } else emptyList()
    }
}