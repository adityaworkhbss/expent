package com.aditya.expent.domain.model

/**
 * A recurring monthly income (salary, freelance…). Not a spending limit.
 * [date] is the day of the month (1-31) the money comes in.
 */
data class Budget(
    val id: String,
    val categoryId: String?,
    val categoryName: String?,
    val amount: Double,
    val date: Int
)
