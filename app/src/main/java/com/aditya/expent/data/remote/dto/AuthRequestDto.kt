package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AuthRequestDto(
    @SerializedName("idToken")
    val idToken: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("id")
    val id: String? = null
)

data class AuthTestRequestDto(
    @SerializedName("email")
    val email: String
)