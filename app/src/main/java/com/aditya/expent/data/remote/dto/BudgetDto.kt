package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BudgetRequestDto(
    @SerializedName("categoryId")
    val categoryId: String?,
    @SerializedName("periodType")
    val periodType: String,
    @SerializedName("limitAmount")
    val limitAmount: Double,
    @SerializedName("startDate")
    val startDate: String,
    @SerializedName("endDate")
    val endDate: String? = null
)

data class BudgetResponseDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("categoryId")
    val categoryId: String?,
    @SerializedName("periodType")
    val periodType: String,
    @SerializedName("limitAmount")
    val limitAmount: Double,
    @SerializedName("startDate")
    val startDate: String,
    @SerializedName("endDate")
    val endDate: String? = null
)
