package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AuthRequestDto(
    @SerializedName("idToken")
    val idToken: String
)
