package com.example.dukaaan.ui

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import com.example.dukaaan.model.EditHistory
import androidx.compose.foundation.layout.Column

@Composable
fun EditHistoryDialog(history: List<EditHistory>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit History") },
        text = {
            if (history.isEmpty()) Text("No history.")
            else Column {
                for (edit in history) {
                    Text("${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(edit.timestamp))}: ${edit.edited_by} - ${edit.changes}")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
} 