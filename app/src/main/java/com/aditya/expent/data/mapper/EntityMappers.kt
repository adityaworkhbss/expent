package com.aditya.expent.data.mapper

import com.aditya.expent.data.local.entity.AccountEntity
import com.aditya.expent.data.local.entity.BudgetEntity
import com.aditya.expent.data.local.entity.CategoryEntity
import com.aditya.expent.data.local.entity.CustomizationEntity
import com.aditya.expent.data.local.entity.ExpenseEntity
import com.aditya.expent.data.local.entity.TransactionEntity
import com.aditya.expent.data.local.entity.UserEntity
import com.aditya.expent.data.remote.dto.BudgetResponseDto
import com.aditya.expent.data.remote.dto.CategoryResponseDto
import com.aditya.expent.data.remote.dto.AccountDto
import com.aditya.expent.data.remote.dto.ExpenseIncomeResponseDto
import com.aditya.expent.data.remote.dto.PaymentModeResponseDto
import com.aditya.expent.data.remote.dto.TransactionResponseDto
import com.aditya.expent.data.remote.dto.UserCustomizationResponseDto
import com.aditya.expent.data.remote.dto.UserDto
import com.aditya.expent.domain.model.Transaction
import com.aditya.expent.domain.model.TransactionType

fun CategoryResponseDto.toEntity(userId: String? = null): CategoryEntity =
    CategoryEntity(
        id = id,
        name = name,
        type = type,
        userId = userId
    )

fun CategoryEntity.toDto(): CategoryResponseDto =
    CategoryResponseDto(
        id = id,
        name = name,
        type = type
    )

fun PaymentModeResponseDto.toEntity(userId: String? = null): AccountEntity =
    AccountEntity(
        id = id,
        name = name,
        type = type,
        userId = userId
    )

fun BudgetResponseDto.toEntity(): BudgetEntity =
    BudgetEntity(
        id = id,
        userId = userId,
        categoryId = categoryId,
        periodType = periodType,
        limitAmount = limitAmount,
        startDate = startDate,
        endDate = endDate,
        createdAt = createdAt,
        updatedAt = updatedAt,
        categoryName = category?.name
    )

fun ExpenseIncomeResponseDto.toEntity(): ExpenseEntity =
    ExpenseEntity(
        id = id,
        userId = userId,
        accountId = accountId,
        transactionId = transactionId,
        name = name,
        principal = principal,
        tenure = tenure,
        monthlyEmi = monthlyEmi,
        startDate = startDate,
        endDate = endDate,
        nextDueDate = nextDueDate,
        remainingBalance = remainingBalance,
        monthsPaid = monthsPaid,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
        accountName = account?.name
    )

fun TransactionResponseDto.toEntity(): TransactionEntity =
    TransactionEntity(
        id = id,
        userId = userId,
        accountId = accountId,
        categoryId = categoryId,
        transferToAccountId = transferToAccountId,
        type = type,
        amount = amount,
        transactionDate = transactionDate,
        note = note,
        merchant = merchant,
        paymentMethod = paymentMethod,
        tags = tags,
        status = status,
        isSalary = isSalary,
        isDeleted = isDeleted,
        createdAt = createdAt,
        updatedAt = updatedAt,
        categoryName = category?.name,
        accountName = account?.name,
        transferToAccountName = transferToAccount?.name
    )

fun TransactionEntity.toDomain(): Transaction =
    Transaction(
        id = id,
        title = note ?: merchant ?: "",
        amount = amount.toDoubleOrNull() ?: 0.0,
        date = transactionDate,
        category = categoryName ?: "",
        type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.EXPENSE),
        accountId = accountId,
        categoryId = categoryId,
        transferToAccountId = transferToAccountId,
        paymentMethod = paymentMethod
    )

fun UserDto.toEntity(): UserEntity =
    UserEntity(
        id = id,
        email = email,
        name = name,
        onboardingStep = onboardingStep
    )

fun UserCustomizationResponseDto.toEntity(): CustomizationEntity =
    CustomizationEntity(
        id = id,
        userId = userId,
        aiTransaction = aiTransaction,
        reminder = reminder
    )

fun CustomizationEntity.toDto(): UserCustomizationResponseDto =
    UserCustomizationResponseDto(
        id = id,
        userId = userId,
        aiTransaction = aiTransaction,
        reminder = reminder
    )

fun AccountEntity.toDto(): PaymentModeResponseDto =
    PaymentModeResponseDto(
        id = id,
        name = name,
        type = type
    )

fun BudgetEntity.toDto(): BudgetResponseDto =
    BudgetResponseDto(
        id = id,
        userId = userId,
        categoryId = categoryId,
        periodType = periodType,
        limitAmount = limitAmount,
        startDate = startDate,
        endDate = endDate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun ExpenseEntity.toDto(): ExpenseIncomeResponseDto =
    ExpenseIncomeResponseDto(
        id = id,
        userId = userId,
        accountId = accountId,
        transactionId = transactionId,
        name = name,
        principal = principal,
        tenure = tenure,
        monthlyEmi = monthlyEmi,
        startDate = startDate,
        endDate = endDate,
        nextDueDate = nextDueDate,
        remainingBalance = remainingBalance,
        monthsPaid = monthsPaid,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
        account = accountName?.let { AccountDto(id = accountId.orEmpty(), name = it) }
    )

fun TransactionEntity.toDto(): TransactionResponseDto =
    TransactionResponseDto(
        id = id,
        userId = userId,
        accountId = accountId,
        categoryId = categoryId,
        transferToAccountId = transferToAccountId,
        type = type,
        amount = amount,
        transactionDate = transactionDate,
        note = note,
        merchant = merchant,
        paymentMethod = paymentMethod,
        tags = tags,
        status = status,
        isSalary = isSalary,
        isDeleted = isDeleted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
