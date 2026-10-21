package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FeedbackRequestDto(
    @SerializedName("userId")
    val userId: String,
    @SerializedName("rating")
    val rating: Int,
    @SerializedName("message")
    val message: String
)
