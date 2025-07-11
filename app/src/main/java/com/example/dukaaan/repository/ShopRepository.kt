package com.example.dukaaan.repository

import com.example.dukaaan.model.Shop

interface ShopRepository {
    suspend fun getShopById(shopId: String): Shop?
    suspend fun getShopByCode(shopCode: String): Shop?
    suspend fun createShop(shop: Shop): Boolean
    suspend fun updateShop(shop: Shop): Boolean
} 