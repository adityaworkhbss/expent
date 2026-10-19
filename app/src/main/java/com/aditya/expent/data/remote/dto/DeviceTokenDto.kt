package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RegisterDeviceTokenRequestDto(
    @SerializedName("token")
    val token: String,
    @SerializedName("userId")
    val userId: String
)

data class RemoveDeviceTokenRequestDto(
    @SerializedName("token")
    val token: String
)

data class DeviceTokenResponseDto(
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("userId")
    val userId: String? = null
)
