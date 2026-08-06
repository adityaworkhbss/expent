package com.aditya.expent.domain.repository

import com.aditya.expent.data.remote.dto.PaymentModeResponseDto
import com.aditya.expent.domain.model.OnboardPaymentMode
import kotlinx.coroutines.flow.Flow

interface PaymentModeRepository {
    fun getAccounts(): Flow<List<PaymentModeResponseDto>>
    suspend fun savePaymentModes(paymentModes: List<OnboardPaymentMode>)
    suspend fun deleteAccounts(id: String)
    suspend fun refreshAccounts()
}