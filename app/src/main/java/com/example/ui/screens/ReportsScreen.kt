package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: ShopViewModel
) {
    val context = LocalContext.current
    val report by viewModel.financialReport.collectAsState()
    val period by viewModel.selectedPeriod.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()

    var showAddExpenseDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header with PDF Export Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Business Reports & P&L",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Accurate calculation from verified local records",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Button(
                    onClick = {
                        try {
                            val pdf = PdfGenerator.generateBusinessReport(context, report, shopProfile)
                            PdfGenerator.sharePdf(context, pdf, "Business Profit & Loss Report")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export PDF", fontSize = 12.sp)
                }
            }
        }

        // Time Period Filter Tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val periods = listOf(
                    "TODAY" to "Today (आज)",
                    "MONTH" to "Month (माह)",
                    "YEAR" to "Year (वर्ष)",
                    "ALL" to "All Time (कुल)"
                )
                periods.forEach { (key, label) ->
                    FilterChip(
                        selected = period == key,
                        onClick = { viewModel.setReportPeriod(key) },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Profit Summary Hero Card
        item {
            val isProfitable = report.netProfit >= 0
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isProfitable) EmeraldContainer else StatusRedContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = if (isProfitable) "NET BUSINESS PROFIT (${report.periodLabel})" else "NET LOSS (${report.periodLabel})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isProfitable) OnEmeraldContainer else StatusRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${String.format(Locale.US, "%.2f", report.netProfit)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isProfitable) OnEmeraldContainer else StatusRed
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Calculated as: Sales Revenue - Cost of Goods Sold (COGS) - Operating Expenses",
                        fontSize = 11.sp,
                        color = if (isProfitable) OnEmeraldContainer.copy(alpha = 0.8f) else StatusRed.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Detailed Metrics Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Revenue & Cash Breakdown", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)

                    ReportRow("Total Sales Revenue (Billed)", "₹${String.format(Locale.US, "%.2f", report.totalSalesRevenue)}")
                    ReportRow("Total Cash & UPI Received", "₹${String.format(Locale.US, "%.2f", report.totalCashReceived)}", color = StatusGreen)
                    ReportRow("Cost of Goods Sold (COGS)", "₹${String.format(Locale.US, "%.2f", report.cogs)}")
                    ReportRow("Gross Profit", "₹${String.format(Locale.US, "%.2f", report.grossProfit)}", isBold = true)

                    HorizontalDivider()

                    Text("Operating Expenses & Balance Sheet", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    ReportRow("Total Recorded Expenses", "₹${String.format(Locale.US, "%.2f", report.totalExpenses)}", color = StatusRed)
                    ReportRow("Total Outstanding Customer Udhar", "₹${String.format(Locale.US, "%.2f", report.totalUdharOutstanding)}", color = StatusRed, isBold = true)
                    ReportRow("Current Inventory Cost Valuation", "₹${String.format(Locale.US, "%.2f", report.inventoryCostValue)}")
                    ReportRow("Current Inventory Retail Valuation", "₹${String.format(Locale.US, "%.2f", report.inventoryRetailValue)}")
                }
            }
        }

        // Expenses Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shop Expenses (${expenses.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                OutlinedButton(
                    onClick = { showAddExpenseDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("btn_add_expense")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Expense", fontSize = 12.sp)
                }
            }
        }

        if (expenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "No shop expenses recorded yet. Tap '+ Add Expense' to record rent, transport, electricity, or wages.",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(expenses.take(6)) { expense ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(expense.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                "${expense.category} • ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(expense.timestamp))}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "₹${String.format(Locale.US, "%.2f", expense.amount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = StatusRed
                            )
                            IconButton(onClick = { viewModel.deleteExpense(expense) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Add Expense Dialog
    if (showAddExpenseDialog) {
        var expTitle by remember { mutableStateOf("") }
        var expCategory by remember { mutableStateOf("Shop Operating") }
        var expAmount by remember { mutableStateOf("") }
        var expNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = { Text("Record Business Expense (खर्च)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = expTitle,
                        onValueChange = { expTitle = it },
                        label = { Text("Expense Title (e.g. Rent, Transport, Chai)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_expense_title")
                    )
                    OutlinedTextField(
                        value = expCategory,
                        onValueChange = { expCategory = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = expAmount,
                        onValueChange = { expAmount = it },
                        label = { Text("Amount (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = expNotes,
                        onValueChange = { expNotes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = expAmount.toDoubleOrNull() ?: 0.0
                        if (expTitle.isBlank() || amt <= 0.0) {
                            Toast.makeText(context, "Please enter valid title and amount", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addExpense(expTitle, expCategory, amt, expNotes)
                        showAddExpenseDialog = false
                        Toast.makeText(context, "Expense recorded!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save Expense")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ReportRow(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color? = null,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = color ?: MaterialTheme.colorScheme.onSurface
        )
    }
}
