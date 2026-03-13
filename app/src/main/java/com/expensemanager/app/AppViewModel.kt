package com.expensemanager.app

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expensemanager.app.data.Account
import com.expensemanager.app.data.AnalysisPeriod
import com.expensemanager.app.data.AppRepository
import com.expensemanager.app.data.Budget
import com.expensemanager.app.data.BudgetType
import com.expensemanager.app.data.ExpenseTransaction
import com.expensemanager.app.data.PersistedAccount
import com.expensemanager.app.data.PersistedBudget
import com.expensemanager.app.data.PersistedScheduled
import com.expensemanager.app.data.PersistedSettings
import com.expensemanager.app.data.PersistedState
import com.expensemanager.app.data.PersistedTransaction
import com.expensemanager.app.data.ScheduledTxn
import com.expensemanager.app.data.SettingsState
import com.expensemanager.app.data.TransactionType
import com.expensemanager.app.google.GoogleAuthManager
import com.expensemanager.app.google.GoogleDriveBackupManager
import java.time.LocalDate
import java.time.LocalTime
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val authManager = GoogleAuthManager(application)
    private val driveBackupManager = GoogleDriveBackupManager(application)

    val transactions = mutableStateListOf<ExpenseTransaction>()
    val accounts = mutableStateListOf<Account>()
    val budgets = mutableStateListOf<Budget>()
    val scheduled = mutableStateListOf<ScheduledTxn>()
    val tags = mutableStateListOf<String>()
    val expenseCategories = mutableStateListOf<String>()
    val incomeCategories = mutableStateListOf<String>()

    var settings by mutableStateOf(SettingsState())
    var analysisPeriod by mutableStateOf(AnalysisPeriod.Month)
    var signedInEmail by mutableStateOf<String?>(null)
    var signedInName by mutableStateOf<String?>(null)
    var isGoogleSignedIn by mutableStateOf(false)
    var signInStatusMessage by mutableStateOf("")
    val appPackageId: String = application.packageName
    val appSigningSha1: String = readSigningSha1(application)

    init {
        loadPersistedState()
        authManager.getLastAccount()?.let { account ->
            signedInEmail = account.email
            signedInName = account.displayName ?: account.email
            isGoogleSignedIn = true
        }
    }

    private fun loadPersistedState() {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = repository.loadState()
            withContext(Dispatchers.Main) {
                if (loaded == null) {
                    seedDefaults()
                    persist()
                } else {
                    applyState(loaded)
                }
            }
        }
    }

    private fun seedDefaults() {
        accounts.clear()
        accounts.add(Account(name = "Cash", type = "Cash", openingBalance = 0.0))
        expenseCategories.clear()
        expenseCategories.addAll(
            listOf(
                "Others", "Food and Dining", "Shopping", "Travelling", "Entertainment",
                "Medical", "Personal Care", "Education", "Bills and Utilities", "Investments"
            )
        )
        incomeCategories.clear()
        incomeCategories.addAll(listOf("Salary", "Bonus", "Gift", "Other Income"))
        transactions.clear()
        budgets.clear()
        scheduled.clear()
        tags.clear()
        settings = SettingsState()
        analysisPeriod = AnalysisPeriod.Month
    }

    private fun applyState(state: PersistedState) {
        transactions.clear()
        transactions.addAll(
            state.transactions.mapNotNull {
                runCatching {
                    ExpenseTransaction(
                        id = it.id,
                        type = TransactionType.valueOf(it.type),
                        amount = it.amount,
                        category = it.category,
                        paymentMode = it.paymentMode,
                        note = it.note,
                        tags = it.tags,
                        date = LocalDate.parse(it.date),
                        time = LocalTime.parse(it.time)
                    )
                }.getOrNull()
            }
        )

        accounts.clear()
        accounts.addAll(state.accounts.map { Account(it.id, it.name, it.type, it.openingBalance) })
        budgets.clear()
        budgets.addAll(
            state.budgets.mapNotNull {
                runCatching { Budget(it.id, it.name, it.amount, BudgetType.valueOf(it.budgetType)) }.getOrNull()
            }
        )
        scheduled.clear()
        scheduled.addAll(
            state.scheduled.mapNotNull {
                runCatching {
                    ScheduledTxn(
                        id = it.id,
                        title = it.title,
                        amount = it.amount,
                        type = TransactionType.valueOf(it.type),
                        frequency = it.frequency,
                        nextDate = LocalDate.parse(it.nextDate),
                        completed = it.completed
                    )
                }.getOrNull()
            }
        )
        tags.clear()
        tags.addAll(state.tags)

        expenseCategories.clear()
        expenseCategories.addAll(state.expenseCategories.ifEmpty { listOf("Others") })
        incomeCategories.clear()
        incomeCategories.addAll(state.incomeCategories.ifEmpty { listOf("Salary") })

        settings = SettingsState(
            showBalances = state.settings.showBalances,
            haptics = state.settings.haptics,
            dailyReminder = state.settings.dailyReminder,
            budgetAlerts = state.settings.budgetAlerts,
            theme = state.settings.theme,
            timeFormat = state.settings.timeFormat,
            decimalFormat = state.settings.decimalFormat,
            currency = state.settings.currency,
            backupDestination = state.settings.backupDestination,
            defaultPaymentMode = state.settings.defaultPaymentMode,
            defaultCategory = state.settings.defaultCategory
        )

        analysisPeriod = runCatching { AnalysisPeriod.valueOf(state.analysisPeriod) }.getOrDefault(AnalysisPeriod.Month)
    }

    private fun snapshot(): PersistedState = PersistedState(
        transactions = transactions.map {
            PersistedTransaction(
                id = it.id,
                type = it.type.name,
                amount = it.amount,
                category = it.category,
                paymentMode = it.paymentMode,
                note = it.note,
                tags = it.tags,
                date = it.date.toString(),
                time = it.time.toString()
            )
        },
        accounts = accounts.map { PersistedAccount(it.id, it.name, it.type, it.openingBalance) },
        budgets = budgets.map { PersistedBudget(it.id, it.name, it.amount, it.budgetType.name) },
        scheduled = scheduled.map {
            PersistedScheduled(
                id = it.id,
                title = it.title,
                amount = it.amount,
                type = it.type.name,
                frequency = it.frequency,
                nextDate = it.nextDate.toString(),
                completed = it.completed
            )
        },
        tags = tags.toList(),
        expenseCategories = expenseCategories.toList(),
        incomeCategories = incomeCategories.toList(),
        settings = PersistedSettings(
            showBalances = settings.showBalances,
            haptics = settings.haptics,
            dailyReminder = settings.dailyReminder,
            budgetAlerts = settings.budgetAlerts,
            theme = settings.theme,
            timeFormat = settings.timeFormat,
            decimalFormat = settings.decimalFormat,
            currency = settings.currency,
            backupDestination = settings.backupDestination,
            defaultPaymentMode = settings.defaultPaymentMode,
            defaultCategory = settings.defaultCategory
        ),
        analysisPeriod = analysisPeriod.name
    )

    private fun persist() {
        viewModelScope.launch(Dispatchers.IO) { repository.saveState(snapshot()) }
    }

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        category: String,
        paymentMode: String,
        note: String,
        selectedTags: List<String>,
        date: LocalDate,
        time: LocalTime
    ) {
        if (amount <= 0.0) return
        val normalizedTags = selectedTags.mapNotNull(::normalizeTag).distinct()
        transactions.add(
            ExpenseTransaction(
                type = type,
                amount = amount,
                category = category,
                paymentMode = paymentMode,
                note = note,
                tags = normalizedTags,
                date = date,
                time = time
            )
        )
        normalizedTags.forEach { tag ->
            if (!tags.contains(tag)) tags.add(tag)
        }
        persist()
    }

    fun addAccount(name: String, type: String, openingBalance: Double) {
        if (name.isBlank()) return
        accounts.add(Account(name = name.trim(), type = type, openingBalance = openingBalance))
        persist()
    }

    fun deleteAccount(accountId: String): Boolean {
        val account = accounts.firstOrNull { it.id == accountId } ?: return false
        if (account.name.equals("Cash", ignoreCase = true)) return false
        val removed = accounts.remove(account)
        if (removed) persist()
        return removed
    }

    fun addBudget(name: String, amount: Double, type: BudgetType) {
        if (name.isBlank() || amount <= 0.0) return
        budgets.add(Budget(name = name.trim(), amount = amount, budgetType = type))
        persist()
    }

    fun addTag(tag: String) {
        val value = normalizeTag(tag) ?: return
        if (!tags.contains(value)) {
            tags.add(value)
            persist()
        }
    }

    fun addCategory(name: String, income: Boolean) {
        val value = name.trim()
        if (value.isBlank()) return
        if (income) {
            if (!incomeCategories.contains(value)) incomeCategories.add(value)
        } else {
            if (!expenseCategories.contains(value)) expenseCategories.add(value)
        }
        persist()
    }

    fun addScheduled(title: String, amount: Double, type: TransactionType, frequency: String, nextDate: LocalDate) {
        if (title.isBlank() || amount <= 0.0) return
        scheduled.add(
            ScheduledTxn(
                title = title.trim(),
                amount = amount,
                type = type,
                frequency = frequency,
                nextDate = nextDate
            )
        )
        persist()
    }

    fun updateAnalysisPeriod(period: AnalysisPeriod) {
        analysisPeriod = period
        persist()
    }

    fun updateSettings(newState: SettingsState) {
        settings = newState
        persist()
    }

    fun backupNow(onDone: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveState(snapshot())
            val file = repository.backupNow()
            withContext(Dispatchers.Main) { onDone("Backup created: $file") }
        }
    }

    fun backupToLocalDownloads(onDone: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.saveState(snapshot())
                repository.backupNow() // keep an app-local .bk for local restore flow
                repository.backupToDownloads()
            }.onSuccess { fileName ->
                withContext(Dispatchers.Main) { onDone("Backup saved in Downloads as $fileName") }
            }.onFailure {
                withContext(Dispatchers.Main) { onDone("Local backup failed: ${it.message}") }
            }
        }
    }

    fun backupByDestination(destination: String, onDone: (String) -> Unit) {
        if (destination.equals("Google Drive", ignoreCase = true)) {
            backupToGoogle(onDone)
        } else {
            backupNow(onDone)
        }
    }

    fun restoreLatest(onDone: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = repository.restoreLatest()
            val loaded = if (ok) repository.loadState() else null
            withContext(Dispatchers.Main) {
                if (loaded != null) {
                    applyState(loaded)
                    onDone("Restored latest backup successfully.")
                } else {
                    onDone("No backup available to restore.")
                }
            }
        }
    }

    fun restoreByDestination(destination: String, onDone: (String) -> Unit) {
        if (destination.equals("Google Drive", ignoreCase = true)) {
            restoreFromGoogle(onDone)
        } else {
            restoreLatest(onDone)
        }
    }

    fun restoreFromLocalFile(uri: Uri, onDone: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = repository.restoreFromUri(uri)
            val loaded = if (ok) repository.loadState() else null
            withContext(Dispatchers.Main) {
                if (loaded != null) {
                    applyState(loaded)
                    onDone("Restored backup file successfully.")
                } else {
                    onDone("Could not restore selected backup file.")
                }
            }
        }
    }

    fun exportTransactions(onDone: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val path = repository.exportCsv(transactions)
            withContext(Dispatchers.Main) { onDone("Exported CSV: $path") }
        }
    }

    fun importTransactions(onDone: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val incoming = repository.importCsv()
            withContext(Dispatchers.Main) {
                if (incoming.isEmpty()) {
                    onDone("No import file found or file is empty.")
                } else {
                    transactions.addAll(incoming)
                    persist()
                    onDone("Imported ${incoming.size} transactions.")
                }
            }
        }
    }

    fun handleGoogleSignInResult(resultCode: Int, data: Intent?, onDone: (String) -> Unit) {
        val parsed = authManager.parseSignInResult(data)
        val account = parsed.account ?: authManager.getLastAccount()
        account?.let {
            signedInEmail = it.email
            signedInName = it.displayName ?: it.email
            isGoogleSignedIn = true
        }
        if (resultCode == -1 && isGoogleSignedIn) {
            signInStatusMessage = ""
            onDone(signInStatusMessage)
        } else {
            isGoogleSignedIn = false
            val details = parsed.error ?: "No account returned from Google Sign-In."
            signInStatusMessage = "Sign-in failed. $details. AppId=$appPackageId SHA1=$appSigningSha1"
            onDone(signInStatusMessage)
        }
    }

    fun handleGoogleSignIn(onDone: (String) -> Unit) {
        authManager.getLastAccount()?.let { account ->
            signedInEmail = account.email
            signedInName = account.displayName ?: account.email
            isGoogleSignedIn = true
        }
        onDone(if (!isGoogleSignedIn) "Google sign-in not completed." else "")
    }

    fun signOutGoogle(onDone: (String) -> Unit) {
        authManager.signOut()
        signedInEmail = null
        signedInName = null
        isGoogleSignedIn = false
        signInStatusMessage = "Signed out from Google."
        onDone("Signed out from Google.")
    }

    fun googleSignInIntent() = authManager.signInClient.signInIntent

    fun backupToGoogle(onDone: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val account = authManager.getSignedInAccount()
            if (account == null) {
                withContext(Dispatchers.Main) { onDone("Sign in with Google first.") }
                return@launch
            }
            runCatching {
                repository.saveState(snapshot())
                driveBackupManager.uploadBackup(account, repository.getStateFile())
            }.onSuccess {
                withContext(Dispatchers.Main) { onDone("Backup synced to Google Drive.") }
            }.onFailure {
                withContext(Dispatchers.Main) { onDone("Google backup failed: ${it.message}") }
            }
        }
    }

    fun restoreFromGoogle(onDone: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val account = authManager.getSignedInAccount()
            if (account == null) {
                withContext(Dispatchers.Main) { onDone("Sign in with Google first.") }
                return@launch
            }
            runCatching {
                driveBackupManager.restoreBackup(account, repository.getStateFile())
            }.onSuccess { restored ->
                withContext(Dispatchers.Main) {
                    if (restored) {
                        repository.loadState()?.let(::applyState)
                        onDone("Restored latest backup from Google Drive.")
                    } else {
                        onDone("No Google backup found.")
                    }
                }
            }.onFailure {
                withContext(Dispatchers.Main) { onDone("Google restore failed: ${it.message}") }
            }
        }
    }

    fun spendingTotal(): Double = transactions.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
    fun incomeTotal(): Double = transactions.filter { it.type == TransactionType.Income }.sumOf { it.amount }
    fun netTotal(): Double = incomeTotal() - spendingTotal()

    fun accountBalance(account: Account): Double {
        val accountNet = transactions
            .filter { it.paymentMode.equals(account.name, ignoreCase = true) }
            .sumOf {
                when (it.type) {
                    TransactionType.Income -> it.amount
                    TransactionType.Expense -> -it.amount
                    TransactionType.Transfer -> 0.0
                }
            }
        return account.openingBalance + accountNet
    }

    fun totalAccountBalance(): Double = accounts.sumOf { accountBalance(it) }

    fun monthTransactions(date: LocalDate = LocalDate.now()): List<ExpenseTransaction> {
        return transactions.filter { it.date.month == date.month && it.date.year == date.year }
    }

    fun dayTransactions(date: LocalDate): List<ExpenseTransaction> {
        return transactions.filter { it.date == date }
    }

    fun setSignInStatus(message: String) {
        signInStatusMessage = message
    }

    private fun normalizeTag(raw: String): String? {
        val compact = raw.trim().replace(" ", "")
        if (compact.isBlank()) return null
        val withoutHash = compact.trimStart('#')
        if (withoutHash.isBlank()) return null
        return "#${withoutHash.lowercase()}"
    }

    private fun readSigningSha1(application: Application): String {
        return runCatching {
            val pm = application.packageManager
            val pkg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(application.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(application.packageName, PackageManager.GET_SIGNATURES)
            }
            val certBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                pkg.signatures?.firstOrNull()?.toByteArray()
            } ?: return "unknown"

            MessageDigest.getInstance("SHA-1")
                .digest(certBytes)
                .joinToString(":") { "%02X".format(it) }
        }.getOrDefault("unknown")
    }
}


