package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserCustomizationResponseDto(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("userId")
    val userId: String = "",
    @SerializedName("currency")
    val currency: String? = "INR",
    @SerializedName("theme")
    val theme: String? = "dark",
    @SerializedName("aiTransaction")
    val aiTransaction: Boolean = false,
    @SerializedName("reminder")
    val reminder: Boolean = false
)