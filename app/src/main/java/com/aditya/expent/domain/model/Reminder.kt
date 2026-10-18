package com.aditya.expent.domain.model

/**
 * An income (Budget) or outgoing (Expense) due within the next few days, built from Room data.
 * [cycle] is the yyyy-MM month of [dueDate]; marking a reminder done hides it for that cycle only.
 */
data class Reminder(
    val sourceId: String,
    val kind: Kind,
    val title: String,
    val description: String,
    val amount: Double,
    val dueDate: String, // "MMM dd, yyyy"
    val cycle: String,
    val category: String,
    val categoryId: String?,
    val type: TransactionType,
    val tenure: Int? = null,
    val paid: Int = 0
) {
    enum class Kind { INCOME, EMI, SUBSCRIPTION }

    val id: String get() = "${sourceId}_$cycle"
}
