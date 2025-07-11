package com.example.dukaaan.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dukaaan.viewmodel.OwnerDashboardViewModel
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import kotlinx.coroutines.launch
import com.example.dukaaan.model.Stock
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.example.dukaaan.ui.StockDialog
import com.example.dukaaan.ui.EditHistoryDialog
import androidx.compose.ui.graphics.Color
import android.util.Log
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.text.font.FontWeight
import java.text.SimpleDateFormat
import java.util.*
import com.example.dukaaan.ui.DateFilterDialog
import androidx.navigation.NavController

@Composable
fun OwnerDashboard(shopId: String, ownerName: String, onLogout: () -> Unit, navController: NavController, viewModel: OwnerDashboardViewModel = viewModel()) {
    val context = LocalContext.current
    val stocks = viewModel.stocks.collectAsState().value
    val staff = viewModel.staff.collectAsState().value
    val loading = viewModel.loading.collectAsState().value
    val searchQuery = viewModel.searchQuery.collectAsState().value
    val shopCode = viewModel.shopCode.collectAsState().value
    val sales = viewModel.sales.collectAsState().value

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Stock?>(null) }
    var showHistoryDialog by remember { mutableStateOf<Stock?>(null) }
    var showDateFilterDialog by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Stocks", "Staff", "Sell Product", "Sales History")

    var selectedStock by remember { mutableStateOf<String?>(null) }
    var customerName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var sellLoading by remember { mutableStateOf(false) }
    var sellError by remember { mutableStateOf<String?>(null) }
    var dialogLoading by remember { mutableStateOf(false) }
    var dialogError by remember { mutableStateOf<String?>(null) }

    // Date filter states
    var fromDate by remember { mutableStateOf<Date?>(null) }
    var toDate by remember { mutableStateOf<Date?>(null) }
    var isDateFilterActive by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showSuggestions by remember { mutableStateOf(false) }

    // Add state for logout confirmation dialog
    var showLogoutDialog by remember { mutableStateOf(false) }

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

    // Add state for delete confirmation dialog
    var stockToDelete by remember { mutableStateOf<Stock?>(null) }

    LaunchedEffect(shopId) {
        viewModel.loadStocks(shopId)
        viewModel.loadStaff(shopId)
        viewModel.loadShopCode(shopId)
    }
    LaunchedEffect(selectedTab) {
        if (selectedTab == 3) viewModel.loadSales(shopId)
        if (selectedTab == 0) {
            viewModel.setSearchQuery("")
            selectedStock = null
        }
    }
    LaunchedEffect(staff) {
        Log.d("OwnerDashboard", "Staff count: ${staff.size}, names: ${staff.map { it.name }}")
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Owner Dashboard") }, actions = {
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
                Text("Shop Code: $shopCode", style = MaterialTheme.typography.body1)
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
                            stocks.forEach { stock: com.example.dukaaan.model.Stock ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(stock.name + " (Qty: ${stock.count})", modifier = Modifier.weight(1f))
                                            IconButton(onClick = { showEditDialog = stock }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                                            }
                                            IconButton(onClick = { stockToDelete = stock }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete")
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
                    1 -> { // Staff
                        Text("View Staff", style = MaterialTheme.typography.h6)
                        if (staff.isEmpty()) {
                            Text("No staff found.")
                        } else {
                            staff.forEach { s: com.example.dukaaan.model.Staff ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("STAFF: " + s.name.ifBlank { "No Name" }, color = Color.Black)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            if (s.active_status) "Active" else "Inactive",
                                            color = if (s.active_status) MaterialTheme.colors.primary else MaterialTheme.colors.error
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        var switchLoading by remember { mutableStateOf(false) }
                                        Switch(
                                            checked = s.active_status,
                                            onCheckedChange = { checked ->
                                                switchLoading = true
                                                viewModel.toggleStaffActiveStatus(s.staff_id, checked) {
                                                    switchLoading = false
                                                }
                                            },
                                            enabled = !switchLoading
                                        )
                                    }
                                }
                            }
                        }
                    }
                    2 -> { // Sell Product
                        Text("Sell Product", style = MaterialTheme.typography.h6)
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
                        // Autocomplete suggestions
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
                                var stock: com.example.dukaaan.model.Stock? = null
                                for (s in stocks) {
                                    if (s.stock_id == selectedStock) {
                                        stock = s
                                        break
                                    }
                                }
                                if (stock == null) return@Button
                                val docRef = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("stocks").document(stock.stock_id)
                                val newCount = stock.count - qty
                                if (newCount < 0) {
                                    sellError = "Not enough stock."
                                    sellLoading = false
                                    return@Button
                                }
                                docRef.update("count", newCount, "last_edited_by", ownerName).addOnSuccessListener {
                                    // Add sale record
                                    val sale = com.example.dukaaan.model.Sale(
                                        sale_id = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("sales").document().id,
                                        shop_id = shopId,
                                        stock_id = stock.stock_id,
                                        sold_by = ownerName,
                                        customer_name = customerName,
                                        quantity = qty,
                                        datetime = System.currentTimeMillis()
                                    )
                                    com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("sales").document(sale.sale_id).set(sale)
                                    // WhatsApp message (optional, if you want to notify yourself)
                                    customerName = ""
                                    quantity = ""
                                    selectedStock = null
                                    sellLoading = false
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Sale successful!") }
                                }.addOnFailureListener {
                                    sellError = "Failed to update stock."
                                    sellLoading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !sellLoading
                        ) { if (sellLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp)) else Text("Sell Product") }
                        if (sellError != null) {
                            Text(sellError!!, color = MaterialTheme.colors.error)
                        }
                    }
                    3 -> { // Sales History
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
                                    var stockName = "Unknown"
                                    for (stock in stocks) {
                                        if (stock.stock_id == sale.stock_id) {
                                            stockName = stock.name
                                            break
                                        }
                                    }
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
            onConfirm = { name: String, count: String ->
                if (name.isBlank() || count.isBlank() || count.toIntOrNull() == null || count.toInt() <= 0) {
                    dialogError = "Enter valid name and count (>0)"
                    dialogLoading = false
                    return@StockDialog
                }
                dialogLoading = true
                viewModel.addStock(shopId, name, count.toInt(), ownerName) { success: Boolean ->
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
            onConfirm = { name: String, count: String ->
                if (name.isBlank() || count.isBlank() || count.toIntOrNull() == null || count.toInt() <= 0) {
                    dialogError = "Enter valid name and count (>0)"
                    return@StockDialog
                }
                dialogLoading = true
                viewModel.editStock(shopId, stock, name, count.toInt(), ownerName) { success: Boolean ->
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

    // Confirmation dialog for deleting stock
    if (stockToDelete != null) {
        AlertDialog(
            onDismissRequest = { stockToDelete = null },
            title = { Text("Delete Stock") },
            text = { Text("Are you sure to delete this stock?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteStock(shopId, stockToDelete!!.stock_id) { }
                    stockToDelete = null
                }, colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { stockToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
} 