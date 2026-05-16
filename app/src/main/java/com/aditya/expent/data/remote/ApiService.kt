package com.aditya.expent.data.remote

import com.aditya.expent.data.remote.dto.AuthRequestDto
import com.aditya.expent.data.remote.dto.AuthResponseDto
import com.aditya.expent.data.remote.dto.CategoryRequestDto
import com.aditya.expent.data.remote.dto.CategoryResponseDto
import com.aditya.expent.data.remote.dto.PaymentModeRequestDto
import com.aditya.expent.data.remote.dto.PaymentModeResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

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
}