package com.example.dukaaan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dukaaan.model.Sale
import com.example.dukaaan.model.Stock
import com.example.dukaaan.model.EditHistory
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StaffDashboardViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _allStocks = MutableStateFlow<List<Stock>>(emptyList())
    private val _stocks = MutableStateFlow<List<Stock>>(emptyList())
    val stocks: StateFlow<List<Stock>> = _stocks

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _activeStatus = MutableStateFlow(false)
    val activeStatus: StateFlow<Boolean> = _activeStatus

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _sales = MutableStateFlow<List<Sale>>(emptyList())
    val sales: StateFlow<List<Sale>> = _sales

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterStocks()
    }

    private fun filterStocks() {
        val query = _searchQuery.value.trim().lowercase()
        _stocks.value = if (query.isEmpty()) {
            _allStocks.value
        } else {
            _allStocks.value.filter { it.name.lowercase().contains(query) }
        }
    }

    fun loadStocks(shopId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = firestore.collection("stocks")
                    .whereEqualTo("shop_id", shopId)
                    .get().await()
                _allStocks.value = result.documents.mapNotNull { it.toObject(Stock::class.java) }
                filterStocks()
            } catch (e: Exception) {
                _allStocks.value = emptyList()
                filterStocks()
            }
            _loading.value = false
        }
    }

    fun addStock(shopId: String, name: String, count: Int, staffName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val stockId = firestore.collection("stocks").document().id
                val stock = Stock(
                    stock_id = stockId,
                    shop_id = shopId,
                    name = name,
                    count = count,
                    last_edited_by = staffName,
                    edit_history = listOf(
                        EditHistory(
                            edited_by = staffName,
                            changes = "Created stock with count $count",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                )
                firestore.collection("stocks").document(stockId).set(stock).await()
                loadStocks(shopId)
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            } finally {
                _loading.value = false
            }
        }
    }

    fun editStock(shopId: String, stock: Stock, newName: String, newCount: Int, staffName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val changes = mutableListOf<String>()
                if (stock.name != newName) changes.add("Name: '${stock.name}' → '$newName'")
                if (stock.count != newCount) changes.add("Count: ${stock.count} → $newCount")
                val newEditHistory = stock.edit_history + EditHistory(
                    edited_by = staffName,
                    changes = changes.joinToString(", "),
                    timestamp = System.currentTimeMillis()
                )
                firestore.collection("stocks").document(stock.stock_id)
                    .update(mapOf(
                        "name" to newName,
                        "count" to newCount,
                        "last_edited_by" to staffName,
                        "edit_history" to newEditHistory
                    )).await()
                loadStocks(shopId)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
            _loading.value = false
        }
    }

    fun getEditHistory(stock: Stock): List<EditHistory> {
        return stock.edit_history.sortedByDescending { it.timestamp }
    }

    fun loadActiveStatus(staffId: String) {
        viewModelScope.launch {
            try {
                val staffDoc = firestore.collection("staff")
                    .whereEqualTo("staff_id", staffId)
                    .get().await()
                if (!staffDoc.isEmpty) {
                    _activeStatus.value = staffDoc.documents.first().getBoolean("active_status") ?: false
                }
            } catch (_: Exception) {}
        }
    }

    fun toggleActiveStatus(staffId: String, newStatus: Boolean) {
        viewModelScope.launch {
            try {
                val staffDoc = firestore.collection("staff")
                    .whereEqualTo("staff_id", staffId)
                    .get().await()
                if (!staffDoc.isEmpty) {
                    firestore.collection("staff").document(staffDoc.documents.first().id)
                        .update("active_status", newStatus).await()
                    _activeStatus.value = newStatus
                }
            } catch (_: Exception) {}
        }
    }

    fun sellProduct(
        stockId: String,
        shopId: String,
        staffId: String,
        staffName: String,
        customerName: String,
        quantity: Int,
        onSuccess: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                // 1. Get stock
                val stockDoc = firestore.collection("stocks").whereEqualTo("stock_id", stockId).get().await()
                if (stockDoc.isEmpty) {
                    onError("Stock not found.")
                    _loading.value = false
                    return@launch
                }
                val docRef = stockDoc.documents.first().reference
                val stock = stockDoc.documents.first().toObject(Stock::class.java) ?: run {
                    onError("Stock data error.")
                    _loading.value = false
                    return@launch
                }
                if (stock.count < quantity) {
                    onError("Not enough stock.")
                    _loading.value = false
                    return@launch
                }
                // 2. Update stock count
                val newCount = stock.count - quantity
                docRef.update("count", newCount, "last_edited_by", staffName).await()
                // 3. Add sale record
                val sale = Sale(
                    sale_id = firestore.collection("sales").document().id,
                    shop_id = shopId,
                    stock_id = stockId,
                    sold_by = staffId,
                    customer_name = customerName,
                    quantity = quantity,
                    datetime = System.currentTimeMillis()
                )
                firestore.collection("sales").document(sale.sale_id).set(sale).await()
                // 4. Fetch owner's mobile number
                val shopDoc = firestore.collection("shops").document(shopId).get().await()
                val ownerMobile = shopDoc.getString("mobile_number")
                if (ownerMobile.isNullOrBlank()) {
                    onError("Owner's mobile number not found.")
                    _loading.value = false
                    return@launch
                }
                // 5. WhatsApp message
                val message = "Item ${stock.name} sold by $staffName to $customerName at ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(sale.datetime))}. Remaining qty: $newCount."
                onSuccess(message, ownerMobile)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Sell failed.")
            }
            _loading.value = false
        }
    }

    fun loadSales(shopId: String) {
        viewModelScope.launch {
            try {
                val result = firestore.collection("sales")
                    .whereEqualTo("shop_id", shopId)
                    .get().await()
                _sales.value = result.documents.mapNotNull { it.toObject(Sale::class.java) }
                    .sortedByDescending { it.datetime }
            } catch (e: Exception) {
                _sales.value = emptyList()
            }
        }
    }
} 