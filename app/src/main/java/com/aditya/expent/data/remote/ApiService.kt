package com.aditya.expent.data.remote

import com.aditya.expent.data.remote.dto.AuthRequestDto
import com.aditya.expent.data.remote.dto.AuthResponseDto
import com.aditya.expent.data.remote.dto.CategoryRequestDto
import com.aditya.expent.data.remote.dto.CategoryResponseDto
import com.aditya.expent.data.remote.dto.PaymentModeRequestDto
import com.aditya.expent.data.remote.dto.PaymentModeResponseDto
import com.aditya.expent.data.remote.dto.TokenRefreshResponseDto
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header

interface ApiService {
    @POST("auth/google")
    suspend fun verifyGoogleToken(
        @Body request: AuthRequestDto
    ): AuthResponseDto

    @POST("categories")
    suspend fun createCategories(
        @Body request: List<CategoryRequestDto>
    ): List<CategoryResponseDto>

    @POST("accounts")
    suspend fun savePaymentModes(
        @Body request: List<PaymentModeRequestDto>
    ) : List<PaymentModeResponseDto>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Header("Cookie") refreshTokenCookie: String
    ): TokenRefreshResponseDto
}