package com.aditya.expent.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun scheduleInitialSync() {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints)
            .setInputData(workDataOf(KEY_FULL_REFRESH to true))
            .build()

        // KEEP: replacing would cancel an in-flight POST and the item would be sent again on retry
        workManager.enqueueUniqueWork(
            INITIAL_SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            syncRequest
        )
    }

    fun schedulePeriodicSync() {
        val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(networkConstraints)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .setInputData(workDataOf(KEY_FULL_REFRESH to true))
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    // Every push shares one unique queue: each SyncWorker drains the whole pending table,
    // so parallel workers would POST the same items twice (e.g. categories again when accounts are pushed).
    fun enqueueCategorySync() = triggerPushSync()
    fun enqueueTransactionSync() = triggerPushSync()
    fun enqueueAccountSync() = triggerPushSync()
    fun enqueueBudgetSync() = triggerPushSync()
    fun enqueueExpenseSync() = triggerPushSync()
    fun enqueueCustomizationSync() = triggerPushSync()
    fun enqueueOnboardingSync() = triggerPushSync()
    fun enqueuePendingSync() = triggerPushSync()

    private fun triggerPushSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints)
            .setInputData(workDataOf(KEY_FULL_REFRESH to false))
            .build()

        // APPEND_OR_REPLACE runs after the in-flight push instead of cancelling it mid-request
        workManager.enqueueUniqueWork(
            PUSH_SYNC_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    fun cancelAllSync() {
        workManager.cancelUniqueWork(INITIAL_SYNC_WORK_NAME)
        workManager.cancelUniqueWork(PUSH_SYNC_WORK_NAME)
    }

    companion object {
        const val KEY_FULL_REFRESH = "key_full_refresh"
        const val INITIAL_SYNC_WORK_NAME = "expent_initial_sync"
        const val PERIODIC_SYNC_WORK_NAME = "expent_periodic_sync"
        const val PUSH_SYNC_WORK_NAME = "expent_push_sync"
    }
}
