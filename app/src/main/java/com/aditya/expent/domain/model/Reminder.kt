package com.aditya.expent.domain.model


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
