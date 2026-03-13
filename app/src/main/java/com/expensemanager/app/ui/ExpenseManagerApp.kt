package com.expensemanager.app.ui

import android.app.DatePickerDialog

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expensemanager.app.AppViewModel
import com.expensemanager.app.data.AnalysisPeriod
import com.expensemanager.app.data.BudgetType
import com.expensemanager.app.data.ExpenseTransaction
import com.expensemanager.app.data.TransactionType
import com.expensemanager.app.ui.theme.ExpenseManagerTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private object Routes {
    const val Home = "home"
    const val Analysis = "analysis"
    const val Accounts = "accounts"
    const val More = "more"
    const val Add = "add"
    const val Transactions = "transactions/{filter}"
    const val TransactionFilterArg = "filter"
    const val TransactionFilterAll = "all"
    const val TransactionFilterExpense = "expense"
    const val TransactionFilterIncome = "income"
    const val Categories = "categories"
    const val Tags = "tags"
    const val DayView = "dayview"
    const val CalendarView = "calendar"
    const val Settings = "settings"

    fun transactionsRoute(filter: String = TransactionFilterAll): String = "transactions/$filter"
}

private enum class HomeRange(val label: String) {
    AllTime("All Time"),
    Today("Today"),
    ThisWeek("This Week"),
    ThisMonth("This Month"),
    ThisYear("This Year")
}

