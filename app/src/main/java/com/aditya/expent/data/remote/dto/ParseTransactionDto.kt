package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ParseTransactionRequestDto(
    @SerializedName("text")
    val text: String
)

data class ParseTransactionResponseDto(
    @SerializedName("success")
    val success: Boolean = true,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("requires_user_input")
    val requiresUserInput: Boolean = false,
    @SerializedName("missing_required_fields")
    val missingRequiredFields: List<String> = emptyList(),
    @SerializedName("data")
    val data: ParseTransactionDataDto? = null
)

data class ParseTransactionDataDto(
    @SerializedName("category")
    val category: String? = null,
    @SerializedName("amount")
    val amount: Double = 0.0,
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("date")
    val date: String = "",
    @SerializedName("currency")
    val currency: String? = null,
    @SerializedName("confidence")
    val confidence: Double? = null,
    @SerializedName("merchant")
    val merchant: String? = null,
    @SerializedName("category_id")
    val categoryId: String? = null,
    @SerializedName("category_name")
    val categoryName: String? = null,
    @SerializedName("account_id")
    val accountId: String? = null,
    @SerializedName("account_name")
    val accountName: String? = null,
    @SerializedName("payment_method")
    val paymentMethod: String? = null,
    @SerializedName("transaction_type")
    val transactionType: String? = null,
    @SerializedName("note")
    val note: String? = null
) {
    val resolvedCategoryName: String
        get() = categoryName ?: category ?: "Others"

    val resolvedTransactionType: String
        get() = transactionType ?: type ?: "EXPENSE"

    val resolvedNote: String
        get() = note ?: description ?: merchant ?: ""
}
