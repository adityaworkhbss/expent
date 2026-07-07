package com.aditya.expent.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aditya.expent.data.local.dao.AccountDao
import com.aditya.expent.data.local.dao.BudgetDao
import com.aditya.expent.data.local.dao.CategoryDao
import com.aditya.expent.data.local.dao.CustomizationDao
import com.aditya.expent.data.local.dao.ExpenseDao
import com.aditya.expent.data.local.dao.PendingSyncDao
import com.aditya.expent.data.local.dao.TransactionDao
import com.aditya.expent.data.local.dao.UserDao
import com.aditya.expent.data.local.entity.AccountEntity
import com.aditya.expent.data.local.entity.BudgetEntity
import com.aditya.expent.data.local.entity.CategoryEntity
import com.aditya.expent.data.local.entity.CustomizationEntity
import com.aditya.expent.data.local.entity.ExpenseEntity
import com.aditya.expent.data.local.entity.PendingSyncEntity
import com.aditya.expent.data.local.entity.TransactionEntity
import com.aditya.expent.data.local.entity.UserEntity

@Database(
    entities = [
        CategoryEntity::class,
        AccountEntity::class,
        BudgetEntity::class,
        ExpenseEntity::class,
        TransactionEntity::class,
        UserEntity::class,
        CustomizationEntity::class,
        PendingSyncEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ExpentDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao

    abstract fun accountDao(): AccountDao

    abstract fun budgetDao(): BudgetDao

    abstract fun expenseDao(): ExpenseDao

    abstract fun transactionDao(): TransactionDao

    abstract fun userDao(): UserDao

    abstract fun customizationDao(): CustomizationDao

    abstract fun pendingSyncDao(): PendingSyncDao
}
