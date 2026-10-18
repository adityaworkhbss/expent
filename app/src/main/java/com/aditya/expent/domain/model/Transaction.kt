package com.aditya.expent.domain.model

data class Transaction(
    val id: String,
    // Display label; transactions are named by their category
    val title: String,
    val amount: Double,
    val date: String,
    val category: String,
    val type: TransactionType,
    val accountId: String? = null,
    val categoryId: String? = null,
    val accountName: String? = null
)

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}
