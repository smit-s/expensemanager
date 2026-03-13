package com.expensemanager.app.data

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

enum class TransactionType { Expense, Income, Transfer }
enum class BudgetType { Monthly, Yearly }
enum class AnalysisPeriod { Week, Month, Year, Custom }

data class ExpenseTransaction(
    val id: String = UUID.randomUUID().toString(),
    val type: TransactionType = TransactionType.Expense,
    val amount: Double,
    val category: String,
    val paymentMode: String,
    val note: String = "",
    val tags: List<String> = emptyList(),
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now()
)

data class Account(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    val openingBalance: Double = 0.0
)

data class Budget(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val budgetType: BudgetType = BudgetType.Monthly
)

data class ScheduledTxn(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val frequency: String,
    val nextDate: LocalDate,
    val completed: Boolean = false
)

data class SettingsState(
    val showBalances: Boolean = false,
    val haptics: Boolean = true,
    val dailyReminder: Boolean = true,
    val budgetAlerts: Boolean = false,
    val theme: String = "Light",
    val timeFormat: String = "12 hr",
    val decimalFormat: String = "Default",
    val currency: String = "USD ($)",
    val backupDestination: String = "Local Storage",
    val defaultPaymentMode: String = "Cash",
    val defaultCategory: String = "Others"
)



