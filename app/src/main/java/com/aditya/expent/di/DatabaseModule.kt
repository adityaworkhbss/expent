package com.aditya.expent.di

import android.content.Context
import androidx.room.Room
import com.aditya.expent.data.local.dao.AccountDao
import com.aditya.expent.data.local.dao.BudgetDao
import com.aditya.expent.data.local.dao.CategoryDao
import com.aditya.expent.data.local.dao.CustomizationDao
import com.aditya.expent.data.local.dao.ExpenseDao
import com.aditya.expent.data.local.dao.PendingSyncDao
import com.aditya.expent.data.local.dao.TransactionDao
import com.aditya.expent.data.local.dao.UserDao
import com.aditya.expent.data.local.database.ExpentDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): ExpentDatabase {
        return Room.databaseBuilder(
            context,
            ExpentDatabase::class.java,
            "expent_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideCategoryDao(database: ExpentDatabase): CategoryDao =
        database.categoryDao()

    @Provides
    fun provideAccountDao(database: ExpentDatabase): AccountDao =
        database.accountDao()

    @Provides
    fun provideBudgetDao(database: ExpentDatabase): BudgetDao =
        database.budgetDao()

    @Provides
    fun provideExpenseDao(database: ExpentDatabase): ExpenseDao =
        database.expenseDao()

    @Provides
    fun provideTransactionDao(database: ExpentDatabase): TransactionDao =
        database.transactionDao()

    @Provides
    fun provideUserDao(database: ExpentDatabase): UserDao =
        database.userDao()

    @Provides
    fun provideCustomizationDao(database: ExpentDatabase): CustomizationDao =
        database.customizationDao()

    @Provides
    fun providePendingSyncDao(database: ExpentDatabase): PendingSyncDao =
        database.pendingSyncDao()
}
