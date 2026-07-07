package com.aditya.expent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(

    @PrimaryKey
    val id: String,

    val name: String,

    val type: String,

    val userId: String? = null
)