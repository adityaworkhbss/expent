package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TokenRefreshRequestDto(
    @SerializedName("refreshToken")
    val refreshToken: String
)
