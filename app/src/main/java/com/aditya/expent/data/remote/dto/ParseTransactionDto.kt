package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ParseTransactionRequestDto(
    @SerializedName("text")
    val text: String,
    // User's local date (yyyy-MM-dd) so "today" / "yesterday" resolve to the right day
    @SerializedName("today")
    val today: String
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

// Only what a transaction stores: type, amount, date, category and account
data class ParseTransactionDataDto(
    @SerializedName("type")
    val type: String? = null,
    @SerializedName("amount")
    val amount: Double = 0.0,
    @SerializedName("date")
    val date: String = "",
    @SerializedName("category_id")
    val categoryId: String? = null,
    @SerializedName("category_name")
    val categoryName: String? = null,
    @SerializedName("account_id")
    val accountId: String? = null,
    @SerializedName("account_name")
    val accountName: String? = null
) {
    val resolvedCategoryName: String
        get() = categoryName?.takeIf { it.isNotBlank() } ?: "Others"
}
