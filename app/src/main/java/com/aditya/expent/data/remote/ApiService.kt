package com.aditya.expent.data.remote

import com.aditya.expent.data.remote.dto.AuthRequestDto
import com.aditya.expent.data.remote.dto.AuthResponseDto
import com.aditya.expent.data.remote.dto.BudgetRequestDto
import com.aditya.expent.data.remote.dto.BudgetResponseDto
import com.aditya.expent.data.remote.dto.CategoryRequestDto
import com.aditya.expent.data.remote.dto.CategoryResponseDto
import com.aditya.expent.data.remote.dto.PaymentModeRequestDto
import com.aditya.expent.data.remote.dto.PaymentModeResponseDto
import com.aditya.expent.data.remote.dto.TokenRefreshRequestDto
import com.aditya.expent.data.remote.dto.TokenRefreshResponseDto
import com.aditya.expent.data.remote.dto.ExpenseIncomeRequestDto
import com.aditya.expent.data.remote.dto.ExpenseIncomeResponseDto
import com.aditya.expent.data.remote.dto.OnboardingStepRequestDto
import com.aditya.expent.data.remote.dto.PaginatedTransactionsResponseDto
import com.aditya.expent.data.remote.dto.TransactionQueryDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.http.Query

interface ApiService {
    @POST("auth/google")
    suspend fun verifyGoogleToken(@Body request: AuthRequestDto): AuthResponseDto

    @POST("categories")
    suspend fun createCategories( @Body request: List<CategoryRequestDto>): List<CategoryResponseDto>

    @POST("accounts")
    suspend fun savePaymentModes(@Body request: List<PaymentModeRequestDto>) : List<PaymentModeResponseDto>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: TokenRefreshRequestDto): TokenRefreshResponseDto

    @GET("categories")
    suspend fun getCategories(): List<CategoryResponseDto>

    @POST("budgets")
    suspend fun saveBudgets(@Body request: List<BudgetRequestDto>): List<BudgetResponseDto>

    @POST("emis")
    suspend fun saveExpensesAndSubscriptions(@Body request: List<ExpenseIncomeRequestDto>): List<ExpenseIncomeResponseDto>

    @POST("auth/onboarding/increment")
    suspend fun updateOnboardingCount(@Body request: OnboardingStepRequestDto)

    @GET("transactions")
    suspend fun getTransactions(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("type") type: String? = null,
        @Query("categoryId") categoryId: String? = null,
        @Query("accountId") accountId: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): PaginatedTransactionsResponseDto
}