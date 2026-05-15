package com.aditya.expent.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CategoryRequestDto(
    @SerializedName("name")
    val name: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("user_id")
    val user_id: String? = null,
    @SerializedName("color")
    val color: String? = null,
    @SerializedName("icon")
    val icon: String? = null
)

data class CategoryResponseDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("type")
    val type: String
)
