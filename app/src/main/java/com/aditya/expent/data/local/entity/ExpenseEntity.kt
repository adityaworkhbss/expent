package com.aditya.expent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(

    @PrimaryKey
    val id: String,

    val userId: String,

    val accountId: String?,

    val transactionId: String?,

    val name: String,

    val principal: String,

    val tenure: Int,

    val monthlyEmi: String,

    val startDate: String,

    val endDate: String?,

    val nextDueDate: String,

    val remainingBalance: String,

    val monthsPaid: Int,

    val active: Boolean,

    val createdAt: String,

    val updatedAt: String,

    val accountName: String? = null,

    val syncStatus: SyncStatus = SyncStatus.SYNCED,

    val isDeleted: Boolean = false
)
