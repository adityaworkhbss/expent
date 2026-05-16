package com.aditya.expent.di

import com.aditya.expent.data.repository.AuthRepositoryImpl
import com.aditya.expent.data.repository.CategoryRepositoryImpl
import com.aditya.expent.data.repository.ExpenseAndSubscriptionRepositoryImpl
import com.aditya.expent.data.repository.IncomeBudgetRepositoryImpl
import com.aditya.expent.data.repository.PaymentModeRepositoryImpl
import com.aditya.expent.domain.repository.AuthRepository
import com.aditya.expent.domain.repository.CategoryRepository
import com.aditya.expent.domain.repository.ExpenseAndSubscriptionRepository
import com.aditya.expent.domain.repository.IncomeBudgetRepository
import com.aditya.expent.domain.repository.PaymentModeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindPaymentModeRepository(
        paymentModeRepositoryImpl: PaymentModeRepositoryImpl
    ): PaymentModeRepository

    @Binds
    @Singleton
    abstract fun bindIncomeBudgetRepository(
        incomeBudgetRepositoryImpl: IncomeBudgetRepositoryImpl
    ): IncomeBudgetRepository

    @Binds
    @Singleton
    abstract fun bindExpenseAndSubscriptionRepository(
        expenseAndSubscriptionRepositoryImpl: ExpenseAndSubscriptionRepositoryImpl
    ): ExpenseAndSubscriptionRepository

}
