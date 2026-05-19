package com.aditya.expent.domain.repository

import com.aditya.expent.data.remote.dto.PaymentModeResponseDto
import com.aditya.expent.domain.model.OnboardPaymentMode

interface PaymentModeRepository {
    suspend fun savePaymentModes(paymentModes: List<OnboardPaymentMode>): Result<Unit>
    suspend fun getAccounts(): Result<List<PaymentModeResponseDto>>
    suspend fun deleteAccounts(id: String): Result<Unit>
}