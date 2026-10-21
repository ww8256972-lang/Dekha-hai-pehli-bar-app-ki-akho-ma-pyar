package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ShopBranch
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.SecurityPinManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ShopViewModel,
    onLockRequested: () -> Unit,
    onNavigateToBackup: () -> Unit = {},
    onNavigateToSync: () -> Unit = {}
) {
    val context = LocalContext.current
    val pinManager = remember { SecurityPinManager(context) }

    val branches by viewModel.branches.collectAsState()
    val activeBranch by viewModel.activeBranch.collectAsState()

    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Multi-Shop", "Lock & PIN", "Backup / 2nd Phone", "Database Info")

    // Multi-Shop Dialog State
    var showAddBranchDialog by remember { mutableStateOf(false) }

    // PIN Dialog State
    var showChangePinDialog by remember { mutableStateOf(false) }
    var isLockEnabled by remember { mutableStateOf(pinManager.isAppLockEnabled()) }
    var isBiometricEnabled by remember { mutableStateOf(pinManager.isBiometricEnabled()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_settings")
    ) {
        // Top Header Banner
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Settings & Management",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Active Shop: ${activeBranch.name}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Direct Lock Now Button
                    FilledTonalButton(
                        onClick = onLockRequested,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = StatusRedContainer,
                            contentColor = StatusRed
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_settings_lock_now")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lock Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable or Primary Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("settings_tab_$index")
                        )
                    }
                }
            }
        }

        // Tab Content Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                // Tab 0: Multi-Shop Management
                0 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Multi-Shop & Branches", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Button(
                                        onClick = { showAddBranchDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("btn_add_shop_branch")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Shop", fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Manage multiple business locations, wholesale godowns, and Dalmot units in one app. Switch anytime to record sales and view inventory for each branch.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "AVAILABLE SHOPS / BRANCHES (${branches.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    items(branches, key = { it.id }) { branch ->
                        val isActive = branch.id == activeBranch.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.switchBranch(branch.id)
                                    Toast.makeText(context, "Switched to ${branch.name}", Toast.LENGTH_SHORT).show()
                                }
                                .testTag("shop_branch_card_${branch.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isActive) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    if (isActive) Icons.Default.Check else Icons.Default.Store,
                                                    contentDescription = null,
                                                    tint = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(branch.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                if (branch.isPrimary) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.primaryContainer
                                                    ) {
                                                        Text(
                                                            "PRIMARY",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                "Code: ${branch.branchCode} • ${branch.phone}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    if (isActive) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = StatusGreenContainer
                                        ) {
                                            Text(
                                                "ACTIVE",
                                                color = StatusGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.switchBranch(branch.id)
                                                Toast.makeText(context, "Switched to ${branch.name}", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Text("Switch", fontSize = 11.sp)
                                        }
                                    }
                                }

                                if (branch.address.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Address: ${branch.address}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }

                                if (branch.description.isNotBlank()) {
                                    Text(
                                        branch.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                if (!branch.isPrimary && branches.size > 1) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(
                                            onClick = {
                                                viewModel.deleteBranch(branch.id)
                                                Toast.makeText(context, "Branch removed", Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = StatusRed, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Delete Branch", color = StatusRed, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

                // Tab 1: Lock Ledger & Security PIN
                1 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Lock Ledger & App Protection",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Prevent unauthorized access to your sales, cash drawer, and ledger entries.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Lock Now Action
                                Button(
                                    onClick = onLockRequested,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_lock_screen_now")
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("LOCK APPLICATION NOW", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))

                                // Biometric Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = StatusGreen)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Biometric Authentication", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Unlock using Fingerprint or Face Scan", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                    Switch(
                                        checked = isBiometricEnabled,
                                        onCheckedChange = {
                                            isBiometricEnabled = it
                                            pinManager.setBiometricEnabled(it)
                                            Toast.makeText(context, if (it) "Biometrics enabled" else "Biometrics disabled", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("switch_biometric_toggle")
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // App Lock on Startup
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Lock on App Launch", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text("Require authentication when opening app", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                    Switch(
                                        checked = isLockEnabled,
                                        onCheckedChange = {
                                            isLockEnabled = it
                                            pinManager.setAppLockEnabled(it)
                                            Toast.makeText(context, if (it) "App lock enabled" else "App lock disabled", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("switch_app_lock_toggle")
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Pin, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Master Security PIN", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text(
                                                if (pinManager.isInitialPin()) "Default PIN: 8055 (Recommended to change)" else "Custom PIN Configured",
                                                fontSize = 11.sp,
                                                color = if (pinManager.isInitialPin()) StatusOrange else StatusGreen
                                            )
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { showChangePinDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("btn_change_pin_dialog")
                                    ) {
                                        Text("Change PIN", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

                // Tab 2: Backup & Second Phone
                2 -> {
                    BackupRestoreScreen(
                        viewModel = viewModel,
                        onNavigateBack = { selectedTab = 0 }
                    )
                }

                // Tab 3: Database & Sensitive Information
                3 -> {
                    SyncStatusScreen(
                        viewModel = viewModel,
                        onNavigateToBackup = { selectedTab = 2 }
                    )
                }
            }
        }
    }

    // Modal: Add Shop Branch Dialog
    if (showAddBranchDialog) {
        var bName by remember { mutableStateOf("") }
        var bCode by remember { mutableStateOf("BR-0${branches.size + 1}") }
        var bPhone by remember { mutableStateOf("") }
        var bAddress by remember { mutableStateOf("") }
        var bDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddBranchDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Shop / Branch", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = bName,
                        onValueChange = { bName = it },
                        label = { Text("Shop / Branch Name *") },
                        placeholder = { Text("e.g. ASLAM Dalmot Unit 3") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_branch_name"),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bCode,
                            onValueChange = { bCode = it },
                            label = { Text("Code") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = bPhone,
                            onValueChange = { bPhone = it },
                            label = { Text("Phone") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    OutlinedTextField(
                        value = bAddress,
                        onValueChange = { bAddress = it },
                        label = { Text("Address / Location") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = bDesc,
                        onValueChange = { bDesc = it },
                        label = { Text("Category / Notes") },
                        placeholder = { Text("e.g. Retail bidi, wholesale dalmot") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bName.isBlank()) {
                            Toast.makeText(context, "Please enter shop name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addBranch(bName, bCode, bPhone, bAddress, bDesc)
                        showAddBranchDialog = false
                        Toast.makeText(context, "New shop branch added!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("btn_confirm_add_branch")
                ) {
                    Text("Save Branch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBranchDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Change PIN Dialog
    if (showChangePinDialog) {
        var currentPinInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }
        var confirmPinInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change Master PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currentPinInput,
                        onValueChange = { if (it.length <= 6) currentPinInput = it },
                        label = { Text("Current PIN (Default: 8055)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 6) newPinInput = it },
                        label = { Text("New PIN (4-6 digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { if (it.length <= 6) confirmPinInput = it },
                        label = { Text("Confirm New PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!pinManager.verifyPin(currentPinInput)) {
                            Toast.makeText(context, "Current PIN is incorrect", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPinInput.length < 4) {
                            Toast.makeText(context, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newPinInput != confirmPinInput) {
                            Toast.makeText(context, "New PINs do not match", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val success = pinManager.changePin(currentPinInput, newPinInput)
                        if (success) {
                            showChangePinDialog = false
                            Toast.makeText(context, "Security PIN updated successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to update PIN", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Update PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
