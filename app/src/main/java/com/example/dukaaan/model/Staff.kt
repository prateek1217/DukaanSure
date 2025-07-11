package com.example.dukaaan.model

data class Staff(
    val staff_id: String = "",
    val name: String = "",
    val shop_id: String = "",
    val active_status: Boolean = false,
    val last_login: Long = 0L
) 