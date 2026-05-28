package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ExpenseIncomeRequestDto(
    @SerializedName("type")
    val type: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("startDate")
    val startDate: String,
    @SerializedName("tenure")
    val tenure: String? = null,
    @SerializedName("monthsPaid")
    val monthsPaid: String? = null
)

data class ExpenseIncomeResponseDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("accountId")
    val accountId: String?,

    @SerializedName("transactionId")
    val transactionId: String?,

    @SerializedName("name")
    val name: String,

    @SerializedName("principal")
    val principal: String,

    @SerializedName("tenure")
    val tenure: Int,

    @SerializedName("monthlyEmi")
    val monthlyEmi: String,

    @SerializedName("startDate")
    val startDate: String,

    @SerializedName("endDate")
    val endDate: String?,

    @SerializedName("nextDueDate")
    val nextDueDate: String,

    @SerializedName("remainingBalance")
    val remainingBalance: String,

    @SerializedName("monthsPaid")
    val monthsPaid: Int,

    @SerializedName("active")
    val active: Boolean,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String,

    @SerializedName("account")
    val account: AccountDto?
)

data class AccountDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String
)