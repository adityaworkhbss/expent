package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PaymentModeRequestDto(
    @SerializedName("name")
    val name: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("user_id")
    val user_id: String? = null
)

data class PaymentModeResponseDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("userId")
    val userId: String? = null
)