@Composable
fun ExpenseManagerApp(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val googleSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.handleGoogleSignInResult(it.resultCode, it.data) {}
    }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Routes.Home
    val rootTabs = listOf(Routes.Home, Routes.Analysis, Routes.Accounts, Routes.More)

    ExpenseManagerTheme(themeMode = vm.settings.theme) {
        Scaffold(
            bottomBar = {
                if (route in rootTabs) {
                    BottomNav(route, nav)
                }
            },
            floatingActionButton = {
                if (route != Routes.Add) {
                    FloatingActionButton(onClick = { nav.navigate(Routes.Add) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = Routes.Home,
                modifier = Modifier.padding(padding)
            ) {
                composable(Routes.Home) { HomeScreen(vm, nav) }
                composable(Routes.Analysis) { AnalysisScreen(vm) }
                composable(Routes.Accounts) { AccountsScreen(vm) }
                composable(Routes.More) { MoreScreen(vm, nav) { googleSignInLauncher.launch(vm.googleSignInIntent()) } }
                composable(Routes.Add) { AddTransactionScreen(vm, nav) }
                composable(Routes.Transactions) { backStackEntry ->
                    val filter = backStackEntry.arguments?.getString(Routes.TransactionFilterArg) ?: Routes.TransactionFilterAll
                    TransactionsScreen(vm, nav, filter)
                }
                composable(Routes.Categories) { CategoriesScreen(vm, nav) }
                composable(Routes.Tags) { TagsScreen(vm, nav) }
                composable(Routes.DayView) { DayViewScreen(vm, nav) }
                composable(Routes.CalendarView) { CalendarViewScreen(vm, nav) }
                composable(Routes.Settings) { SettingsScreen(vm, nav) }
            }
        }
    }
}

@Composable
private fun BottomNav(route: String, nav: NavHostController) {
    NavigationBar {
        NavigationBarItem(selected = route == Routes.Home, onClick = { nav.navigate(Routes.Home) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
        NavigationBarItem(selected = route == Routes.Analysis, onClick = { nav.navigate(Routes.Analysis) }, icon = { Icon(Icons.Default.Analytics, null) }, label = { Text("Analysis") })
        NavigationBarItem(selected = route == Routes.Accounts, onClick = { nav.navigate(Routes.Accounts) }, icon = { Icon(Icons.Default.AccountBalance, null) }, label = { Text("Accounts") })
        NavigationBarItem(selected = route == Routes.More, onClick = { nav.navigate(Routes.More) }, icon = { Icon(Icons.Default.MoreHoriz, null) }, label = { Text("More") })
    }
}

@Composable
private fun HomeScreen(vm: AppViewModel, nav: NavHostController) {
    var selectedRange by remember { mutableStateOf(HomeRange.ThisMonth) }
    val filtered = remember(vm.transactions.toList(), selectedRange) {
        filterTransactionsByRange(vm.transactions.toList(), selectedRange)
    }
    val spending = filtered.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
    val income = filtered.filter { it.type == TransactionType.Income }.sumOf { it.amount }
    val net = income - spending
    val userName = if (vm.isGoogleSignedIn) (vm.signedInName ?: vm.signedInEmail ?: "Signed In User") else "Guest User"
    val currencySymbol = currencySymbolFromSetting(vm.settings.currency)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Hello,", style = MaterialTheme.typography.titleMedium)
                    Text(userName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("CASH FLOW", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                HomeRangeDropdown(selectedRange = selectedRange, onRangeSelected = { selectedRange = it })
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    title = "SPENDING",
                    amount = spending,
                    symbol = currencySymbol,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFFFEBEE),
                    onClick = { nav.navigate(Routes.transactionsRoute(Routes.TransactionFilterExpense)) }
                )
                StatCard(
                    title = "INCOME",
                    amount = income,
                    symbol = currencySymbol,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFE8F5E9),
                    onClick = { nav.navigate(Routes.transactionsRoute(Routes.TransactionFilterIncome)) }
                )
            }
            Spacer(Modifier.height(8.dp))
            StatCard(
                title = "Net Balance",
                amount = net,
                symbol = currencySymbol,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFE3F2FD),
                onClick = { nav.navigate(Routes.transactionsRoute(Routes.TransactionFilterAll)) }
            )
            Spacer(Modifier.height(20.dp))
            Text("Quick Setup Guide", fontWeight = FontWeight.SemiBold)
        }
        item { SetupItem("Customize your categories", { nav.navigate(Routes.Categories) }) }
        item { SetupItem("Set up your accounts", { nav.navigate(Routes.Accounts) }) }
        item { SetupItem("Add transaction", { nav.navigate(Routes.Add) }) }
        item { SetupItem("View transactions", { nav.navigate(Routes.transactionsRoute()) }) }
        item {
            Spacer(Modifier.height(14.dp))
            CashFlowInsightsCard(
                spending = spending,
                income = income,
                net = net
            )
            Spacer(Modifier.height(10.dp))
            CategoryInsightsCard(
                filteredTransactions = filtered,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeRangeDropdown(selectedRange: HomeRange, onRangeSelected: (HomeRange) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        TextButton(onClick = { expanded = true }, modifier = Modifier.menuAnchor()) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null)
            Text(selectedRange.label)
        }
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            HomeRange.entries.forEach { range ->
                DropdownMenuItem(
                    text = { Text(range.label) },
                    onClick = {
                        onRangeSelected(range)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun filterTransactionsByRange(transactions: List<ExpenseTransaction>, range: HomeRange): List<ExpenseTransaction> {
    val today = LocalDate.now()
    return when (range) {
        HomeRange.AllTime -> transactions
        HomeRange.Today -> transactions.filter { it.date == today }
        HomeRange.ThisWeek -> {
            val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val end = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            transactions.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
        }
        HomeRange.ThisMonth -> transactions.filter { it.date.month == today.month && it.date.year == today.year }
        HomeRange.ThisYear -> transactions.filter { it.date.year == today.year }
    }
}

@Composable
private fun CashFlowInsightsCard(
    spending: Double,
    income: Double,
    net: Double
) {
    val maxValue = listOf(spending, income, abs(net)).maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val spendingHeight = ((spending / maxValue) * 130.0).toFloat().dp
    val incomeHeight = ((income / maxValue) * 130.0).toFloat().dp
    val netHeight = ((abs(net) / maxValue) * 130.0).toFloat().dp

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Cash Flow Insights", fontWeight = FontWeight.Bold)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(178.dp)
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                InsightBar("Spending", spendingHeight, Color(0xFFEF5350), "$${"%.0f".format(spending)}", Modifier.weight(1f))
                InsightBar("Income", incomeHeight, Color(0xFF66BB6A), "$${"%.0f".format(income)}", Modifier.weight(1f))
                InsightBar("Net", netHeight, if (net >= 0) Color(0xFF42A5F5) else Color(0xFFFFA726), "$${"%.0f".format(net)}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CategoryInsightsCard(filteredTransactions: List<ExpenseTransaction>) {
    val expenseSlices = filteredTransactions
        .filter { it.type == TransactionType.Expense }
        .groupBy { it.category }
        .map { (category, txns) -> category to txns.sumOf { it.amount } }
        .sortedByDescending { it.second }
        .mapIndexed { index, (category, value) ->
            PieSlice(category, value, pieColors[index % pieColors.size])
        }

    val incomeSlices = filteredTransactions
        .filter { it.type == TransactionType.Income }
        .groupBy { it.category }
        .map { (category, txns) -> category to txns.sumOf { it.amount } }
        .sortedByDescending { it.second }
        .mapIndexed { index, (category, value) ->
            PieSlice(category, value, pieColors[index % pieColors.size])
        }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Category Insights", fontWeight = FontWeight.Bold)
            PieChartSection("Spending by Category", expenseSlices)
            PieChartSection("Income by Category", incomeSlices)
        }
    }
}

@Composable
private fun InsightBar(
    label: String,
    height: androidx.compose.ui.unit.Dp,
    color: Color,
    amount: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(amount, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        Box(
            Modifier
                .size(width = 42.dp, height = height.coerceAtLeast(6.dp))
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun PieChartSection(title: String, slices: List<PieSlice>) {
    Text(title, fontWeight = FontWeight.SemiBold)
    if (slices.isEmpty()) {
        Text("No data", color = Color.Gray)
        return
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PieChart(
            slices = slices,
            modifier = Modifier.size(130.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            slices.take(5).forEach { slice ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(slice.color)
                    )
                    Text("${slice.label}: $${"%.2f".format(slice.value)}", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (slices.size > 5) {
                Text("+${slices.size - 5} more", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun PieChart(slices: List<PieSlice>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1.0)
    Canvas(modifier = modifier) {
        var startAngle = -90f
        slices.forEach { slice ->
            val sweep = ((slice.value / total) * 360f).toFloat()
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true
            )
            startAngle += sweep
        }
    }
}

private data class PieSlice(
    val label: String,
    val value: Double,
    val color: Color
)

private val pieColors = listOf(
    Color(0xFFEF5350),
    Color(0xFF42A5F5),
    Color(0xFF66BB6A),
    Color(0xFFFFA726),
    Color(0xFFAB47BC),
    Color(0xFF26A69A),
    Color(0xFFFF7043),
    Color(0xFF7E57C2)
)

@Composable
private fun AnalysisScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy")
    var monthCursor by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var weekCursor by remember { mutableStateOf(LocalDate.now()) }
    var yearCursor by remember { mutableStateOf(LocalDate.now().year) }
    var customStartDate by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var customEndDate by remember { mutableStateOf(LocalDate.now()) }

    val weekStart = weekCursor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekEnd = weekCursor.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val customStart = if (customStartDate <= customEndDate) customStartDate else customEndDate
    val customEnd = if (customStartDate <= customEndDate) customEndDate else customStartDate

    val filteredTransactions = remember(
        vm.transactions.toList(),
        vm.analysisPeriod,
        weekCursor,
        monthCursor,
        yearCursor,
        customStartDate,
        customEndDate
    ) {
        vm.transactions.filter { txn ->
            when (vm.analysisPeriod) {
                AnalysisPeriod.Week -> !txn.date.isBefore(weekStart) && !txn.date.isAfter(weekEnd)
                AnalysisPeriod.Month -> txn.date.month == monthCursor.month && txn.date.year == monthCursor.year
                AnalysisPeriod.Year -> txn.date.year == yearCursor
                AnalysisPeriod.Custom -> !txn.date.isBefore(customStart) && !txn.date.isAfter(customEnd)
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Analysis", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AnalysisPeriod.values().forEach { p ->
                val selected = vm.analysisPeriod == p
                Button(
                    onClick = { vm.updateAnalysisPeriod(p) },
                    modifier = Modifier.weight(1f),
                    enabled = !selected,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = p.name,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        when (vm.analysisPeriod) {
            AnalysisPeriod.Week -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { weekCursor = weekCursor.minusWeeks(1) }) { Text("<") }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${weekStart.format(dateFormat)} - ${weekEnd.format(dateFormat)}")
                        Text("${filteredTransactions.size} TRANSACTIONS", color = Color.Gray)
                    }
                    TextButton(onClick = { weekCursor = weekCursor.plusWeeks(1) }) { Text(">") }
                }
            }

            AnalysisPeriod.Month -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { monthCursor = monthCursor.minusMonths(1) }) { Text("<") }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${monthCursor.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${monthCursor.year}")
                        Text("${filteredTransactions.size} TRANSACTIONS", color = Color.Gray)
                    }
                    TextButton(onClick = { monthCursor = monthCursor.plusMonths(1) }) { Text(">") }
                }
            }

            AnalysisPeriod.Year -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { yearCursor -= 1 }) { Text("<") }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$yearCursor")
                        Text("${filteredTransactions.size} TRANSACTIONS", color = Color.Gray)
                    }
                    TextButton(onClick = { yearCursor += 1 }) { Text(">") }
                }
            }

            AnalysisPeriod.Custom -> {
                OutlinedTextField(
                    value = customStart.format(dateFormat),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("From Date") },
                    trailingIcon = {
                        IconButton(onClick = {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> customStartDate = LocalDate.of(y, m + 1, d) },
                                customStart.year,
                                customStart.monthValue - 1,
                                customStart.dayOfMonth
                            ).show()
                        }) { Icon(Icons.Default.CalendarMonth, contentDescription = "Pick from date") }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customEnd.format(dateFormat),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("To Date") },
                    trailingIcon = {
                        IconButton(onClick = {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> customEndDate = LocalDate.of(y, m + 1, d) },
                                customEnd.year,
                                customEnd.monthValue - 1,
                                customEnd.dayOfMonth
                            ).show()
                        }) { Icon(Icons.Default.CalendarMonth, contentDescription = "Pick to date") }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("${filteredTransactions.size} TRANSACTIONS", color = Color.Gray)
            }
        }

        Spacer(Modifier.height(24.dp))
        if (filteredTransactions.isEmpty()) {
            Text("No transactions")
        } else {
            itemsBlock(filteredTransactions.map { "${it.category}: $${"%.2f".format(it.amount)}" })
        }
    }
}

