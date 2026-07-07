package com.aditya.expent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(

    @PrimaryKey
    val id: String,

    val userId: String,

    val categoryId: String?,

    val periodType: String,

    val limitAmount: String,

    val startDate: String,

    val endDate: String? = null,

    val createdAt: String? = null,

    val updatedAt: String? = null,

    val categoryName: String? = null
)
