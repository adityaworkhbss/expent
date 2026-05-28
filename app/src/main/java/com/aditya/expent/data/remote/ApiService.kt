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
import com.aditya.expent.data.remote.dto.CreateTransactionRequestDto
import com.aditya.expent.data.remote.dto.TransactionResponseDto
import com.aditya.expent.data.remote.dto.UserCustomizationResponseDto
import com.aditya.expent.data.remote.dto.ParseTransactionRequestDto
import com.aditya.expent.data.remote.dto.ParseTransactionResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
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

    @GET("budgets")
    suspend fun getBudgets(): List<BudgetResponseDto>

    @POST("budgets")
    suspend fun saveBudgets(@Body request: List<BudgetRequestDto>): List<BudgetResponseDto>

    @GET("emis")
    suspend fun getExpensesAndSubscriptions(): List<ExpenseIncomeResponseDto>

    @POST("emis")
    suspend fun saveExpensesAndSubscriptions(@Body request: List<ExpenseIncomeRequestDto>): List<ExpenseIncomeResponseDto>

    @POST("auth/onboarding/increment")
    suspend fun updateOnboardingCount(@Body request: OnboardingStepRequestDto)

    @GET("transactions")
    suspend fun getTransactions(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): PaginatedTransactionsResponseDto

    @GET("transactions")
    suspend fun getTransactions(
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): PaginatedTransactionsResponseDto

    @POST("transactions")
    suspend fun addTransaction(@Body request: CreateTransactionRequestDto): TransactionResponseDto

    @GET("accounts")
    suspend fun getAccounts(): List<PaymentModeResponseDto>

    @DELETE("categories/{id}")
    suspend fun deleteCategory( @Path("id") categoryId: String)

    @DELETE("accounts/{id}")
    suspend fun deleteAccount( @Path("id") accountId: String)

    @DELETE("budgets/{id}")
    suspend fun deleteBudget(@Path("id") budgetId: String)

    @PUT("budgets/{id}")
    suspend fun updateBudget(@Path("id") budgetId: String, @Body request: BudgetRequestDto): BudgetResponseDto

    @GET("user-customization")
    suspend fun getUserCustomization(): UserCustomizationResponseDto

    @PUT("user-customization")
    suspend fun updateUserCustomization(@Body request: UserCustomizationResponseDto): UserCustomizationResponseDto

    @POST("parse-transaction")
    suspend fun parseTransaction(@Body request: ParseTransactionRequestDto): ParseTransactionResponseDto

//    @GET("transactions")
//    suspend fun getTransactions(
//        @Query("from") from: String? = null,
//        @Query("to") to: String? = null,
//        @Query("type") type: String? = null,
//        @Query("categoryId") categoryId: String? = null,
//        @Query("accountId") accountId: String? = null,
//        @Query("page") page: Int? = null,
//        @Query("limit") limit: Int? = null
//    ): PaginatedTransactionsResponseDto
}