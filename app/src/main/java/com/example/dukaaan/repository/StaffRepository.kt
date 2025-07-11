package com.example.dukaaan.repository

import com.example.dukaaan.model.Staff

interface StaffRepository {
    suspend fun getStaffById(staffId: String): Staff?
    suspend fun getStaffByShop(shopId: String): List<Staff>
    suspend fun createStaff(staff: Staff): Boolean
    suspend fun updateStaff(staff: Staff): Boolean
} 