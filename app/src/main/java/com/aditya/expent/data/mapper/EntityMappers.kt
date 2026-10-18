package com.aditya.expent.data.mapper

import com.aditya.expent.data.local.entity.AccountEntity
import com.aditya.expent.data.local.entity.BudgetEntity
import com.aditya.expent.data.local.entity.CategoryEntity
import com.aditya.expent.data.local.entity.CustomizationEntity
import com.aditya.expent.data.local.entity.ExpenseEntity
import com.aditya.expent.data.local.entity.TransactionEntity
import com.aditya.expent.data.remote.dto.BudgetResponseDto
import com.aditya.expent.data.remote.dto.CategoryResponseDto
import com.aditya.expent.data.remote.dto.ExpenseIncomeResponseDto
import com.aditya.expent.data.remote.dto.PaymentModeResponseDto
import com.aditya.expent.data.remote.dto.TransactionCategoryDto
import com.aditya.expent.data.remote.dto.TransactionAccountDto
import com.aditya.expent.data.remote.dto.TransactionResponseDto
import com.aditya.expent.data.remote.dto.UserCustomizationResponseDto
import com.aditya.expent.data.remote.dto.BudgetRequestDto
import com.aditya.expent.data.remote.dto.ExpenseIncomeRequestDto
import com.aditya.expent.data.local.entity.SyncStatus
import com.aditya.expent.domain.model.Budget
import com.aditya.expent.domain.model.Expense
import com.aditya.expent.utils.DayOfMonth
import com.aditya.expent.domain.model.OnboardCategory
import com.aditya.expent.domain.model.OnboardPaymentMode
import com.aditya.expent.domain.model.Transaction
import com.aditya.expent.domain.model.TransactionType
import com.aditya.expent.utils.AppUtils

fun CategoryResponseDto.toEntity(
    userId: String? = null,
    syncStatus: SyncStatus = SyncStatus.SYNCED,
    isDeleted: Boolean = false
): CategoryEntity =
    CategoryEntity(
        id = id,
        name = name,
        type = type,
        userId = userId ?: this.userId,
        syncStatus = syncStatus,
        isDeleted = isDeleted
    )

fun OnboardCategory.toEntity(
    userId: String? = null,
    syncStatus: SyncStatus = SyncStatus.SYNCED,
    isDeleted: Boolean = false
): CategoryEntity =
    CategoryEntity(
        id = if (id.isBlank()) "local-${java.util.UUID.randomUUID()}" else id,
        name = name,
        type = type,
        userId = userId,
        syncStatus = syncStatus,
        isDeleted = isDeleted
    )

fun CategoryEntity.toDomain(): OnboardCategory =
    OnboardCategory(
        id = id,
        name = name,
        type = type
    )

fun CategoryEntity.toDto(): CategoryResponseDto =
    CategoryResponseDto(
        id = id,
        name = name,
        type = type
    )

fun PaymentModeResponseDto.toEntity(
    userId: String? = null,
    syncStatus: SyncStatus = SyncStatus.SYNCED,
    isDeleted: Boolean = false
): AccountEntity =
    AccountEntity(
        id = id,
        name = name,
        type = type,
        userId = userId ?: this.userId,
        syncStatus = syncStatus,
        isDeleted = isDeleted
    )

fun OnboardPaymentMode.toEntity(
    userId: String? = null,
    syncStatus: SyncStatus = SyncStatus.SYNCED,
    isDeleted: Boolean = false
): AccountEntity =
    AccountEntity(
        id = "local-${java.util.UUID.randomUUID()}",
        name = name,
        type = type,
        userId = userId,
        syncStatus = syncStatus,
        isDeleted = isDeleted
    )

fun BudgetResponseDto.toEntity(
    syncStatus: SyncStatus = SyncStatus.SYNCED,
    isDeleted: Boolean = false
): BudgetEntity =
    BudgetEntity(
        id = id,
        userId = userId ?: "",
        categoryId = categoryId,
        // Backend still calls the monthly income "limitAmount" and keeps a full start date
        amount = limitAmount,
        date = DayOfMonth.from(startDate) ?: DayOfMonth.MIN,
        categoryName = category?.name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = syncStatus,
        isDeleted = isDeleted
    )

