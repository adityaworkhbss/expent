package com.aditya.expent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val amount: Double,
    val date: Int,
    val categoryId: String? = null,
    val accountId: String? = null,
    val tenure: Int? = null,
    val paid: Int = 0,
    val accountName: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val isDeleted: Boolean = false
)
