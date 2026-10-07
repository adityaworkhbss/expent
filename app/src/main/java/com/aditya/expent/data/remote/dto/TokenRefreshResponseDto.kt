package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TokenRefreshDataDto(
    @SerializedName("accessToken")
    val accessToken: String = "",
    @SerializedName("refreshToken")
    val refreshToken: String? = null
)

data class TokenRefreshResponseDto(
    @SerializedName("success")
    val success: Boolean = true,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: TokenRefreshDataDto? = null,
    @SerializedName("accessToken")
    val flatAccessToken: String? = null,
    @SerializedName("refreshToken")
    val flatRefreshToken: String? = null
) {
    val accessToken: String
        get() = data?.accessToken ?: flatAccessToken ?: ""

    val refreshToken: String
        get() = data?.refreshToken ?: flatRefreshToken ?: ""
}

