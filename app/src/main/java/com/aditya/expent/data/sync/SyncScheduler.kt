package com.aditya.expent.data.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun enqueueCategorySync() {}
    fun enqueueTransactionSync() {}
    fun enqueueAccountSync() {}
    fun enqueueBudgetSync() {}
    fun enqueueExpenseSync() {}
}
