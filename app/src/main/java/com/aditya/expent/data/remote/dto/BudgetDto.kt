package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BudgetRequestDto(
    @SerializedName("categoryId")
    val categoryId: String? = null,
    @SerializedName("periodType")
    val periodType: String,
    @SerializedName("limitAmount")
    val limitAmount: Double,
    @SerializedName("startDate")
    val startDate: String? = null,
    @SerializedName("endDate")
    val endDate: String? = null
)

data class BudgetResponseDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("userId")
    val userId: String? = null,
    @SerializedName("categoryId")
    val categoryId: String? = null,
    @SerializedName("periodType")
    val periodType: String,
    @SerializedName("limitAmount")
    val limitAmount: Double,
    @SerializedName("startDate")
    val startDate: String? = null,
    @SerializedName("endDate")
    val endDate: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null,
    @SerializedName("category")
    val category: CategoryResponseDto? = null
)
