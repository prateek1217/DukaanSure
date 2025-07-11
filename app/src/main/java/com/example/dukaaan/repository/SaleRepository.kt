package com.example.dukaaan.repository

import com.example.dukaaan.model.Sale
 
interface SaleRepository {
    suspend fun getSalesByStock(stockId: String): List<Sale>
    suspend fun createSale(sale: Sale): Boolean
} 