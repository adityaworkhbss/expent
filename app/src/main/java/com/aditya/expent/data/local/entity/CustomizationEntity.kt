package com.aditya.expent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customizations")
data class CustomizationEntity(

    @PrimaryKey
    val id: String,

    val userId: String,

    val aiTransaction: Boolean,

    val reminder: Boolean
)
