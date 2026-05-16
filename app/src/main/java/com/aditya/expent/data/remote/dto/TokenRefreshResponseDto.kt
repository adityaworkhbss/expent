package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TokenRefreshResponseDto(
    @SerializedName("accessToken")
    val accessToken: String,
    @SerializedName("refreshToken")
    val refreshToken: String,
    @SerializedName("message")
    val message: String? = null
)
