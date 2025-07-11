package com.example.dukaaan.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.text.font.FontWeight
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun DateFilterDialog(
    fromDate: Date?,
    toDate: Date?,
    onFromDateChange: (Date?) -> Unit,
    onToDateChange: (Date?) -> Unit,
    onDismiss: () -> Unit,
    onFind: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Sales by Date Range") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Select date range to filter sales:")
                
                // From Date
                Column {
                    Text("From Date:", style = MaterialTheme.typography.body2, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = fromDate?.let { dateFormat.format(it) } ?: "",
                        onValueChange = { },
                        label = { Text("Select From Date") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                // Show date picker for from date
                                val calendar = Calendar.getInstance()
                                val year = calendar.get(Calendar.YEAR)
                                val month = calendar.get(Calendar.MONTH)
                                val day = calendar.get(Calendar.DAY_OF_MONTH)
                                
                                val datePickerDialog = android.app.DatePickerDialog(
                                    context,
                                    { _, selectedYear, selectedMonth, selectedDay ->
                                        val selectedDate = Calendar.getInstance().apply {
                                            set(selectedYear, selectedMonth, selectedDay)
                                        }.time
                                        onFromDateChange(selectedDate)
                                    },
                                    year, month, day
                                )
                                datePickerDialog.show()
                            }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select From Date")
                            }
                        }
                    )
                }
                
                // To Date
                Column {
                    Text("To Date:", style = MaterialTheme.typography.body2, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = toDate?.let { dateFormat.format(it) } ?: "",
                        onValueChange = { },
                        label = { Text("Select To Date") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                // Show date picker for to date
                                val calendar = Calendar.getInstance()
                                val year = calendar.get(Calendar.YEAR)
                                val month = calendar.get(Calendar.MONTH)
                                val day = calendar.get(Calendar.DAY_OF_MONTH)
                                
                                val datePickerDialog = android.app.DatePickerDialog(
                                    context,
                                    { _, selectedYear, selectedMonth, selectedDay ->
                                        val selectedDate = Calendar.getInstance().apply {
                                            set(selectedYear, selectedMonth, selectedDay)
                                        }.time
                                        onToDateChange(selectedDate)
                                    },
                                    year, month, day
                                )
                                datePickerDialog.show()
                            }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select To Date")
                            }
                        }
                    )
                }
                
                // Preview selected dates
                if (fromDate != null || toDate != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Selected Date Range:", style = MaterialTheme.typography.body2, fontWeight = FontWeight.Bold)
                            Text("From: ${fromDate?.let { dateFormat.format(it) } ?: "Not selected"}")
                            Text("To: ${toDate?.let { dateFormat.format(it) } ?: "Not selected"}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onFind,
                enabled = fromDate != null && toDate != null
            ) {
                Text("Find")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 