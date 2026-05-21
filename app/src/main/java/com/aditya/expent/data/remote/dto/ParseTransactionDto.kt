package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ParseTransactionRequestDto(
    @SerializedName("text")
    val text: String
)

data class ParseTransactionResponseDto(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("requires_user_input")
    val requiresUserInput: Boolean,
    @SerializedName("missing_required_fields")
    val missingRequiredFields: List<String>,
    @SerializedName("data")
    val data: ParseTransactionDataDto?
)

data class ParseTransactionDataDto(
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("merchant")
    val merchant: String?,
    @SerializedName("category_id")
    val categoryId: String?,
    @SerializedName("category_name")
    val categoryName: String?,
    @SerializedName("account_id")
    val accountId: String?,
    @SerializedName("account_name")
    val accountName: String?,
    @SerializedName("payment_method")
    val paymentMethod: String?,
    @SerializedName("date")
    val date: String,
    @SerializedName("transaction_type")
    val transactionType: String,
    @SerializedName("note")
    val note: String?
)
