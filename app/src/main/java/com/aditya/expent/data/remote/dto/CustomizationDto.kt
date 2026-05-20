package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserCustomizationResponseDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("userId")
    val userId: String,
    @SerializedName("aiTransaction")
    val aiTransaction: Boolean,
    @SerializedName("reminder")
    val reminder: Boolean
)