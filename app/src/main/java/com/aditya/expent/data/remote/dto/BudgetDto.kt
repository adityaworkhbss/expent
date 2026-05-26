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
    @SerializedName("userId")
    val userId: String,
    @SerializedName("categoryId")
    val categoryId: String?,
    @SerializedName("periodType")
    val periodType: String,
    @SerializedName("limitAmount")
    val limitAmount: String,
    @SerializedName("startDate")
    val startDate: String,
    @SerializedName("endDate")
    val endDate: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null,
    @SerializedName("category")
    val category: BudgetCategoryDto? = null
)

data class BudgetCategoryDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("color")
    val color: String? = null,
    @SerializedName("icon")
    val icon: String? = null
)

