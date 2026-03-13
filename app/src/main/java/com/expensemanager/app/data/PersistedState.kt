package com.expensemanager.app.data

import kotlinx.serialization.Serializable

@Serializable
data class PersistedTransaction(
    val id: String,
    val type: String,
    val amount: Double,
    val category: String,
    val paymentMode: String,
    val note: String,
    val tags: List<String>,
    val date: String,
    val time: String
)

@Serializable
data class PersistedAccount(
    val id: String,
    val name: String,
    val type: String,
    val openingBalance: Double
)

@Serializable
data class PersistedBudget(
    val id: String,
    val name: String,
    val amount: Double,
    val budgetType: String
)

@Serializable
data class PersistedScheduled(
    val id: String,
    val title: String,
    val amount: Double,
    val type: String,
    val frequency: String,
    val nextDate: String,
    val completed: Boolean
)

@Serializable
data class PersistedSettings(
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

@Serializable
data class PersistedState(
    val transactions: List<PersistedTransaction> = emptyList(),
    val accounts: List<PersistedAccount> = emptyList(),
    val budgets: List<PersistedBudget> = emptyList(),
    val scheduled: List<PersistedScheduled> = emptyList(),
    val tags: List<String> = emptyList(),
    val expenseCategories: List<String> = emptyList(),
    val incomeCategories: List<String> = emptyList(),
    val settings: PersistedSettings = PersistedSettings(),
    val analysisPeriod: String = "Month"
)


