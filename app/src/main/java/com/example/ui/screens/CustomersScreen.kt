package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.api.WhatsAppManager
import com.example.data.model.CustomerEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SaleEntity
import com.example.domain.SaleItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: ShopViewModel
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val products by viewModel.products.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterDuesOnly by remember { mutableStateOf(false) }

    // Dialog States
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var selectedCustomerForLedger by remember { mutableStateOf<CustomerEntity?>(null) }
    var showRecordPaymentDialog by remember { mutableStateOf<CustomerEntity?>(null) }
    var showAddCreditSaleDialog by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerForReminderReview by remember { mutableStateOf<CustomerEntity?>(null) }
    var newlyCreatedCustomerForWhatsApp by remember { mutableStateOf<CustomerEntity?>(null) }
    var createdSaleForReceipt by remember { mutableStateOf<SaleEntity?>(null) }

    val filteredCustomers = remember(customers, searchQuery, filterDuesOnly) {
        customers.filter { c ->
            val matchQuery = searchQuery.isBlank() ||
                    c.name.contains(searchQuery, ignoreCase = true) ||
                    c.phone.contains(searchQuery) ||
                    c.address.contains(searchQuery, ignoreCase = true)
            val matchDues = !filterDuesOnly || c.outstandingBalance > 0
            matchQuery && matchDues
        }
    }

    val totalUdhar = remember(customers) {
        customers.sumOf { it.outstandingBalance }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCustomerDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_customer")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header & Total Udhar Overview with 3D gradient card
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Customer Khata Ledger",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${customers.size} Registered Customers",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Udhar Outstanding",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "₹${String.format(Locale.US, "%.2f", totalUdhar)}",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalUdhar > 0) StatusRed else StatusGreen
                                )
                            }
                        }
                    }
                }
            }

            // Search Box & Dues Filter
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search customer name, phone, address...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_customers"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !filterDuesOnly,
                            onClick = { filterDuesOnly = false },
                            label = { Text("All Customers (${customers.size})", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = filterDuesOnly,
                            onClick = { filterDuesOnly = true },
                            label = {
                                Text(
                                    "उधार बाकी / Dues (${customers.count { it.outstandingBalance > 0 }})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StatusRedContainer,
                                selectedLabelColor = StatusRed
                            )
                        )
                    }
                }
            }

            // Empty State
            if (filteredCustomers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.PeopleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (customers.isEmpty()) "No customers added yet" else "No matching customers found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Add customers with photos, phone & WhatsApp numbers to track their purchases and send instant payment reminders.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddCustomerDialog = true },
                                modifier = Modifier.testTag("btn_empty_add_customer")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add First Customer")
                            }
                        }
                    }
                }
            } else {
                items(filteredCustomers, key = { it.id }) { customer ->
                    val hasUdhar = customer.outstandingBalance > 0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCustomerForLedger = customer },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Customer Photo or Avatar
                                if (customer.imageUri.isNotBlank()) {
                                    AsyncImage(
                                        model = customer.imageUri,
                                        contentDescription = customer.name,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (hasUdhar) StatusRedContainer else StatusGreenContainer,
                                        modifier = Modifier.size(50.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = customer.name.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                color = if (hasUdhar) StatusRed else StatusGreen
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Customer Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = customer.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (customer.points > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = GoldContainer
                                            ) {
                                                Text(
                                                    "⭐ ${customer.points} pts",
                                                    color = OnGoldContainer,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (customer.phone.isNotBlank()) {
                                        Text(
                                            text = "📞 ${customer.phone}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    if (customer.address.isNotBlank()) {
                                        Text(
                                            text = "📍 ${customer.address}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                // Outstanding Balance
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (hasUdhar) "Due (बाकी):" else "Status:",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = if (hasUdhar) "₹${String.format(Locale.US, "%.2f", customer.outstandingBalance)}" else "All Settled ✅",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasUdhar) StatusRed else StatusGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Action Buttons Row: Reminder, Payment, New Credit, Ledger
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (hasUdhar) {
                                    // Primary Udhar Reminder Button
                                    Button(
                                        onClick = { customerForReminderReview = customer },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("उधार रिमाइंडर", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Receive Payment Button
                                    FilledTonalButton(
                                        onClick = { showRecordPaymentDialog = customer },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("जमा (Receive)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                // Add New Credit Entry Button
                                OutlinedButton(
                                    onClick = { showAddCreditSaleDialog = customer },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ नया उधार", fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                // Detailed Ledger Button
                                IconButton(
                                    onClick = { selectedCustomerForLedger = customer },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.MenuBook, contentDescription = "View Ledger", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Review & Send Payment Reminder on WhatsApp or SMS
    customerForReminderReview?.let { cust ->
        var reminderText by remember(cust) {
            mutableStateOf(WhatsAppManager.formatPaymentReminderMessage(shopProfile, cust, cust.outstandingBalance))
        }

        AlertDialog(
            onDismissRequest = { customerForReminderReview = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = StatusRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("उधार पेमेंट रिमाइंडर", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StatusRedContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("ग्राहक: ${cust.name}", fontWeight = FontWeight.Bold, color = StatusRed)
                            if (cust.phone.isNotBlank()) {
                                Text("मोबाइल: ${cust.phone}", fontSize = 12.sp, color = StatusRed)
                            }
                            Text(
                                "कुल बकाया: ₹${String.format(Locale.US, "%.2f", cust.outstandingBalance)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = StatusRed
                            )
                        }
                    }

                    Text("रिमाइंडर संदेश (समीक्षा एवं संपादन करें):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = reminderText,
                        onValueChange = { reminderText = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val phone = cust.phone
                        if (phone.isBlank()) {
                            Toast.makeText(context, "ग्राहक का फोन नंबर नहीं है, शेयर करें", Toast.LENGTH_SHORT).show()
                            WhatsAppManager.launchShareIntent(context, reminderText, "उधार रिमाइंडर")
                        } else {
                            WhatsAppManager.launchWhatsAppIntent(context, phone, reminderText)
                        }
                        customerForReminderReview = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp पर भेजें", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        WhatsAppManager.launchShareIntent(context, reminderText, "उधार रिमाइंडर - ${cust.name}")
                        customerForReminderReview = null
                    }
                ) {
                    Text("SMS / Share")
                }
            }
        )
    }

    // Modal: Record New Credit / Udhar Transaction Directly
    showAddCreditSaleDialog?.let { targetCustomer ->
        var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
        var productExpanded by remember { mutableStateOf(false) }
        var customItemName by remember { mutableStateOf("") }
        var quantityText by remember { mutableStateOf("1") }
        var unitText by remember { mutableStateOf("Packet") }
        var unitPriceText by remember { mutableStateOf("") }
        var totalAmountText by remember { mutableStateOf("") }
        var amountPaidNowText by remember { mutableStateOf("0") }
        var creditNotes by remember { mutableStateOf("") }
        var isSavingCredit by remember { mutableStateOf(false) }

        // Recalculate total when qty or price changes
        LaunchedEffect(quantityText, unitPriceText) {
            val q = quantityText.toDoubleOrNull() ?: 1.0
            val p = unitPriceText.toDoubleOrNull()
            if (p != null) {
                totalAmountText = String.format(Locale.US, "%.2f", q * p)
            }
        }

        val totalAmount = totalAmountText.toDoubleOrNull() ?: 0.0
        val paidNow = amountPaidNowText.toDoubleOrNull() ?: 0.0
        val remainingDue = (totalAmount - paidNow).coerceAtLeast(0.0)

        AlertDialog(
            onDismissRequest = { if (!isSavingCredit) showAddCreditSaleDialog = null },
            title = {
                Column {
                    Text("नया उधार खाता दर्ज करें", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("ग्राहक: ${targetCustomer.name}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text(
                            "तारीख: ${SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date())}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    // Product Selector Dropdown
                    item {
                        ExposedDropdownMenuBox(
                            expanded = productExpanded,
                            onExpandedChange = { productExpanded = !productExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedProduct?.name ?: if (customItemName.isNotBlank()) customItemName else "",
                                onValueChange = {
                                    customItemName = it
                                    selectedProduct = null
                                },
                                label = { Text("सामान चुनें या नाम लिखें *") },
                                placeholder = { Text("बीड़ी बंडल, तंबाकू, या अन्य सामान...") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = productExpanded,
                                onDismissRequest = { productExpanded = false }
                            ) {
                                if (products.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No products cataloged. Enter name below.") },
                                        onClick = { productExpanded = false }
                                    )
                                } else {
                                    products.forEach { prod ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(prod.name, fontWeight = FontWeight.Medium)
                                                    Text(
                                                        "₹${String.format(Locale.US, "%.2f", prod.sellingPrice)} / ${prod.unit}",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedProduct = prod
                                                customItemName = prod.name
                                                unitText = prod.unit
                                                unitPriceText = String.format(Locale.US, "%.2f", prod.sellingPrice)
                                                productExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = quantityText,
                                onValueChange = { quantityText = it },
                                label = { Text("मात्रा (Qty) *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = unitText,
                                onValueChange = { unitText = it },
                                label = { Text("इकाई (Unit)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = unitPriceText,
                                onValueChange = { unitPriceText = it },
                                label = { Text("दर (Rate ₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = totalAmountText,
                                onValueChange = { totalAmountText = it },
                                label = { Text("कुल रकम (Total ₹) *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = amountPaidNowText,
                            onValueChange = { amountPaidNowText = it },
                            label = { Text("अभी नकद दिया (0 भी चलेगा) ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Remaining Due Highlight
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusRedContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("खाते में जुड़ेगा बाकी (Due):", fontWeight = FontWeight.Bold, color = StatusRed, fontSize = 13.sp)
                                Text("₹${String.format(Locale.US, "%.2f", remainingDue)}", fontWeight = FontWeight.ExtraBold, color = StatusRed, fontSize = 16.sp)
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = creditNotes,
                            onValueChange = { creditNotes = it },
                            label = { Text("विवरण / नोट (Optional Notes)") },
                            placeholder = { Text("जैसे: शनिवार को देने का वादा") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (totalAmount <= 0.0) {
                            Toast.makeText(context, "कृपया कुल राशि दर्ज करें", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val qty = quantityText.toDoubleOrNull() ?: 1.0
                        val rate = unitPriceText.toDoubleOrNull() ?: (totalAmount / qty)
                        val itemName = customItemName.ifBlank { selectedProduct?.name ?: "बीड़ी / माल" }
                        val pId = selectedProduct?.id ?: 0L
                        val buyRate = selectedProduct?.purchasePrice ?: 0.0

                        val items = listOf(
                            SaleItem(
                                productId = pId,
                                productName = itemName,
                                quantity = qty,
                                unit = unitText,
                                unitPrice = rate,
                                lineTotal = totalAmount,
                                purchasePrice = buyRate
                            )
                        )

                        isSavingCredit = true
                        viewModel.recordSale(
                            customerId = targetCustomer.id,
                            customerName = targetCustomer.name,
                            items = items,
                            totalAmount = totalAmount,
                            amountPaid = paidNow,
                            paymentMethod = "Credit/Udhar",
                            notes = creditNotes
                        ) { savedSale, _ ->
                            isSavingCredit = false
                            showAddCreditSaleDialog = null
                            selectedCustomerForLedger = null
                            createdSaleForReceipt = savedSale
                            Toast.makeText(context, "उधार खाता सफलतापूर्वक सुरक्षित किया गया!", Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isSavingCredit && totalAmount > 0
                ) {
                    if (isSavingCredit) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("उधार सुरक्षित करें (Save)")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCreditSaleDialog = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Modal: Add Customer Dialog with Photo and Direct WhatsApp Integration
    if (showAddCustomerDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var pointsText by remember { mutableStateOf("0") }
        var imageUri by remember { mutableStateOf("") }

        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            uri?.let { imageUri = it.toString() }
        }

        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text("नया ग्राहक खाता जोड़ें", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Customer Photo Picker
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (imageUri.isNotBlank()) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Customer Photo",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(32.dp))
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (imageUri.isBlank()) "ग्राहक की फोटो जोड़ें" else "फोटो बदलें", fontSize = 12.sp)
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("ग्राहक का पूरा नाम *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_customer_name")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("WhatsApp / मोबाइल नंबर *") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("पता / मोहल्ला / गाँव") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = pointsText,
                                onValueChange = { pointsText = it },
                                label = { Text("लॉयल्टी पॉइंट्स") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("उधार सीमा / विवरण") },
                                modifier = Modifier.weight(2f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "ग्राहक का नाम आवश्यक है", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val pts = pointsText.toIntOrNull() ?: 0
                        viewModel.addCustomer(
                            name = name,
                            phone = phone,
                            address = address,
                            notes = notes,
                            points = pts,
                            imageUri = imageUri
                        ) { created ->
                            showAddCustomerDialog = false
                            newlyCreatedCustomerForWhatsApp = created
                            Toast.makeText(context, "ग्राहक ${created.name} का खाता बन गया!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("सुरक्षित करें (Save)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Post-Save WhatsApp Welcome Prompt
    newlyCreatedCustomerForWhatsApp?.let { newCust ->
        AlertDialog(
            onDismissRequest = { newlyCreatedCustomerForWhatsApp = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ग्राहक खाता तैयार!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("क्या आप ${newCust.name} को WhatsApp पर डिजिटल खाता खुलने का स्वागत संदेश भेजना चाहते हैं?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val welcomeMsg = """
                            *${shopProfile.name}*
                            📍 ${shopProfile.address}
                            📞 ${shopProfile.phone}
                            ───────────────────────
                            नमस्ते *${newCust.name}* जी!
                            हमारी दुकान पर आपका बही-खाता सफलतापूर्वक शुरू हो गया है।
                            
                            धन्यवाद!
                            — *${shopProfile.name}*
                        """.trimIndent()
                        WhatsAppManager.launchWhatsAppIntent(context, newCust.phone, welcomeMsg)
                        newlyCreatedCustomerForWhatsApp = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp पर भेजें", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { newlyCreatedCustomerForWhatsApp = null }) {
                    Text("छोड़ें")
                }
            }
        )
    }

    // Modal: Record Balance Payment Dialog (Udhar Vasooli) with instant receipt sharing
    showRecordPaymentDialog?.let { customer ->
        var payAmount by remember { mutableStateOf(String.format(Locale.US, "%.2f", customer.outstandingBalance)) }
        var payMethod by remember { mutableStateOf("Cash") }
        var payNotes by remember { mutableStateOf("") }
        var isSavingPay by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSavingPay) showRecordPaymentDialog = null },
            title = { Text("उधार वसूली / जमा: ${customer.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StatusRedContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "कुल बकाया राशि: ₹${String.format(Locale.US, "%.2f", customer.outstandingBalance)}",
                            modifier = Modifier.padding(10.dp),
                            color = StatusRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = { payAmount = it },
                        label = { Text("जमा की गई राशि (Amount ₹) *") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_receive_payment_amount")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Cash", "UPI", "Bank").forEach { method ->
                            FilterChip(
                                selected = payMethod == method,
                                onClick = { payMethod = method },
                                label = { Text(method) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = payNotes,
                        onValueChange = { payNotes = it },
                        label = { Text("भुगतान विवरण / संदर्भ (Notes)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = payAmount.toDoubleOrNull() ?: 0.0
                        if (amount <= 0.0) {
                            Toast.makeText(context, "कृपया वैध राशि दर्ज करें", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSavingPay = true
                        viewModel.recordBalancePayment(
                            customerId = customer.id,
                            customerName = customer.name,
                            amount = amount,
                            paymentMethod = payMethod,
                            notes = payNotes
                        ) { payment ->
                            isSavingPay = false
                            showRecordPaymentDialog = null
                            val updatedBalance = (customer.outstandingBalance - amount).coerceAtLeast(0.0)

                            if (customer.phone.isNotBlank()) {
                                val receiptMsg = """
                                    🧾 *${shopProfile.name} — भुगतान रसीद*
                                    रसीद नं: *${payment.receiptNumber}*
                                    ग्राहक: *${customer.name}*
                                    जमा राशि: *₹${String.format(Locale.US, "%.2f", amount)}* माध्यम $payMethod
                                    बाकी बकाया: *₹${String.format(Locale.US, "%.2f", updatedBalance)}*
                                    ───────────────────────
                                    धन्यवाद!
                                """.trimIndent()
                                WhatsAppManager.launchWhatsAppIntent(context, customer.phone, receiptMsg)
                            }

                            try {
                                val pdf = PdfGenerator.generatePaymentReceipt(context, payment, updatedBalance, shopProfile)
                                PdfGenerator.sharePdf(context, pdf, "Payment Receipt ${payment.receiptNumber}")
                            } catch (_: Exception) {}
                        }
                    },
                    enabled = !isSavingPay
                ) {
                    Text("रसीद जारी करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordPaymentDialog = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Modal: Detailed Customer Ledger Sheet
    selectedCustomerForLedger?.let { customer ->
        val customerSales = remember(sales, customer.id) {
            sales.filter { it.customerId == customer.id }
        }
        val customerPayments = remember(payments, customer.id) {
            payments.filter { it.customerId == customer.id }
        }

        AlertDialog(
            onDismissRequest = { selectedCustomerForLedger = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (customer.phone.isNotBlank()) {
                            Text("📞 ${customer.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    if (customer.outstandingBalance > 0) {
                        Text(
                            "₹${String.format(Locale.US, "%.2f", customer.outstandingBalance)}",
                            color = StatusRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Direct Action Buttons inside Ledger
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (customer.outstandingBalance > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val target = customer
                                            selectedCustomerForLedger = null
                                            customerForReminderReview = target
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("रिमाइंडर", color = Color.White, fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            val target = customer
                                            selectedCustomerForLedger = null
                                            showRecordPaymentDialog = target
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("जमा करें", fontSize = 11.sp)
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val target = customer
                                    selectedCustomerForLedger = null
                                    showAddCreditSaleDialog = target
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ नया उधार जोड़ें (New Credit Sale)", fontSize = 12.sp)
                            }
                        }
                    }

                    item {
                        Text("बही-खाता इतिहास (Ledger History):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (customerSales.isEmpty() && customerPayments.isEmpty()) {
                        item {
                            Text(
                                "No transactions recorded for this customer yet.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        val allEvents = (customerSales.map { "SALE" to it } + customerPayments.map { "PAYMENT" to it })
                            .sortedByDescending {
                                if (it.first == "SALE") (it.second as SaleEntity).timestamp
                                else (it.second as PaymentEntity).timestamp
                            }

                        items(allEvents) { (type, event) ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (type == "SALE") {
                                        val sale = event as SaleEntity
                                        Column {
                                            Text("Invoice: ${sale.invoiceNumber} (${sale.paymentMethod})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(
                                                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(sale.timestamp)),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("₹${String.format(Locale.US, "%.2f", sale.totalAmount)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            if (sale.outstandingBalance > 0) {
                                                Text("Due: ₹${String.format(Locale.US, "%.2f", sale.outstandingBalance)}", fontSize = 10.sp, color = StatusRed)
                                            }
                                        }
                                    } else {
                                        val pay = event as PaymentEntity
                                        Column {
                                            Text("रसीद: ${pay.receiptNumber}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusGreen)
                                            Text(
                                                "${pay.paymentMethod} • ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(pay.timestamp))}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Text("+ ₹${String.format(Locale.US, "%.2f", pay.amount)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCustomerForLedger = null }) {
                    Text("बंद करें (Close)")
                }
            },
            dismissButton = null
        )
    }

    // Modal: Automatic Customer Receipt Dialog After Successful Transaction
    createdSaleForReceipt?.let { sale ->
        val customerPhone = filteredCustomers.find { it.id == sale.customerId }?.phone ?: ""
        val hasUdhar = sale.outstandingBalance > 0

        AlertDialog(
            onDismissRequest = { createdSaleForReceipt = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("रसीद तैयार है! (Receipt)", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("बिल नं: ${sale.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("ग्राहक: ${sale.customerName}", fontWeight = FontWeight.Medium)
                            Text("माध्यम: ${sale.paymentMethod}")
                            Text("तारीख: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(sale.timestamp))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("कुल बिल:")
                                Text("₹${String.format(Locale.US, "%.2f", sale.totalAmount)}", fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("जमा राशि:")
                                Text("₹${String.format(Locale.US, "%.2f", sale.amountPaid)}", color = StatusGreen, fontWeight = FontWeight.Bold)
                            }
                            if (hasUdhar) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("बाकी (Udhar Due):", color = StatusRed, fontWeight = FontWeight.Bold)
                                    Text("₹${String.format(Locale.US, "%.2f", sale.outstandingBalance)}", color = StatusRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Direct WhatsApp Share Action
                    if (customerPhone.isNotBlank()) {
                        Button(
                            onClick = {
                                val msg = WhatsAppManager.formatReceiptMessage(
                                    shopProfile,
                                    sale,
                                    CustomerEntity(id = sale.customerId ?: 0L, name = sale.customerName, phone = customerPhone, outstandingBalance = sale.outstandingBalance)
                                )
                                WhatsAppManager.launchWhatsAppIntent(context, customerPhone, msg)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp पर रसीद भेजें")
                        }
                    }
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        try {
                            val items = viewModel.parseSaleItems(sale.itemsJson)
                            val pdf = PdfGenerator.generateSaleReceipt(context, sale, items, shopProfile)
                            PdfGenerator.sharePdf(context, pdf, "Sale Receipt ${sale.invoiceNumber}")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF रसीद")
                }
            },
            dismissButton = {
                TextButton(onClick = { createdSaleForReceipt = null }) {
                    Text("पूर्ण (Done)")
                }
            }
        )
    }
}
