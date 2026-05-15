package com.aditya.expent.domain.model

data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val date: String,
    val category: String,
    val type: TransactionType
)

enum class TransactionType {
    INCOME,
    EXPENSE
}
