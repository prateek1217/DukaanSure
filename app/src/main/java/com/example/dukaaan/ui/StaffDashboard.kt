package com.example.dukaaan.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dukaaan.viewmodel.StaffDashboardViewModel
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import kotlinx.coroutines.launch
import com.example.dukaaan.ui.StockDialog
import com.example.dukaaan.ui.EditHistoryDialog
import com.example.dukaaan.ui.DateFilterDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.text.font.FontWeight
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.NavController

@Composable
fun StaffDashboard(
    shopId: String,
    staffId: String,
    staffName: String,
    onLogout: () -> Unit,
    navController: NavController,
    viewModel: StaffDashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val stocks by viewModel.stocks.collectAsState()
    val activeStatus by viewModel.activeStatus.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sales = viewModel.sales.collectAsState().value

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<com.example.dukaaan.model.Stock?>(null) }
    var showHistoryDialog by remember { mutableStateOf<com.example.dukaaan.model.Stock?>(null) }
    var showDateFilterDialog by remember { mutableStateOf(false) }

    var selectedStock by remember { mutableStateOf<String?>(null) }
    var customerName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var sellLoading by remember { mutableStateOf(false) }
    var sellError by remember { mutableStateOf<String?>(null) }
    var toggleLoading by remember { mutableStateOf(false) }
    var dialogLoading by remember { mutableStateOf(false) }
    var dialogError by remember { mutableStateOf<String?>(null) }

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Stocks", "Sell Product", "History")

    var showSuggestions by remember { mutableStateOf(false) }

    // Add state for logout confirmation dialog
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Date filter states
    var fromDate by remember { mutableStateOf<Date?>(null) }
    var toDate by remember { mutableStateOf<Date?>(null) }
    var isDateFilterActive by remember { mutableStateOf(false) }

    // Filter sales based on date range
    val filteredSales = remember(sales, fromDate, toDate, isDateFilterActive) {
        if (!isDateFilterActive || fromDate == null || toDate == null) {
            sales
        } else {
            sales.filter { sale ->
                val saleDate = Date(sale.datetime)
                saleDate >= fromDate!! && saleDate <= toDate!!
            }
        }
    }

    LaunchedEffect(shopId) { viewModel.loadStocks(shopId) }
    LaunchedEffect(staffId) { viewModel.loadActiveStatus(staffId) }
    LaunchedEffect(selectedTab) { 
        if (selectedTab == 2) viewModel.loadSales(shopId)
        if (selectedTab == 0) {
            viewModel.setSearchQuery("")
            selectedStock = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Staff Dashboard") }, actions = {
                Button(onClick = { showLogoutDialog = true }) { Text("Logout") }
            })
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Text("+")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Status: ", style = MaterialTheme.typography.body1)
                    Text(
                        if (activeStatus) "Active" else "Inactive",
                        color = if (activeStatus) MaterialTheme.colors.primary else MaterialTheme.colors.error
                    )
                }
                TabRow(selectedTabIndex = selectedTab) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
                when (selectedTab) {
                    0 -> { // Stocks
                        Text("View/Edit Stocks", style = MaterialTheme.typography.h6)
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            label = { Text("Search Stocks") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (stocks.isEmpty()) {
                            Text("No stocks found.")
                        } else {
                            stocks.forEach { stock ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(stock.name + " (Qty: ${stock.count})", modifier = Modifier.weight(1f))
                                            IconButton(onClick = { showEditDialog = stock }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                                            }
                                            Button(
                                                onClick = {
                                                    navController.navigate(
                                                        com.example.dukaaan.ui.Screen.ProductSalesHistory.createRoute(stock.stock_id, stock.name)
                                                    )
                                                },
                                                modifier = Modifier.padding(start = 4.dp)
                                            ) {
                                                Text("View Stock History")
                                            }
                                        }
                                        Text("Last edited by: ${stock.last_edited_by}", style = MaterialTheme.typography.body2)
                                    }
                                }
                            }
                        }
                    }
                    1 -> { // Sell Product
                        Text("Sell Product", style = MaterialTheme.typography.h6)
                        if (stocks.isEmpty()) {
                            Text("No stocks available.")
                        } else {
                            // Autocomplete for stock selection
                            OutlinedTextField(
                                value = selectedStock?.let { sId ->
                                    stocks.find { it.stock_id == sId }?.let { "${it.name} (Qty: ${it.count})" } ?: searchQuery
                                } ?: searchQuery,
                                onValueChange = {
                                    viewModel.setSearchQuery(it)
                                    showSuggestions = true
                                    selectedStock = null
                                },
                                label = { Text("Search Stocks") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            if (showSuggestions && searchQuery.isNotBlank() && stocks.isNotEmpty()) {
                                Card(elevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        stocks.forEach { stock ->
                                            DropdownMenuItem(onClick = {
                                                selectedStock = stock.stock_id
                                                viewModel.setSearchQuery(stock.name)
                                                showSuggestions = false
                                            }) {
                                                Text(stock.name + " (Qty: ${stock.count})")
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { Text("Customer Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = quantity,
                                onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                                label = { Text("Quantity") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    sellError = null
                                    val qty = quantity.toIntOrNull() ?: 0
                                    if (selectedStock == null || customerName.isBlank() || qty <= 0) {
                                        sellError = "Fill all fields correctly."
                                        return@Button
                                    }
                                    sellLoading = true
                                    viewModel.sellProduct(
                                        stockId = selectedStock!!,
                                        shopId = shopId,
                                        staffId = staffId,
                                        staffName = staffName,
                                        customerName = customerName,
                                        quantity = qty,
                                        onSuccess = { message, ownerMobile ->
                                            // Format phone for WhatsApp intent
                                            val phone = if (ownerMobile.startsWith("+")) ownerMobile else "+91$ownerMobile"
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse("whatsapp://send?phone=$phone&text=" + Uri.encode(message))
                                            }
                                            context.startActivity(intent)
                                            customerName = ""
                                            quantity = ""
                                            selectedStock = null
                                            sellLoading = false
                                            coroutineScope.launch { snackbarHostState.showSnackbar("Sale successful!") }
                                        },
                                        onError = { error ->
                                            sellError = error
                                            sellLoading = false
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !sellLoading
                            ) { if (sellLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp)) else Text("Sell Product") }
                            if (sellError != null) {
                                Text(sellError!!, color = MaterialTheme.colors.error)
                            }
                        }
                    }
                    2 -> { // History
                        Text("Sales History", style = MaterialTheme.typography.h6)
                        
                        // Date filter controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showDateFilterDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = "Date Range")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Find by Date")
                            }
                            
                            if (isDateFilterActive) {
                                Button(
                                    onClick = {
                                        fromDate = null
                                        toDate = null
                                        isDateFilterActive = false
                                    },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Reset Filter")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset Filter")
                                }
                            }
                        }
                        
                        // Show active filter info
                        if (isDateFilterActive && fromDate != null && toDate != null) {
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        "Filtered by Date Range:",
                                        style = MaterialTheme.typography.body2,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${dateFormat.format(fromDate!!)} to ${dateFormat.format(toDate!!)}",
                                        style = MaterialTheme.typography.body2
                                    )
                                    Text(
                                        "Showing ${filteredSales.size} of ${sales.size} sales",
                                        style = MaterialTheme.typography.body2
                                    )
                                }
                            }
                        }
                        
                        if (filteredSales.isEmpty()) {
                            Text(if (isDateFilterActive) "No sales found in selected date range." else "No sales yet.")
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(filteredSales.size) { index ->
                                    val sale = filteredSales[index]
                                    val stockName = stocks.find { it.stock_id == sale.stock_id }?.name ?: "Unknown"
                                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Product: $stockName", style = MaterialTheme.typography.body1)
                                            Text("Quantity: ${sale.quantity}")
                                            Text("Customer: ${sale.customer_name}")
                                            Text("Sold by: ${sale.sold_by}")
                                            Text("Date: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(sale.datetime))}")
                                        }
                                    }
                                }
                            }
                        }
                        Divider()
                        Text("Edit History", style = MaterialTheme.typography.h6)
                        val allEditEvents = stocks.flatMap { stock ->
                            stock.edit_history.map { edit ->
                                Triple(stock, edit, stock.edit_history)
                            }
                        }.sortedByDescending { it.second.timestamp }
                        if (allEditEvents.isEmpty()) {
                            Text("No edit history.")
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(allEditEvents.size) { index ->
                                    val (stock, edit, _) = allEditEvents[index]
                                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Product: ${stock.name}", style = MaterialTheme.typography.body1)
                                            Text("Edited by: ${edit.edited_by}")
                                            Text("Change: ${edit.changes}")
                                            Text("Date: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(edit.timestamp))}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Date Filter Dialog
    if (showDateFilterDialog) {
        DateFilterDialog(
            fromDate = fromDate,
            toDate = toDate,
            onFromDateChange = { fromDate = it },
            onToDateChange = { toDate = it },
            onDismiss = { showDateFilterDialog = false },
            onFind = {
                if (fromDate != null && toDate != null) {
                    isDateFilterActive = true
                    showDateFilterDialog = false
                    coroutineScope.launch { 
                        snackbarHostState.showSnackbar("Date filter applied successfully!") 
                    }
                } else {
                    coroutineScope.launch { 
                        snackbarHostState.showSnackbar("Please select both from and to dates!") 
                    }
                }
            }
        )
    }

    if (showAddDialog) {
        StockDialog(
            title = "Add Stock",
            initialName = "",
            initialCount = "",
            loading = dialogLoading,
            error = dialogError,
            onDismiss = {
                showAddDialog = false
                dialogError = null
            },
            onConfirm = { name, count ->
                if (name.isBlank() || count.isBlank() || count.toIntOrNull() == null || count.toInt() <= 0) {
                    dialogError = "Enter valid name and count (>0)"
                    dialogLoading = false
                    return@StockDialog
                }
                dialogLoading = true
                viewModel.addStock(shopId, name, count.toInt(), staffName) { success ->
                    dialogLoading = false
                    showAddDialog = false
                    if (success) {
                        dialogError = null
                        coroutineScope.launch { snackbarHostState.showSnackbar("Stock added successfully") }
                    } else {
                        dialogError = "Failed to add stock"
                        coroutineScope.launch { snackbarHostState.showSnackbar("Failed to add stock") }
                    }
                }
            }
        )
    }
    if (showEditDialog != null) {
        val stock = showEditDialog!!
        StockDialog(
            title = "Edit Stock",
            initialName = stock.name,
            initialCount = stock.count.toString(),
            loading = dialogLoading,
            error = dialogError,
            onDismiss = {
                showEditDialog = null
                dialogError = null
            },
            onConfirm = { name, count ->
                if (name.isBlank() || count.isBlank() || count.toIntOrNull() == null || count.toInt() <= 0) {
                    dialogError = "Enter valid name and count (>0)"
                    return@StockDialog
                }
                dialogLoading = true
                viewModel.editStock(shopId, stock, name, count.toInt(), staffName) { success ->
                    dialogLoading = false
                    if (success) {
                        showEditDialog = null
                        dialogError = null
                        coroutineScope.launch { snackbarHostState.showSnackbar("Stock updated successfully") }
                    } else {
                        dialogError = "Failed to update stock"
                    }
                }
            }
        )
    }
    if (showHistoryDialog != null) {
        val stock = showHistoryDialog!!
        EditHistoryDialog(
            history = viewModel.getEditHistory(stock),
            onDismiss = { showHistoryDialog = null }
        )
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
} 