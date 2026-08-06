package com.aditya.expent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(

    @PrimaryKey
    val id: String,

    val userId: String,

    val accountId: String,

    val categoryId: String? = null,

    val transferToAccountId: String? = null,

    val type: String,

    val amount: String,

    val transactionDate: String,

    val note: String? = null,

    val merchant: String? = null,

    val paymentMethod: String? = null,

    val tags: List<String>? = null,

    val status: String? = null,

    val isSalary: Boolean? = null,

    val isDeleted: Boolean? = null,

    val createdAt: String,

    val updatedAt: String,

    val categoryName: String? = null,

    val accountName: String? = null,

    val transferToAccountName: String? = null,

    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