fun BudgetEntity.toDomain(): Budget =
    Budget(
        id = id,
        categoryId = categoryId,
        categoryName = categoryName,
        amount = amount,
        date = date
    )

// Current API contract: always MONTHLY, a full date in this month, no end date
fun BudgetEntity.toRequest(): BudgetRequestDto =
    BudgetRequestDto(
        categoryId = categoryId,
        periodType = "MONTHLY",
        limitAmount = amount,
        startDate = DayOfMonth.toIsoDateThisMonth(date),
        endDate = null
    )

fun ExpenseIncomeResponseDto.toEntity(
    syncStatus: SyncStatus = SyncStatus.SYNCED,
    isDeleted: Boolean = false
): ExpenseEntity =
    ExpenseEntity(
        id = id,
        userId = userId ?: "",
        name = resolvedName,
        // The amount entered in the app is the monthly payment; the server may echo it back as "amount"
        amount = monthlyEmiAmount.toDoubleOrNull() ?: 0.0,
        date = DayOfMonth.from(resolvedStartDate) ?: DayOfMonth.MIN,
        categoryId = categoryId,
        accountId = accountId,
        // Backend sends 0 for "every month"; the app keeps that as null
        tenure = tenure?.takeIf { it > 0 },
        paid = monthsPaid ?: 0,
        accountName = account?.name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = syncStatus,
        isDeleted = isDeleted
    )

fun ExpenseEntity.toDomain(): Expense =
    Expense(
        id = id,
        name = name,
        amount = amount,
        date = date,
        categoryId = categoryId,
        accountId = accountId,
        tenure = tenure,
        paid = paid
    )

// Current API contract: "subscription" vs "expense" is decided by tenure, the day becomes a full date
fun ExpenseEntity.toRequest(): ExpenseIncomeRequestDto =
    ExpenseIncomeRequestDto(
        type = if (tenure == null) "subscription" else "expense",
        name = name,
        amount = amount.toPlainAmount(),
        startDate = DayOfMonth.toIsoDateThisMonth(date),
        categoryId = categoryId,
        accountId = accountId,
        tenure = tenure?.toString(),
        monthsPaid = if (tenure == null) null else paid.toString()
    )

private fun Double.toPlainAmount(): String =
    java.math.BigDecimal(toString()).stripTrailingZeros().toPlainString()

fun TransactionResponseDto.toEntity(
    syncStatus: SyncStatus = SyncStatus.SYNCED
): TransactionEntity =
    TransactionEntity(
        id = id,
        userId = userId ?: "",
        accountId = accountId,
        categoryId = categoryId,
        transferToAccountId = transferToAccountId,
        type = type,
        amount = amount,
        transactionDate = AppUtils.convertDateToIso(transactionDate),
        note = note,
        merchant = merchant,
        paymentMethod = paymentMethod,
        tags = tags,
        status = status,
        isSalary = isSalary,
        isDeleted = isDeleted,
        createdAt = createdAt ?: "",
        updatedAt = updatedAt ?: "",
        categoryName = category?.name,
        accountName = account?.name,
        transferToAccountName = transferToAccount?.name,
        syncStatus = syncStatus
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

fun TransactionEntity.toDto(): TransactionResponseDto =
    TransactionResponseDto(
        id = id,
        userId = userId,
        accountId = accountId,
        categoryId = categoryId,
        transferToAccountId = transferToAccountId,
        type = type,
        amountRaw = amount,
        transactionDateRaw = transactionDate,
        note = note,
        merchant = merchant,
        paymentMethod = paymentMethod,
        tags = tags,
        status = status,
        isSalary = isSalary,
        isDeleted = isDeleted,
        createdAt = createdAt,
        updatedAt = updatedAt,
        category = categoryName?.let { TransactionCategoryDto(id = categoryId.orEmpty(), name = it) },
        account = accountName?.let { TransactionAccountDto(id = accountId, name = it, type = "") }
    )
