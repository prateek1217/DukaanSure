package com.example.dukaaan.model

import com.google.firebase.Timestamp

data class EditHistory(
    val edited_by: String = "",
    val changes: String = "",
    val timestamp: Long = 0L
)

data class Stock(
    val stock_id: String = "",
    val shop_id: String = "",
    val name: String = "",
    val count: Int = 0,
    val last_edited_by: String = "",
    val edit_history: List<EditHistory> = emptyList()
) 