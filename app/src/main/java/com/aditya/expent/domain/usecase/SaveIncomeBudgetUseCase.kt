package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.repository.IncomeBudgetRepository
import com.aditya.expent.presentation.onboard.RecurringIncome
import javax.inject.Inject

class SaveIncomeBudgetUseCase @Inject constructor(
    val repository: IncomeBudgetRepository
) {
    suspend operator fun invoke(
        salary : RecurringIncome,
        customIncomes : List<RecurringIncome>
    ): Result<Unit> {
        return repository.saveIncomeBudget(salary, customIncomes)
    }
}
