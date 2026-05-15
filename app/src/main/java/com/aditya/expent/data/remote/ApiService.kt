package com.aditya.expent.data.remote

import com.aditya.expent.data.remote.dto.AuthRequestDto
import com.aditya.expent.data.remote.dto.AuthResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/auth/google")
    suspend fun verifyGoogleToken(
        @Body request: AuthRequestDto
    ): AuthResponseDto
}