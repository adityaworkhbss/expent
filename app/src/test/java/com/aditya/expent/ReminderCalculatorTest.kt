package com.aditya.expent

import com.aditya.expent.domain.model.Budget
import com.aditya.expent.domain.model.Expense
import com.aditya.expent.domain.model.Reminder
import com.aditya.expent.domain.usecase.ReminderCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReminderCalculatorTest {

    private fun day(year: Int, month: Int, dayOfMonth: Int): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, dayOfMonth, 10, 30)
        }

    private fun expense(id: String, date: Int, tenure: Int? = null, paid: Int = 0) =
        Expense(id = id, name = "Item $id", amount = 100.0, date = date, categoryId = null, tenure = tenure, paid = paid)

    private fun upcoming(
        today: Calendar,
        budgets: List<Budget> = emptyList(),
        expenses: List<Expense> = emptyList(),
        done: Set<String> = emptySet()
    ) = ReminderCalculator.upcoming(budgets, expenses, emptyMap(), { id, cycle -> "${id}_$cycle" in done }, today)

    @Test
    fun includesOnlyItemsDueWithinSevenDays() {
        val result = upcoming(
            today = day(2026, 10, 18),
            expenses = listOf(expense("today", 18), expense("in7", 25), expense("in8", 26), expense("past", 10))
        )
        assertEquals(listOf("today", "in7"), result.map { it.sourceId })
    }

    @Test
    fun dueDayAlreadyPassedRollsIntoNextMonth() {
        val result = upcoming(today = day(2026, 10, 28), expenses = listOf(expense("e", 2)))
        assertEquals("Nov 02, 2026", result.single().dueDate)
        assertEquals("2026-11", result.single().cycle)
    }

    @Test
    fun day31IsClampedToShortMonth() {
        val result = upcoming(today = day(2026, 11, 27), expenses = listOf(expense("e", 31)))
        assertEquals("Nov 30, 2026", result.single().dueDate)
    }

    @Test
    fun finishedEmiIsSkippedAndRunningEmiIsLabelled() {
        val result = upcoming(
            today = day(2026, 10, 18),
            expenses = listOf(expense("done", 20, tenure = 12, paid = 12), expense("running", 20, tenure = 12, paid = 3))
        )
        val reminder = result.single()
        assertEquals("running", reminder.sourceId)
        assertEquals(Reminder.Kind.EMI, reminder.kind)
        assertEquals("EMI 4 of 12", reminder.description)
    }

    @Test
    fun reminderMarkedDoneForItsCycleIsHidden() {
        val budget = Budget(id = "b", categoryId = "c", categoryName = "Salary", amount = 5000.0, date = 20)
        val today = day(2026, 10, 18)
        assertTrue(upcoming(today, budgets = listOf(budget), done = setOf("b_2026-10")).isEmpty())
        assertEquals("Salary", upcoming(today, budgets = listOf(budget)).single().title)
    }
}
