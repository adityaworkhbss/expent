package com.aditya.expent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(

    @PrimaryKey
    val id: String,

    val name: String,

    val type: String,

    val userId: String? = null,

    val syncStatus: SyncStatus = SyncStatus.SYNCED,

    val isDeleted: Boolean = false
)
