package com.aditya.expent.domain.repository

import com.aditya.expent.domain.model.OnboardPaymentMode

interface PaymentModeRepository {
    suspend fun savePaymentModes(paymentModes: List<OnboardPaymentMode>): Result<Unit>
}