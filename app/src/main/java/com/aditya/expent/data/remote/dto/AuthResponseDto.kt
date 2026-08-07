package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AuthDataDto(
    @SerializedName("accessToken")
    val accessToken: String = "",
    @SerializedName("refreshToken")
    val refreshToken: String = "",
    @SerializedName("onboardingCount")
    val onboardingStep: Int = 0,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("id")
    val id: String? = null
)

data class AuthResponseDto(
    @SerializedName("success")
    val success: Boolean = true,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: AuthDataDto? = null,

    // Fallbacks if backend responds flat
    @SerializedName("user")
    val userDto: UserDto? = null,
    @SerializedName("accessToken")
    val flatAccessToken: String? = null,
    @SerializedName("refreshToken")
    val flatRefreshToken: String? = null,
    @SerializedName("onboardingCount")
    val flatOnboardingStep: Int? = null
) {
    val accessToken: String
        get() = data?.accessToken ?: flatAccessToken ?: ""

    val refreshToken: String
        get() = data?.refreshToken ?: flatRefreshToken ?: ""

    val user: UserDto
        get() = userDto ?: UserDto(
            id = data?.id ?: "",
            email = data?.email ?: "",
            name = data?.name ?: "",
            onboardingStep = data?.onboardingStep ?: flatOnboardingStep ?: 0
        )
}

data class UserDto(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("email")
    val email: String = "",
    @SerializedName("name")
    val name: String = "",
    @SerializedName("onboardingCount")
    val onboardingStep: Int = 0
)
