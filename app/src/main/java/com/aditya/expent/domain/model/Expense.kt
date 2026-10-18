package com.aditya.expent.domain.model

/**
 * A recurring monthly outgoing.
 * - [tenure] == null → every month with no end (subscription, rent, SIP…)
 * - [tenure] == N   → EMI / loan for N months; [paid] counts months paid and may exceed [tenure].
 * [date] is the day of the month (1-31) it is due.
 */
data class Expense(
    val id: String,
    val name: String,
    val amount: Double,
    val date: Int,
    val categoryId: String?,
    val tenure: Int?,
    val paid: Int
) {
    val isEveryMonth: Boolean get() = tenure == null
    val isFinished: Boolean get() = tenure != null && paid >= tenure
}
