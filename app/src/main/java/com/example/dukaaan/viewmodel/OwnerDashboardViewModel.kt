package com.example.dukaaan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dukaaan.model.Staff
import com.example.dukaaan.model.Stock
import com.example.dukaaan.model.EditHistory
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import com.google.firebase.firestore.ListenerRegistration
import com.example.dukaaan.model.Sale

class OwnerDashboardViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _allStocks = MutableStateFlow<List<Stock>>(emptyList())
    private val _stocks = MutableStateFlow<List<Stock>>(emptyList())
    val stocks: StateFlow<List<Stock>> = _stocks.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var stocksListener: ListenerRegistration? = null
    private var stocksJob: Job? = null

    private val _staff = MutableStateFlow<List<Staff>>(emptyList())
    val staff: StateFlow<List<Staff>> = _staff

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private var staffListener: ListenerRegistration? = null

    private val _shopCode = MutableStateFlow("")
    val shopCode: StateFlow<String> = _shopCode

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
        stocksListener?.remove()
        stocksListener = firestore.collection("stocks")
            .whereEqualTo("shop_id", shopId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(Stock::class.java) }
                    _allStocks.value = list
                    filterStocks()
                }
            }
    }

    fun loadStaff(shopId: String) {
        staffListener?.remove()
        staffListener = firestore.collection("staff")
            .whereEqualTo("shop_id", shopId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _staff.value = snapshot.documents.mapNotNull { it.toObject(Staff::class.java) }
                }
            }
    }

    fun addStock(shopId: String, name: String, count: Int, ownerName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val stockId = firestore.collection("stocks").document().id
                val stock = Stock(
                    stock_id = stockId,
                    shop_id = shopId,
                    name = name,
                    count = count,
                    last_edited_by = ownerName,
                    edit_history = listOf(
                        EditHistory(
                            edited_by = ownerName,
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

    fun editStock(shopId: String, stock: Stock, newName: String, newCount: Int, editorName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val changes = mutableListOf<String>()
                if (stock.name != newName) changes.add("Name: '${stock.name}' → '$newName'")
                if (stock.count != newCount) changes.add("Count: ${stock.count} → $newCount")
                val newEditHistory = stock.edit_history + EditHistory(
                    edited_by = editorName,
                    changes = changes.joinToString(", "),
                    timestamp = System.currentTimeMillis()
                )
                firestore.collection("stocks").document(stock.stock_id)
                    .update(mapOf(
                        "name" to newName,
                        "count" to newCount,
                        "last_edited_by" to editorName,
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

    fun deleteStock(shopId: String, stockId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                firestore.collection("stocks").document(stockId).delete().await()
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

    fun loadShopCode(shopId: String) {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("shops").document(shopId).get().await()
                _shopCode.value = doc.getString("shop_code") ?: ""
            } catch (e: Exception) {
                _shopCode.value = ""
            }
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

    fun toggleStaffActiveStatus(staffId: String, newStatus: Boolean, onResult: () -> Unit) {
        viewModelScope.launch {
            try {
                val staffDoc = firestore.collection("staff")
                    .whereEqualTo("staff_id", staffId)
                    .get().await()
                if (!staffDoc.isEmpty) {
                    firestore.collection("staff").document(staffDoc.documents.first().id)
                        .update("active_status", newStatus).await()
                }
            } catch (_: Exception) {}
            onResult()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stocksListener?.remove()
        staffListener?.remove()
    }
} 