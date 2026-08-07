package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ExpenseIncomeRequestDto(
    @SerializedName("type")
    val type: String,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("amount")
    val amount: String? = null,
    @SerializedName("date")
    val date: String? = null,
    @SerializedName("startDate")
    val startDate: String? = null,
    @SerializedName("categoryId")
    val categoryId: String? = null,
    @SerializedName("accountId")
    val accountId: String? = null,
    @SerializedName("tenure")
    val tenure: String? = null,
    @SerializedName("monthsPaid")
    val monthsPaid: String? = null
)

data class ExpenseIncomeResponseDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("userId")
    val userId: String? = null,

    @SerializedName("accountId")
    val accountId: String? = null,

    @SerializedName("categoryId")
    val categoryId: String? = null,

    @SerializedName("transactionId")
    val transactionId: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("amount")
    val amountRaw: Any? = null,

    @SerializedName("type")
    val type: String? = null,

    @SerializedName("date")
    val date: String? = null,

    @SerializedName("principal")
    val principal: String? = null,

    @SerializedName("tenure")
    val tenure: Int? = null,

    @SerializedName("monthlyEmi")
    val monthlyEmi: String? = null,

    @SerializedName("startDate")
    val startDate: String? = null,

    @SerializedName("endDate")
    val endDate: String? = null,

    @SerializedName("nextDueDate")
    val nextDueDate: String? = null,

    @SerializedName("remainingBalance")
    val remainingBalance: String? = null,

    @SerializedName("monthsPaid")
    val monthsPaid: Int? = null,

    @SerializedName("active")
    val active: Boolean? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("updatedAt")
    val updatedAt: String? = null,

    @SerializedName("account")
    val account: AccountDto? = null
) {
    val resolvedName: String
        get() = name ?: description ?: "EMI"

    val resolvedAmount: String
        get() = amountRaw?.toString() ?: monthlyEmi ?: principal ?: "0"

    val resolvedStartDate: String
        get() = startDate ?: date ?: ""
}

data class AccountDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String
)