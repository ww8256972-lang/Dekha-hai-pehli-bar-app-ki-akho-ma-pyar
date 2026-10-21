package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.BackupManager
import com.example.util.RestoreResult
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun BackupRestoreScreen(
    viewModel: ShopViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var restoreResult by remember { mutableStateOf<RestoreResult?>(null) }

    // File picker launcher for selecting JSON backup file
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isImporting = true
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val jsonContent = reader.use { it.readText() }
                    val result = BackupManager.restoreFromJson(
                        context = context,
                        database = viewModel.getAppDatabase(),
                        jsonContent = jsonContent
                    )
                    restoreResult = result
                    if (result.success) {
                        Toast.makeText(context, "Data restored successfully!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Restore failed: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    restoreResult = RestoreResult(
                        success = false,
                        message = "Could not read file: ${e.message}"
                    )
                } finally {
                    isImporting = false
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Backup & Second-Phone Recovery",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Export your complete ledger data or restore it to a second mobile phone with zero data loss.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        // Export Backup Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Export Full Backup File",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Creates a verified JSON backup file containing all Products (Bidi & Dalmot), Customers, Suppliers, Sales, Payments, and Expenses.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isExporting = true
                                try {
                                    val backupFile = BackupManager.createBackupFile(
                                        context = context,
                                        database = viewModel.getAppDatabase()
                                    )
                                    BackupManager.shareBackupFile(context, backupFile)
                                    Toast.makeText(context, "Backup file generated successfully!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error creating backup: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isExporting = false
                                }
                            }
                        },
                        enabled = !isExporting,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_export_backup")
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Backup File...")
                        } else {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export & Share Backup (व्हाट्सएप/ड्राइव पर भेजें)")
                        }
                    }
                }
            }
        }

        // Import Backup Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. Restore / Import into New Device",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Restore an existing ASLAM RECORD backup file on this device or a new mobile phone. All records will be imported with full relationship integrity.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        enabled = !isImporting,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_import_backup")
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importing & Validating Records...")
                        } else {
                            Icon(Icons.Default.FileOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Backup File (बैकअप फ़ाइल चुनें)")
                        }
                    }
                }
            }
        }

        // Restore Results Summary Card
        restoreResult?.let { res ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (res.success) StatusGreenContainer else StatusRedContainer
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (res.success) StatusGreen else StatusRed
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (res.success) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (res.success) StatusGreenDark else StatusRedDark
                            )
                            Text(
                                text = if (res.success) "Restore Completed Successfully" else "Restore Failed",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (res.success) OnStatusGreenContainer else OnStatusRedContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = res.message,
                            fontSize = 12.sp,
                            color = if (res.success) OnStatusGreenContainer else OnStatusRedContainer
                        )

                        if (res.success) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            Text("• Products Restored: ${res.productsRestored}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnStatusGreenContainer)
                            Text("• Customers Restored: ${res.customersRestored}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnStatusGreenContainer)
                            Text("• Suppliers Restored: ${res.suppliersRestored}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnStatusGreenContainer)
                            Text("• Sales Invoices Restored: ${res.salesRestored}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnStatusGreenContainer)
                            Text("• Payment Receipts Restored: ${res.paymentsRestored}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnStatusGreenContainer)
                            Text("• Expenses Restored: ${res.expensesRestored}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnStatusGreenContainer)
                        }
                    }
                }
            }
        }

        // How-To Guide for Second Mobile
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📱 Second Phone Data Transfer Guide",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. On Phone 1: Tap 'Export & Share Backup' and send the file to your WhatsApp or Google Drive.\n" +
                                "2. On Phone 2: Install ASLAM RECORD and open this 'Backup & Restore' screen.\n" +
                                "3. Tap 'Select Backup File', pick the saved .json backup file.\n" +
                                "4. All customer ledgers, sales bills, stock counts, and payments will immediately restore on Phone 2!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
