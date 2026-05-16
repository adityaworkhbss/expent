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
    @SerializedName("name")
    val name: String
)