@Composable
private fun AccountsScreen(vm: AppViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Cash") }
    var opening by remember { mutableStateOf("0") }
    val currencySymbol = currencySymbolFromSetting(vm.settings.currency)

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("All Accounts", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showAdd = true }) { Text("Add account") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Show balance")
                Switch(checked = vm.settings.showBalances, onCheckedChange = { vm.updateSettings(vm.settings.copy(showBalances = it)) })
            }
            Spacer(Modifier.height(8.dp))
            StatCard("Available Balance", vm.totalAccountBalance(), currencySymbol, Modifier.fillMaxWidth(), Color(0xFFE3F2FD))
        }
        items(vm.accounts) { a ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(a.name, fontWeight = FontWeight.SemiBold)
                            Text(a.type)
                        }
                        if (!a.name.equals("Cash", ignoreCase = true)) {
                            TextButton(onClick = { vm.deleteAccount(a.id) }) { Text("Delete") }
                        }
                    }
                    val balance = vm.accountBalance(a)
                    val text = if (vm.settings.showBalances) "$${"%.2f".format(balance)}" else "*****"
                    Text(text, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add account") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type") })
                    OutlinedTextField(value = opening, onValueChange = { opening = it }, label = { Text("Opening balance") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addAccount(name, type, opening.toDoubleOrNull() ?: 0.0)
                    showAdd = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MoreScreen(vm: AppViewModel, nav: NavHostController, onGoogleSignIn: () -> Unit) {
    val userName = if (vm.isGoogleSignedIn) (vm.signedInName ?: vm.signedInEmail ?: "Signed In User") else "Guest User"
    val items = listOf(
        "Transactions" to Routes.transactionsRoute(),
        "Categories" to Routes.Categories,
        "Tags" to Routes.Tags,
        "Day" to Routes.DayView,
        "Calendar" to Routes.CalendarView,
        "Settings" to Routes.Settings
    )
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text(userName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = {
                    if (!vm.isGoogleSignedIn) {
                        onGoogleSignIn()
                    } else {
                        vm.signOutGoogle {}
                    }
                }) { Text(if (!vm.isGoogleSignedIn) "Sign in" else "Sign out") }
                TextButton(onClick = { vm.backupToGoogle {} }) { Text("Backup now") }
            }
            Text("Last backup: No backups created.", color = Color.Gray)
            Spacer(Modifier.height(10.dp))
        }
        items(items.chunked(2)) { chunk ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chunk.forEach { (label, route) ->
                    FeatureTile(label = label, modifier = Modifier.weight(1f)) { nav.navigate(route) }
                }
                if (chunk.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionScreen(vm: AppViewModel, nav: NavHostController) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(TransactionType.Expense) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(vm.settings.defaultCategory) }
    var payment by remember { mutableStateOf(vm.settings.defaultPaymentMode) }
    var note by remember { mutableStateOf("") }
    var tagText by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val selectedTime = LocalTime.now()
    val currencySymbol = currencySymbolFromSetting(vm.settings.currency)
    val availableCategories = when (type) {
        TransactionType.Expense -> if (vm.expenseCategories.isNotEmpty()) vm.expenseCategories.toList() else listOf("Others")
        TransactionType.Income -> if (vm.incomeCategories.isNotEmpty()) vm.incomeCategories.toList() else listOf("Salary")
        TransactionType.Transfer -> listOf("Transfer")
    }
    val accountOptions = vm.accounts.map { it.name }.distinct().ifEmpty { listOf("Cash") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var paymentExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(type, availableCategories, accountOptions) {
        if (category !in availableCategories) {
            category = if (vm.settings.defaultCategory in availableCategories) {
                vm.settings.defaultCategory
            } else {
                availableCategories.firstOrNull() ?: "Others"
            }
        }
        if (payment !in accountOptions) {
            payment = accountOptions.first()
        }
    }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Add transaction") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
        )
    }) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(TransactionType.Expense, TransactionType.Income).forEach { selectedType ->
                    Button(
                        onClick = {
                            type = selectedType
                            val categories = when (selectedType) {
                                TransactionType.Expense -> if (vm.expenseCategories.isNotEmpty()) vm.expenseCategories.toList() else listOf("Others")
                                TransactionType.Income -> if (vm.incomeCategories.isNotEmpty()) vm.incomeCategories.toList() else listOf("Salary")
                                TransactionType.Transfer -> listOf("Transfer")
                            }
                            if (category !in categories) {
                                category = if (vm.settings.defaultCategory in categories) {
                                    vm.settings.defaultCategory
                                } else {
                                    categories.firstOrNull() ?: "Others"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = type != selectedType
                    ) { Text(selectedType.name) }
                }
            }
            OutlinedTextField(
                value = selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                trailingIcon = {
                    IconButton(onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                selectedDate = LocalDate.of(year, month + 1, day)
                            },
                            selectedDate.year,
                            selectedDate.monthValue - 1,
                            selectedDate.dayOfMonth
                        ).show()
                    }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = formatTimeBySettings(selectedTime, vm.settings.timeFormat),
                onValueChange = {},
                readOnly = true,
                label = { Text("Time") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount ($currencySymbol)") }, modifier = Modifier.fillMaxWidth())
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    availableCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(expanded = paymentExpanded, onExpandedChange = { paymentExpanded = !paymentExpanded }) {
                OutlinedTextField(
                    value = payment,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment mode") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = paymentExpanded, onDismissRequest = { paymentExpanded = false }) {
                    accountOptions.forEach { accountName ->
                        DropdownMenuItem(
                            text = { Text(accountName) },
                            onClick = {
                                payment = accountName
                                paymentExpanded = false
                            }
                        )
                    }
                }
            }
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Other details") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = tagText,
                onValueChange = { tagText = it },
                label = { Text("Add tags (#food #rent)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                val selectedTags = parseTagTokens(tagText)
                vm.addTransaction(
                    type = type,
                    amount = amount.toDoubleOrNull() ?: 0.0,
                    category = category,
                    paymentMode = payment,
                    note = note,
                    selectedTags = selectedTags,
                    date = selectedDate,
                    time = selectedTime
                )
                nav.popBackStack()
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Save transaction")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionsScreen(vm: AppViewModel, nav: NavHostController, initialFilter: String) {
    val allTransactions = vm.transactions.toList().sortedWith(
        compareByDescending<ExpenseTransaction> { it.date }.thenByDescending { it.time }
    )
    val earliestDate = allTransactions.minOfOrNull { it.date } ?: LocalDate.now()
    val latestDate = allTransactions.maxOfOrNull { it.date } ?: LocalDate.now()
    val initialType = when (initialFilter.lowercase()) {
        Routes.TransactionFilterExpense -> TransactionType.Expense
        Routes.TransactionFilterIncome -> TransactionType.Income
        else -> null
    }

    var selectedType by remember(initialType) { mutableStateOf<TransactionType?>(initialType) }
    var startDate by remember(earliestDate) { mutableStateOf(earliestDate) }
    var endDate by remember(latestDate) { mutableStateOf(latestDate) }
    var selectedCategory by remember { mutableStateOf("All Categories") }
    var selectedTag by remember { mutableStateOf("All Tags") }
    val context = LocalContext.current

    val categories = listOf("All Categories") + (vm.expenseCategories + vm.incomeCategories + allTransactions.map { it.category })
        .distinct()
        .sorted()
    val tags = listOf("All Tags") + (vm.tags + allTransactions.flatMap { it.tags })
        .distinct()
        .sorted()

    val rangeStart = if (startDate <= endDate) startDate else endDate
    val rangeEnd = if (startDate <= endDate) endDate else startDate

    val filteredTransactions = allTransactions.filter { txn ->
        val matchesType = selectedType == null || txn.type == selectedType
        val matchesCategory = selectedCategory == "All Categories" || txn.category == selectedCategory
        val matchesTag = selectedTag == "All Tags" || txn.tags.any { it.equals(selectedTag, ignoreCase = true) }
        val matchesDate = !txn.date.isBefore(rangeStart) && !txn.date.isAfter(rangeEnd)
        matchesType && matchesCategory && matchesTag && matchesDate
    }

    CommonScreen("Transactions", nav) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { selectedType = null },
                modifier = Modifier.weight(1f),
                enabled = selectedType != null
            ) { Text("All") }
            Button(
                onClick = { selectedType = TransactionType.Expense },
                modifier = Modifier.weight(1f),
                enabled = selectedType != TransactionType.Expense
            ) { Text("Expense") }
            Button(
                onClick = { selectedType = TransactionType.Income },
                modifier = Modifier.weight(1f),
                enabled = selectedType != TransactionType.Income
            ) { Text("Income") }
        }

        OutlinedTextField(
            value = rangeStart.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
            onValueChange = {},
            readOnly = true,
            label = { Text("From Date") },
            trailingIcon = {
                IconButton(onClick = {
                    DatePickerDialog(
                        context,
                        { _, y, m, d -> startDate = LocalDate.of(y, m + 1, d) },
                        rangeStart.year,
                        rangeStart.monthValue - 1,
                        rangeStart.dayOfMonth
                    ).show()
                }) { Icon(Icons.Default.CalendarMonth, contentDescription = "Pick from date") }
            },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = rangeEnd.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
            onValueChange = {},
            readOnly = true,
            label = { Text("To Date") },
            trailingIcon = {
                IconButton(onClick = {
                    DatePickerDialog(
                        context,
                        { _, y, m, d -> endDate = LocalDate.of(y, m + 1, d) },
                        rangeEnd.year,
                        rangeEnd.monthValue - 1,
                        rangeEnd.dayOfMonth
                    ).show()
                }) { Icon(Icons.Default.CalendarMonth, contentDescription = "Pick to date") }
            },
            modifier = Modifier.fillMaxWidth()
        )

        CategoryFilterDropdown(
            selected = selectedCategory,
            categories = categories,
            onSelect = { selectedCategory = it }
        )
        TagFilterDropdown(
            selected = selectedTag,
            tags = tags,
            onSelect = { selectedTag = it }
        )

        Text("${filteredTransactions.size} transaction(s)")

        if (filteredTransactions.isEmpty()) {
            Text("No transactions found for selected filters.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                filteredTransactions.forEach {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${it.type} - ${it.category}", fontWeight = FontWeight.SemiBold)
                            Text("$${"%.2f".format(it.amount)} | ${it.paymentMode}")
                            Text("${it.date} ${formatTimeBySettings(it.time, vm.settings.timeFormat)}")
                            if (it.tags.isNotEmpty()) Text(it.tags.joinToString(" "))
                            if (it.note.isNotBlank()) Text(it.note)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduledScreen(vm: AppViewModel, nav: NavHostController) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    CommonScreen("Scheduled Transactions", nav) {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            vm.addScheduled(title, amount.toDoubleOrNull() ?: 0.0, TransactionType.Expense, "Monthly", LocalDate.now().plusDays(7))
            title = ""
            amount = ""
        }) { Text("Add schedule") }
        Spacer(Modifier.height(10.dp))
        if (vm.scheduled.isEmpty()) Text("No Scheduled Transactions Set")
        vm.scheduled.forEach {
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(it.title, fontWeight = FontWeight.SemiBold)
                    Text("$${"%.2f".format(it.amount)} • ${it.frequency}")
                    Text("Next: ${it.nextDate}")
                }
            }
        }
    }
}

