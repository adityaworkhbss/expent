package com.aditya.expent.data.remote

import com.aditya.expent.data.remote.dto.ApiResponse
import com.aditya.expent.data.remote.dto.AuthRequestDto
import com.aditya.expent.data.remote.dto.AuthResponseDto
import com.aditya.expent.data.remote.dto.AuthTestRequestDto
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

    @POST("auth/test-login")
    suspend fun testLogin(@Body request: AuthTestRequestDto): AuthResponseDto

    @POST("categories")
    suspend fun createCategories(@Body request: List<CategoryRequestDto>): ApiResponse<List<CategoryResponseDto>>

    @POST("accounts")
    suspend fun savePaymentModes(@Body request: List<PaymentModeRequestDto>): ApiResponse<List<PaymentModeResponseDto>>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: TokenRefreshRequestDto): TokenRefreshResponseDto

    @GET("categories")
    suspend fun getCategories(): ApiResponse<List<CategoryResponseDto>>

    @GET("budgets")
    suspend fun getBudgets(): ApiResponse<List<BudgetResponseDto>?>

    @POST("budgets")
    suspend fun saveBudgets(@Body request: List<BudgetRequestDto>): ApiResponse<List<BudgetResponseDto>>

    @GET("emis")
    suspend fun getExpensesAndSubscriptions(): ApiResponse<List<ExpenseIncomeResponseDto>?>

    @POST("emis")
    suspend fun saveExpensesAndSubscriptions(@Body request: List<ExpenseIncomeRequestDto>): ApiResponse<List<ExpenseIncomeResponseDto>>

    @DELETE("emis/{id}")
    suspend fun deleteEmi(@Path("id") emiId: String): ApiResponse<Any?>

    @PUT("emis/{id}")
    suspend fun updateEmi(@Path("id") emiId: String, @Body request: ExpenseIncomeRequestDto): ApiResponse<ExpenseIncomeResponseDto>

    @POST("auth/onboarding/increment")
    suspend fun updateOnboardingCount(@Body request: OnboardingStepRequestDto)

    @GET("transactions")
    suspend fun getTransactions(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): ApiResponse<PaginatedTransactionsResponseDto>

    @GET("transactions")
    suspend fun getTransactions(
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): ApiResponse<PaginatedTransactionsResponseDto>

    @POST("transactions")
    suspend fun addTransaction(@Body request: CreateTransactionRequestDto): ApiResponse<TransactionResponseDto>

    @GET("accounts")
    suspend fun getAccounts(): ApiResponse<List<PaymentModeResponseDto>>

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") categoryId: String): ApiResponse<Any?>

    @DELETE("accounts/{id}")
    suspend fun deleteAccount(@Path("id") accountId: String): ApiResponse<Any?>

    @DELETE("budgets/{id}")
    suspend fun deleteBudget(@Path("id") budgetId: String): ApiResponse<Any?>

    @PUT("budgets/{id}")
    suspend fun updateBudget(@Path("id") budgetId: String, @Body request: BudgetRequestDto): ApiResponse<BudgetResponseDto>

    @GET("user-customization")
    suspend fun getUserCustomization(): ApiResponse<UserCustomizationResponseDto>

    @PUT("user-customization")
    suspend fun updateUserCustomization(@Body request: UserCustomizationResponseDto): ApiResponse<UserCustomizationResponseDto>

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