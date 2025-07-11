package com.example.dukaaan.ui

import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StockDialog(
    title: String,
    initialName: String,
    initialCount: String,
    loading: Boolean = false,
    error: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var count by remember { mutableStateOf(initialCount) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, enabled = !loading)
                OutlinedTextField(value = count, onValueChange = { count = it.filter { c -> c.isDigit() } }, label = { Text("Count") }, enabled = !loading)
                if (error != null) Text(error, color = MaterialTheme.colors.error)
                if (loading) Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Please wait...") }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, count) }, enabled = !loading) { Text("OK") }
        },
        dismissButton = {
            Button(onClick = onDismiss, enabled = !loading) { Text("Cancel") }
        }
    )
} 