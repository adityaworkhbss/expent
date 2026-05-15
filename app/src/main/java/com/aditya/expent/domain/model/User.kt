package com.aditya.expent.domain.model

data class User(
    val id: String,
    val email: String,
    val name: String,
    val accessToken: String,
    val refreshToken: String,
    val onboardingStep: Int
)