@Composable
private fun BudgetsScreen(vm: AppViewModel, nav: NavHostController) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(BudgetType.Monthly) }
    CommonScreen("Budgets", nav) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { type = BudgetType.Monthly }, enabled = type != BudgetType.Monthly) { Text("Monthly") }
            Button(onClick = { type = BudgetType.Yearly }, enabled = type != BudgetType.Yearly) { Text("Yearly") }
        }
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Budget name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            vm.addBudget(name, amount.toDoubleOrNull() ?: 0.0, type)
            name = ""
            amount = ""
        }) { Text("Set Up Budget") }
        if (vm.budgets.isEmpty()) Text("Set Up Your First ${type.name} Budget")
        vm.budgets.forEach {
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(it.name, fontWeight = FontWeight.SemiBold)
                    Text("$${"%.2f".format(it.amount)} • ${it.budgetType.name}")
                }
            }
        }
    }
}

@Composable
private fun CategoriesScreen(vm: AppViewModel, nav: NavHostController) {
    var incomeMode by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf("") }
    val list = if (incomeMode) vm.incomeCategories else vm.expenseCategories

    CommonScreen("Categories", nav) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { incomeMode = false }, enabled = incomeMode) { Text("Expense") }
            Button(onClick = { incomeMode = true }, enabled = !incomeMode) { Text("Income") }
        }
        OutlinedTextField(value = newCategory, onValueChange = { newCategory = it }, label = { Text("Add category") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            vm.addCategory(newCategory, incomeMode)
            newCategory = ""
        }) { Text("Save") }
        Text("Default Category: ${vm.settings.defaultCategory}")
        list.forEach { cat ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(cat, Modifier.padding(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagsScreen(vm: AppViewModel, nav: NavHostController) {
    var expanded by remember { mutableStateOf(false) }
    var selectedTags by remember { mutableStateOf(listOf<String>()) }
    val allTags = (vm.tags + vm.transactions.flatMap { it.tags }).distinct().sorted()
    val filteredTransactions = vm.transactions
        .sortedWith(compareByDescending<ExpenseTransaction> { it.date }.thenByDescending { it.time })
        .filter { txn ->
            selectedTags.isEmpty() || txn.tags.any { it in selectedTags }
        }

    CommonScreen("Tags", nav) {
        if (allTags.isEmpty()) {
            Text("No tags available yet.")
            Text("Add tags while creating transactions, then filter here.")
            return@CommonScreen
        }

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = if (selectedTags.isEmpty()) "Select tags" else selectedTags.joinToString(" "),
                onValueChange = {},
                readOnly = true,
                label = { Text("Filter by Tags") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                allTags.forEach { tag ->
                    val isSelected = tag in selectedTags
                    DropdownMenuItem(
                        text = { Text(if (isSelected) "✓ $tag" else tag) },
                        onClick = {
                            selectedTags = if (isSelected) selectedTags - tag else selectedTags + listOf(tag)
                        }
                    )
                }
            }
        }

        if (selectedTags.isNotEmpty()) {
            Text("Selected: ${selectedTags.joinToString(" ")}", color = Color.Gray)
            TextButton(onClick = { selectedTags = emptyList() }) { Text("Clear selection") }
        }

        if (filteredTransactions.isEmpty()) {
            Text("No transactions found for selected tags.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                filteredTransactions.forEach {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${it.type} - ${it.category}", fontWeight = FontWeight.SemiBold)
                            Text("$${"%.2f".format(it.amount)} | ${it.paymentMode}")
                            Text("${it.date} ${formatTimeBySettings(it.time, vm.settings.timeFormat)}")
                            if (it.tags.isNotEmpty()) Text(it.tags.joinToString(" "))
                            if (it.note.isNotBlank()) Text(it.note)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayViewScreen(vm: AppViewModel, nav: NavHostController) {
    val context = LocalContext.current
    var date by remember { mutableStateOf(LocalDate.now()) }
    val txns = vm.dayTransactions(date)
    CommonScreen("Day View", nav) {
        OutlinedTextField(
            value = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Date") },
            trailingIcon = {
                IconButton(onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day -> date = LocalDate.of(year, month + 1, day) },
                        date.year,
                        date.monthValue - 1,
                        date.dayOfMonth
                    ).show()
                }) { Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date") }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Text("${date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }} • ${txns.size} TRANSACTIONS", color = Color.Gray)
        if (txns.isEmpty()) {
            Text("No transactions for selected date.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                txns.forEach {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${it.type} - ${it.category}", fontWeight = FontWeight.SemiBold)
                            Text("$${"%.2f".format(it.amount)} | ${it.paymentMode}")
                            Text("${it.date} ${formatTimeBySettings(it.time, vm.settings.timeFormat)}")
                            if (it.tags.isNotEmpty()) Text(it.tags.joinToString(" "))
                            if (it.note.isNotBlank()) Text(it.note)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarViewScreen(vm: AppViewModel, nav: NavHostController) {
    val today = LocalDate.now()
    var monthCursor by remember { mutableStateOf(today.withDayOfMonth(1)) }
    var selectedDate by remember { mutableStateOf(today) }
    val firstDayOffset = monthCursor.dayOfWeek.value % 7
    val daysInMonth = monthCursor.lengthOfMonth()
    val totalCells = ((firstDayOffset + daysInMonth + 6) / 7) * 7
    val monthTxns = vm.monthTransactions(monthCursor)
    val selectedTxns = vm.dayTransactions(selectedDate)

    CommonScreen("Calendar View", nav) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                monthCursor = monthCursor.minusMonths(1).withDayOfMonth(1)
                if (selectedDate.month != monthCursor.month || selectedDate.year != monthCursor.year) {
                    selectedDate = monthCursor
                }
            }) { Text("<") }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${monthCursor.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${monthCursor.year}")
                Text("${monthTxns.size} TRANSACTIONS", color = Color.Gray)
            }
            TextButton(onClick = {
                monthCursor = monthCursor.plusMonths(1).withDayOfMonth(1)
                if (selectedDate.month != monthCursor.month || selectedDate.year != monthCursor.year) {
                    selectedDate = monthCursor
                }
            }) { Text(">") }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Text(day, modifier = Modifier.size(42.dp), color = Color.Gray)
            }
        }
        Spacer(Modifier.height(10.dp))
        repeat(totalCells / 7) { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(7) { day ->
                    val cellIndex = week * 7 + day
                    val num = cellIndex - firstDayOffset + 1
                    val validDay = num in 1..daysInMonth
                    val cellDate = if (validDay) monthCursor.withDayOfMonth(num) else null
                    val isSelected = cellDate == selectedDate
                    val isToday = cellDate == today
                    Box(
                        Modifier
                            .size(42.dp)
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else -> Color.Transparent
                                }
                            )
                            .clickable(enabled = validDay) { selectedDate = cellDate!! },
                        contentAlignment = Alignment.Center
                    ) { Text(if (validDay) "$num" else "") }
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        Spacer(Modifier.height(8.dp))
        Text("${selectedDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}, $selectedDate", fontWeight = FontWeight.SemiBold)
        if (selectedTxns.isEmpty()) {
            Text("No transactions for selected date.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                selectedTxns.forEach {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${it.type} - ${it.category}", fontWeight = FontWeight.SemiBold)
                            Text("$${"%.2f".format(it.amount)} | ${it.paymentMode}")
                            if (it.tags.isNotEmpty()) Text(it.tags.joinToString(" "))
                            if (it.note.isNotBlank()) Text(it.note)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(vm: AppViewModel, nav: NavHostController) {
    var state by remember { mutableStateOf(vm.settings) }
    var actionMessage by remember { mutableStateOf("") }
    var showBackupChooser by remember { mutableStateOf(false) }
    var showRestoreChooser by remember { mutableStateOf(false) }
    var restoreSource by remember { mutableStateOf("File Manager") }
    val localRestoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            vm.restoreFromLocalFile(uri) { actionMessage = it }
        } else {
            actionMessage = "No file selected from $restoreSource."
        }
    }
    val currencies = listOf(
        "USD ($)",
        "EUR (€)",
        "GBP (£)",
        "INR (₹)",
        "JPY (¥)",
        "CAD (C$)",
        "AUD (A$)"
    )
    val themeOptions = listOf("Light", "Dark")
    val timeFormatOptions = listOf("12 hr", "24 hr")
    val allCategories = (vm.expenseCategories + vm.incomeCategories).distinct().sorted()
    val defaultCategoryOptions = if (allCategories.isNotEmpty()) allCategories else listOf("Others")

    LaunchedEffect(state.theme) {
        if (state.theme !in themeOptions) {
            state = state.copy(theme = "Light")
        }
    }
    LaunchedEffect(state.timeFormat) {
        if (state.timeFormat !in timeFormatOptions && state.timeFormat != "12-Hour Format") {
            state = state.copy(timeFormat = "12 hr")
        } else if (state.timeFormat == "12-Hour Format") {
            state = state.copy(timeFormat = "12 hr")
        }
    }
    LaunchedEffect(state.defaultCategory, defaultCategoryOptions) {
        if (state.defaultCategory !in defaultCategoryOptions) {
            state = state.copy(defaultCategory = defaultCategoryOptions.first())
        }
    }
    CommonScreen("Settings", nav) {
        CurrencyDropdown(
            label = "Theme",
            value = state.theme,
            options = themeOptions,
            onSelect = { state = state.copy(theme = it) }
        )
        CurrencyDropdown(
            label = "Time Format",
            value = state.timeFormat,
            options = timeFormatOptions,
            onSelect = { state = state.copy(timeFormat = it) }
        )
        CurrencyDropdown(
            label = "Currency & Format",
            value = state.currency,
            options = currencies,
            onSelect = { state = state.copy(currency = it) }
        )
        CurrencyDropdown(
            label = "Default Category",
            value = state.defaultCategory,
            options = defaultCategoryOptions,
            onSelect = { state = state.copy(defaultCategory = it) }
        )
        val backupCardColor = Color(0xFFE3F2FD)
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = backupCardColor,
                contentColor = readableTextColor(backupCardColor)
            )
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Backup, Restore & Export", fontWeight = FontWeight.SemiBold)
                Text("Choose backup destination after tapping Backup.")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showBackupChooser = true }, modifier = Modifier.weight(1f)) { Text("Backup") }
            Button(onClick = { showRestoreChooser = true }, modifier = Modifier.weight(1f)) { Text("Restore") }
        }
        Button(onClick = { vm.exportTransactions { actionMessage = it } }, modifier = Modifier.fillMaxWidth()) { Text("Export") }
        if (actionMessage.isNotBlank()) Text(actionMessage, color = MaterialTheme.colorScheme.primary)
        Button(onClick = {
            vm.updateSettings(state)
            nav.navigate(Routes.More) {
                popUpTo(Routes.More) { inclusive = false }
                launchSingleTop = true
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Save Settings") }

        if (showBackupChooser) {
            ModalBottomSheet(onDismissRequest = { showBackupChooser = false }) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Select Backup Destination", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = {
                            showBackupChooser = false
                            vm.backupToLocalDownloads { actionMessage = "$it. Stored in Downloads." }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Local Storage") }
                    Text("Local backups are always stored in Downloads.", color = Color.Gray)
                    Button(
                        onClick = {
                            showBackupChooser = false
                            vm.backupToGoogle { actionMessage = it }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Google Drive") }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
        if (showRestoreChooser) {
            ModalBottomSheet(onDismissRequest = { showRestoreChooser = false }) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Select Restore Source", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = {
                            showRestoreChooser = false
                            restoreSource = "File Manager"
                            localRestoreLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("File Manager") }
                    Text("Choose a .bk file from local storage.", color = Color.Gray)
                    Button(
                        onClick = {
                            showRestoreChooser = false
                            restoreSource = "Google Drive"
                            localRestoreLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Google Drive") }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommonScreen(title: String, nav: NavHostController, content: @Composable () -> Unit) {
    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text(title) },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
        )
    }) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun FeatureTile(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier.padding(vertical = 4.dp).clickable { onClick() }) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val icon = when (label) {
                "Categories" -> Icons.Default.Category
                "Tags" -> Icons.Default.Tag
                "Settings" -> Icons.Default.Settings
                else -> Icons.Default.MoreHoriz
            }
            Icon(icon, null)
            Text(label, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SetupItem(text: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text)
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    amount: Double,
    symbol: String,
    modifier: Modifier,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(
            containerColor = color,
            contentColor = readableTextColor(color)
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text("$symbol${"%.2f".format(amount)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

private fun readableTextColor(background: Color): Color {
    val luminance = (0.299f * background.red) + (0.587f * background.green) + (0.114f * background.blue)
    return if (luminance > 0.6f) Color(0xFF111111) else Color.White
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterDropdown(selected: String, categories: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onSelect(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagFilterDropdown(selected: String, tags: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tag") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            tags.forEach { tag ->
                DropdownMenuItem(
                    text = { Text(tag) },
                    onClick = {
                        onSelect(tag)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun parseTagTokens(input: String): List<String> {
    val explicitHashtags = Regex("#[A-Za-z0-9_]+").findAll(input).map { it.value.lowercase() }.toList()
    if (explicitHashtags.isNotEmpty()) return explicitHashtags.distinct()

    return input
        .split(',', ' ', '\n', '\t')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { raw -> "#${raw.trimStart('#').lowercase()}" }
        .distinct()
}

@Composable
private fun SettingsSwitch(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label)
            Switch(checked = value, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun SettingsField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun currencySymbolFromSetting(currency: String): String {
    val start = currency.indexOf('(')
    val end = currency.indexOf(')')
    return if (start >= 0 && end > start) currency.substring(start + 1, end) else "$"
}

private fun formatTimeBySettings(time: LocalTime, timeFormat: String): String {
    val pattern = if (timeFormat.equals("24 hr", ignoreCase = true)) "HH:mm" else "hh:mm a"
    return time.format(DateTimeFormatter.ofPattern(pattern))
}

@Composable
private fun itemsBlock(lines: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach {
            Card(Modifier.fillMaxWidth()) { Text(it, Modifier.padding(12.dp)) }
        }
    }
}








