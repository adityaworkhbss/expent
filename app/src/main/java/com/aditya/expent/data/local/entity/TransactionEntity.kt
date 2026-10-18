package com.aditya.expent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One actual income or expense. [date] is an ISO-8601 UTC timestamp. */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val accountId: String,
    val categoryId: String? = null,
    val type: String,
    val amount: Double,
    val date: String,
    // Cached for display so the UI doesn't need a join
    val categoryName: String? = null,
    val accountName: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val isDeleted: Boolean = false
)
