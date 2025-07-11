package com.example.dukaaan.repository

import com.example.dukaaan.model.Stock

interface StockRepository {
    suspend fun getStocksByShop(shopId: String): List<Stock>
    suspend fun getStockById(stockId: String): Stock?
    suspend fun createStock(stock: Stock): Boolean
    suspend fun updateStock(stock: Stock): Boolean
} 