package com.example.dukaaan.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

@Composable
fun ProductSalesHistoryScreen(
    stockId: String,
    stockName: String,
    onBack: () -> Unit
) {
    var sales by remember { mutableStateOf<List<SaleItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(stockId) {
        loading = true
        FirebaseFirestore.getInstance()
            .collection("sales")
            .whereEqualTo("stock_id", stockId)
            .get()
            .addOnSuccessListener { result ->
                sales = result.documents.mapNotNull { doc ->
                    val customer = doc.getString("customer_name") ?: ""
                    val qty = doc.getLong("quantity")?.toInt() ?: 0
                    val soldBy = doc.getString("sold_by") ?: ""
                    val datetime = doc.getLong("datetime") ?: 0L
                    SaleItem(customer, qty, soldBy, datetime)
                }.sortedByDescending { it.datetime }
                loading = false
            }
            .addOnFailureListener {
                error = "Failed to load sales."
                loading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales History: $stockName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error!!, color = MaterialTheme.colors.error)
                    }
                }
                sales.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No sales for this product.")
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sales.size) { idx ->
                            val sale = sales[idx]
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Customer: ${sale.customer}", fontWeight = FontWeight.Bold)
                                    Text("Quantity: ${sale.quantity}")
                                    Text("Sold by: ${sale.soldBy}")
                                    Text("Date: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(sale.datetime))}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class SaleItem(
    val customer: String,
    val quantity: Int,
    val soldBy: String,
    val datetime: Long
) 