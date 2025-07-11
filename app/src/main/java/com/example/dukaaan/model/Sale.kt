package com.example.dukaaan.model

data class Sale(
    val sale_id: String = "",
    val shop_id: String = "",
    val stock_id: String = "",
    val sold_by: String = "",
    val customer_name: String = "",
    val quantity: Int = 0,
    val datetime: Long = 0L
) 