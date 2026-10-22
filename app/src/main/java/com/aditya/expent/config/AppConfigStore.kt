package com.aditya.expent.config

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.aditya.expent.BuildConfig
import com.aditya.expent.utils.AppLogger
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppConfigStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fetchMutex = Mutex()
    private val prefs: SharedPreferences by lazy { createEncryptedPrefs() }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val loadMutex = Mutex()

    @Volatile
    private var loaded = false

    @Volatile
    private var currentServerUrl: HttpUrl? = null

    @Volatile
    private var hasWorkerConfig = false

    @Volatile
    private var lastFetchAt = 0L
    private var networkCallbackRegistered = false

    fun init() {
        scope.launch {
            ensureLoaded()
            refresh()
        }
        registerNetworkCallback()
    }

    suspend fun ensureLoaded() {
        if (loaded) return
        loadMutex.withLock {
            if (loaded) return
            val cached = runCatching { prefs.getString(KEY_CONFIG_JSON, null) }.getOrNull()?.let(::parse)

            if (cached == null) runCatching { prefs.edit(commit = true) { remove(KEY_ETAG) } }
            if (cached != null) {
                apply(cached, source = "cache")
            } else {
                refresh(force = true)

                if (!hasWorkerConfig) {
                    loadBundled()?.let { apply(it, source = "assets (offline first launch)", fromWorker = false) }
                }
            }
            loaded = true
        }
    }

    fun awaitServerUrl(): HttpUrl {
        if (!loaded) runBlocking { ensureLoaded() }
        return currentServerUrl ?: throw IOException("No serverUrl available: config couldn't be loaded")
    }

    fun refreshAsync(force: Boolean = false) {
        scope.launch { refresh(force) }
    }

    suspend fun refresh(force: Boolean = false): Boolean = fetchMutex.withLock {
        if (!force && System.currentTimeMillis() - lastFetchAt < MIN_REFRESH_INTERVAL_MS) return@withLock false

        try {
            val etag = prefs.getString(KEY_ETAG, null)
            val request = Request.Builder()
                .url(CONFIG_URL)
                .apply { if (etag != null) header("If-None-Match", etag) }
                .apply { if (BuildConfig.CONFIG_API_KEY.isNotEmpty()) header(CONFIG_KEY_HEADER, BuildConfig.CONFIG_API_KEY) }
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.code == 304) {
                    lastFetchAt = System.currentTimeMillis()
                    AppLogger.debug(TAG, "Config unchanged (304)")
                    return@withLock false
                }
                if (!response.isSuccessful) {
                    AppLogger.warn(TAG, "Config fetch failed: HTTP ${response.code}")
                    return@withLock false
                }
                val body = response.body?.string().orEmpty()
                val root = parse(body)
                if (root == null) {
                    AppLogger.warn(TAG, "Config fetch returned invalid JSON, keeping current config")
                    return@withLock false
                }
                prefs.edit(commit = true) {
                    putString(KEY_CONFIG_JSON, body)
                    response.header("ETag")?.let { putString(KEY_ETAG, it) } ?: remove(KEY_ETAG)
                }
                lastFetchAt = System.currentTimeMillis()
                apply(root, source = "network")
                true
            }
        } catch (e: Exception) {
            AppLogger.warn(TAG, "Config fetch failed: ${e.message}")
            false
        }
    }

    private fun apply(root: JsonObject, source: String, fromWorker: Boolean = true) {
        if (!fromWorker && hasWorkerConfig) return
        if (fromWorker) hasWorkerConfig = true
        serverUrlOf(root)?.let { currentServerUrl = it }
        AppText.update(root.getAsJsonObject("content"))
        AppLogger.debug(TAG, "Config applied from $source (serverUrl=$currentServerUrl)")
    }

    private fun serverUrlOf(root: JsonObject): HttpUrl? = root.get("serverUrl")
        ?.takeIf { it.isJsonPrimitive }
        ?.asString
        ?.trim()
        ?.toHttpUrlOrNull()

    private fun parse(json: String): JsonObject? = runCatching {
        JsonParser.parseString(json).asJsonObject.takeIf { it.has("content") && it.get("content").isJsonObject }
    }.getOrNull()

    private fun loadBundled(): JsonObject? = runCatching {
        context.assets.open(ASSET_FILE).bufferedReader().use { parse(it.readText()) }
    }.getOrNull()

    private fun registerNetworkCallback() {
        if (networkCallbackRegistered) return
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    refreshAsync()
                }
            })
            networkCallbackRegistered = true
        }.onFailure { AppLogger.warn(TAG, "Couldn't watch connectivity: ${it.message}") }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        fun create(): SharedPreferences = EncryptedSharedPreferences.create(
            PREFS_FILE_NAME,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        return try {
            create()
        } catch (e: Exception) {
            AppLogger.error(TAG, "Failed to open encrypted config prefs, resetting", e)
            context.deleteSharedPreferences(PREFS_FILE_NAME)
            create()
        }
    }

    companion object {
        private const val TAG = "AppConfigStore"
        const val CONFIG_URL = "https://expent-config.adityaworkhbss.workers.dev/"
        private const val CONFIG_KEY_HEADER = "X-Config-Key"
        private const val ASSET_FILE = "remote_config.json"
        private const val PREFS_FILE_NAME = "expent_config_prefs"
        private const val KEY_CONFIG_JSON = "config_json"
        private const val KEY_ETAG = "config_etag"
        private const val MIN_REFRESH_INTERVAL_MS = 5 * 60 * 1000L
    }
}
