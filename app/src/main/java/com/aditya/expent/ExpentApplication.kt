package com.aditya.expent

import android.app.Application
import com.aditya.expent.data.sync.SyncScheduler
import com.aditya.expent.domain.usecase.RegisterDeviceTokenUseCase
import com.aditya.expent.presentation.widget.AddTransactionWidget
import com.aditya.expent.utils.AppLogger
import com.aditya.expent.utils.SessionManager
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ExpentApplication : Application() {

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var registerDeviceTokenUseCase: RegisterDeviceTokenUseCase

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val user = sessionManager.getUser()
        AppLogger.setUser(user?.id)
        if (user != null) {
            if (sessionManager.isOnboardingComplete()) {
                syncScheduler.schedulePeriodicSync()
                syncScheduler.scheduleInitialSync()
            } else {
                syncScheduler.enqueuePendingSync()
            }
        }
        initFcmToken()
        AddTransactionWidget.updateAll(this)
    }

    private fun initFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                AppLogger.warn("ExpentApp", "Fetching FCM registration token failed: ${task.exception?.message}")
                return@addOnCompleteListener
            }

            val token = task.result
            if (!token.isNullOrBlank()) {
                AppLogger.debug("ExpentApp", "FCM Device Token retrieved: $token")
                sessionManager.saveFcmToken(token)

                val user = sessionManager.getUser()
                if (user != null && user.id.isNotBlank()) {
                    applicationScope.launch {
                        try {
                            registerDeviceTokenUseCase(user.id, token)
                            AppLogger.debug("ExpentApp", "Device token registered on app start")
                        } catch (e: Exception) {
                            AppLogger.error("ExpentApp", "Failed to register device token on start", e)
                        }
                    }
                }
            }
        }
    }
}
