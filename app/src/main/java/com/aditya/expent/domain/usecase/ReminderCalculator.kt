package com.aditya.expent.domain.usecase

import com.aditya.expent.domain.model.Budget
import com.aditya.expent.domain.model.Expense
import com.aditya.expent.domain.model.Reminder
import com.aditya.expent.domain.model.TransactionType
import com.aditya.expent.utils.DayOfMonth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Builds reminders from the monthly incomes (budgets) and outgoings (expenses) already in Room:
 * anything whose next due day falls within [WINDOW_DAYS] days from today.
 */
object ReminderCalculator {

    const val WINDOW_DAYS = 7

    fun upcoming(
        budgets: List<Budget>,
        expenses: List<Expense>,
        categoryNames: Map<String, String>,
        isDone: (sourceId: String, cycle: String) -> Boolean,
        today: Calendar = Calendar.getInstance()
    ): List<Reminder> {
        val start = today.startOfDay()

        val incomes = budgets.mapNotNull { budget ->
            val due = nextDue(budget.date, start) ?: return@mapNotNull null
            val category = budget.categoryName
                ?: budget.categoryId?.let { categoryNames[it] }
                ?: "Income"
            Reminder(
                sourceId = budget.id,
                kind = Reminder.Kind.INCOME,
                title = category,
                description = "Credited on the ${DayOfMonth.withSuffix(budget.date)}",
                amount = budget.amount,
                dueDate = due.format("MMM dd, yyyy"),
                cycle = due.format("yyyy-MM"),
                category = category,
                categoryId = budget.categoryId,
                type = TransactionType.INCOME
            )
        }

        val outgoings = expenses
            .filterNot { it.isFinished }
            .mapNotNull { expense ->
                val due = nextDue(expense.date, start) ?: return@mapNotNull null
                val isEmi = !expense.isEveryMonth
                Reminder(
                    sourceId = expense.id,
                    kind = if (isEmi) Reminder.Kind.EMI else Reminder.Kind.SUBSCRIPTION,
                    title = expense.name,
                    description = if (isEmi) {
                        "EMI ${expense.paid + 1} of ${expense.tenure}"
                    } else {
                        "Billing on the ${DayOfMonth.withSuffix(expense.date)}"
                    },
                    amount = expense.amount,
                    dueDate = due.format("MMM dd, yyyy"),
                    cycle = due.format("yyyy-MM"),
                    category = expense.categoryId?.let { categoryNames[it] } ?: expense.name,
                    categoryId = expense.categoryId,
                    type = TransactionType.EXPENSE,
                    tenure = expense.tenure,
                    paid = expense.paid
                )
            }

        return (incomes + outgoings)
            .filterNot { isDone(it.sourceId, it.cycle) }
            .sortedBy { it.dueDate.parse()?.time ?: Long.MAX_VALUE }
    }

    /** The next date on [day] (clamped to the month's length) on or after [start], if within the window. */
    internal fun nextDue(day: Int, start: Calendar): Calendar? {
        val thisMonth = start.onDay(day, monthOffset = 0)
        val due = if (thisMonth.before(start)) start.onDay(day, monthOffset = 1) else thisMonth
        val days = TimeUnit.MILLISECONDS.toDays(due.timeInMillis - start.timeInMillis)
        return due.takeIf { days in 0..WINDOW_DAYS }
    }

    private fun Calendar.startOfDay(): Calendar = (clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun Calendar.onDay(day: Int, monthOffset: Int): Calendar = (clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, monthOffset)
        set(Calendar.DAY_OF_MONTH, day.coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH)))
    }

    private fun Calendar.format(pattern: String): String =
        SimpleDateFormat(pattern, Locale.US).format(time)

    private fun String.parse() = runCatching { SimpleDateFormat("MMM dd, yyyy", Locale.US).parse(this) }.getOrNull()
}
