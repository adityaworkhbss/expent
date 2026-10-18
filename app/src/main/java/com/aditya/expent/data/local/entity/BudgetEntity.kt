package com.aditya.expent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val categoryId: String?,
    val amount: Double,
    val date: Int,
    val categoryName: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val isDeleted: Boolean = false
)
