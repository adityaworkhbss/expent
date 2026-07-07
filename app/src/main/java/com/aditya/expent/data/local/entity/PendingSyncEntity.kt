package com.aditya.expent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync")
data class PendingSyncEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val entityType: String,

    val entityId: String,

    val operation: String,

    val payload: String,

    val createdAt: Long,

    val retryCount: Int = 0
)
