package com.aditya.expent.domain.model

data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val date: String,
    val category: String,
    val type: TransactionType,
    val accountId: String? = null,
    val categoryId: String? = null,
    val transferToAccountId: String? = null,
    val paymentMethod: String? = null
)

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}
