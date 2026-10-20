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
import androidx.compose.foundation.lazy.itemsIndexed
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSaleScreen(
    viewModel: ShopViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()

    // Customer Selection State (No default walk-in customer)
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    // Direct manual customer input if not picked from database
    var manualCustomerName by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var manualCustomerPhone by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }

    // Product Selection State
    var selectedProductToAdd by remember { mutableStateOf<ProductEntity?>(null) }
    var productDropdownExpanded by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }

    // Quick custom item input
    var customItemName by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var customItemPrice by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var customItemQty by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("1") }
    var showCustomItemRow by remember { mutableStateOf(false) }

    // Cart Items
    var cartItems by remember { mutableStateOf<List<SaleItem>>(emptyList()) }

    // Payment details
    var paymentMethod by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("Cash") } // Cash, UPI, Credit/Udhar
    var amountPaidText by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var saleNotes by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }

    // Save & Receipt state
    var isSaving by remember { mutableStateOf(false) }
    var savedSaleResult by remember { mutableStateOf<SaleEntity?>(null) }
    var savedPaymentResult by remember { mutableStateOf<PaymentEntity?>(null) }

    // Calculate Bill Total automatically
    val billTotal = remember(cartItems) {
        cartItems.sumOf { it.lineTotal }
    }

    val amountPaid = remember(amountPaidText, billTotal, paymentMethod) {
        if (paymentMethod == "Credit/Udhar" && amountPaidText.isBlank()) {
            0.0
        } else {
            amountPaidText.toDoubleOrNull() ?: billTotal
        }
    }

    val outstandingBalance = remember(billTotal, amountPaid) {
        (billTotal - amountPaid).coerceAtLeast(0.0)
    }

    // Auto-update amount paid when bill total changes or payment method changes
    LaunchedEffect(billTotal, paymentMethod) {
        if (paymentMethod == "Credit/Udhar") {
            if (amountPaidText.isEmpty() || amountPaidText == String.format(Locale.US, "%.2f", billTotal)) {
                amountPaidText = "0"
            }
        } else if (amountPaidText.isEmpty() || amountPaidText == "0") {
            amountPaidText = String.format(Locale.US, "%.2f", billTotal)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PointOfSale,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Record New Sale",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Select customer, enter products, calculate total & issue receipt",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Section 1: Customer Details & Points Balance
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. Customer Information",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = { showAddCustomerDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Customer", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Existing Customer Dropdown
                    ExposedDropdownMenuBox(
                        expanded = customerDropdownExpanded,
                        onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: if (manualCustomerName.isNotBlank()) manualCustomerName else "",
                            onValueChange = {
                                manualCustomerName = it
                                selectedCustomer = null
                            },
                            label = { Text("Select Registered Customer or Type Name *") },
                            placeholder = { Text("Choose from list or type customer name...") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("dropdown_customer_select"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = customerDropdownExpanded,
                            onDismissRequest = { customerDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("+ Create & Save New Customer", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                onClick = {
                                    customerDropdownExpanded = false
                                    showAddCustomerDialog = true
                                }
                            )
                            HorizontalDivider()

                            if (customers.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No customers in database yet. Add one above.") },
                                    onClick = {
                                        customerDropdownExpanded = false
                                        showAddCustomerDialog = true
                                    }
                                )
                            } else {
                                customers.forEach { customer ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(customer.name, fontWeight = FontWeight.SemiBold)
                                                    if (customer.phone.isNotBlank()) {
                                                        Text(customer.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                                    }
                                                }
                                                if (customer.outstandingBalance > 0) {
                                                    Text(
                                                        "Due: ₹${String.format(Locale.US, "%.2f", customer.outstandingBalance)}",
                                                        fontSize = 11.sp,
                                                        color = StatusRed,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedCustomer = customer
                                            manualCustomerName = customer.name
                                            manualCustomerPhone = customer.phone
                                            customerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // If manual customer name typed and not yet registered
                    if (selectedCustomer == null && manualCustomerName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualCustomerPhone,
                            onValueChange = { manualCustomerPhone = it },
                            label = { Text("Customer Phone (for WhatsApp Receipt)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Active Customer Detail Badge & Point / Balance Card
                    selectedCustomer?.let { cust ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (cust.outstandingBalance > 0) StatusRedContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (cust.imageUri.isNotBlank()) {
                                        AsyncImage(
                                            model = cust.imageUri,
                                            contentDescription = cust.name,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = cust.name.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (cust.phone.isNotBlank()) {
                                            Text(cust.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Outstanding Due", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        text = "₹${String.format(Locale.US, "%.2f", cust.outstandingBalance)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (cust.outstandingBalance > 0) StatusRed else StatusGreen
                                    )
                                    // Quick action button to set payment method or clear customer
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (cust.phone.isNotBlank()) {
                                            IconButton(
                                                onClick = {
                                                    val msg = WhatsAppManager.formatPaymentReminderMessage(shopProfile, cust, cust.outstandingBalance)
                                                    WhatsAppManager.launchWhatsAppIntent(context, cust.phone, msg)
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = "WhatsApp", tint = StatusGreen, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                selectedCustomer = null
                                                manualCustomerName = ""
                                                manualCustomerPhone = ""
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = StatusRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Product Selection & Bill Items
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Add Products to Bill",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row {
                            TextButton(
                                onClick = { showCustomItemRow = !showCustomItemRow },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Custom Item", fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = { showAddProductDialog = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Product", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Product Selector Dropdown
                    ExposedDropdownMenuBox(
                        expanded = productDropdownExpanded,
                        onExpandedChange = { productDropdownExpanded = !productDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedProductToAdd?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pick Product from Catalog") },
                            placeholder = { Text("Choose item to add to bill...") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("dropdown_product_select"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = productDropdownExpanded,
                            onDismissRequest = { productDropdownExpanded = false }
                        ) {
                            if (products.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No products found. Tap '+ New Product' or add Custom Item.") },
                                    onClick = {
                                        productDropdownExpanded = false
                                        showAddProductDialog = true
                                    }
                                )
                            } else {
                                products.forEach { product ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(product.name, fontWeight = FontWeight.SemiBold)
                                                    Text(
                                                        "Stock: ${product.stockQuantity.toInt()} ${product.unit} • ₹${String.format(Locale.US, "%.2f", product.sellingPrice)}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                                val profit = product.sellingPrice - product.purchasePrice
                                                if (profit > 0) {
                                                    Text(
                                                        "+₹${String.format(Locale.US, "%.0f", profit)}",
                                                        fontSize = 11.sp,
                                                        color = StatusGreen,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            // Add or increment
                                            val existing = cartItems.find { it.productId == product.id }
                                            if (existing != null) {
                                                cartItems = cartItems.map {
                                                    if (it.productId == product.id) {
                                                        val newQty = it.quantity + 1.0
                                                        it.copy(quantity = newQty, lineTotal = newQty * it.unitPrice)
                                                    } else it
                                                }
                                            } else {
                                                cartItems = cartItems + SaleItem(
                                                    productId = product.id,
                                                    productName = product.name,
                                                    quantity = 1.0,
                                                    unit = product.unit,
                                                    unitPrice = product.sellingPrice,
                                                    lineTotal = product.sellingPrice,
                                                    purchasePrice = product.purchasePrice
                                                )
                                            }
                                            selectedProductToAdd = null
                                            productDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Direct Custom Item Entry
                    if (showCustomItemRow) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Add Custom / Unlisted Item", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = customItemName,
                                        onValueChange = { customItemName = it },
                                        label = { Text("Item Name") },
                                        modifier = Modifier.weight(1.8f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    OutlinedTextField(
                                        value = customItemPrice,
                                        onValueChange = { customItemPrice = it },
                                        label = { Text("Price (₹)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    OutlinedTextField(
                                        value = customItemQty,
                                        onValueChange = { customItemQty = it },
                                        label = { Text("Qty") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(0.8f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val price = customItemPrice.toDoubleOrNull() ?: 0.0
                                        val qty = customItemQty.toDoubleOrNull() ?: 1.0
                                        if (customItemName.isNotBlank() && price > 0) {
                                            cartItems = cartItems + SaleItem(
                                                productId = 0L,
                                                productName = customItemName,
                                                quantity = qty,
                                                unit = "Item",
                                                unitPrice = price,
                                                lineTotal = qty * price,
                                                purchasePrice = 0.0
                                            )
                                            customItemName = ""
                                            customItemPrice = ""
                                            customItemQty = "1"
                                            showCustomItemRow = false
                                        } else {
                                            Toast.makeText(context, "Please enter item name and price", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add to Bill")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bill Items List
        if (cartItems.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bill Items (${cartItems.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Total: ₹${String.format(Locale.US, "%.2f", billTotal)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            itemsIndexed(cartItems) { index, item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "₹${String.format(Locale.US, "%.2f", item.unitPrice)} / ${item.unit}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // Quantity +/- Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (item.quantity > 1.0) {
                                        val newQty = item.quantity - 1.0
                                        cartItems = cartItems.mapIndexed { i, old ->
                                            if (i == index) old.copy(quantity = newQty, lineTotal = newQty * old.unitPrice) else old
                                        }
                                    } else {
                                        cartItems = cartItems.filterIndexed { i, _ -> i != index }
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.primary)
                            }

                            Text(
                                text = "${item.quantity.toInt()} ${item.unit}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            IconButton(
                                onClick = {
                                    val newQty = item.quantity + 1.0
                                    cartItems = cartItems.mapIndexed { i, old ->
                                        if (i == index) old.copy(quantity = newQty, lineTotal = newQty * old.unitPrice) else old
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "₹${String.format(Locale.US, "%.2f", item.lineTotal)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            IconButton(
                                onClick = {
                                    cartItems = cartItems.filterIndexed { i, _ -> i != index }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = StatusRed)
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Payment Method, Amount, and Narration
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. Payment & Settlement",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bill Total Row (Appears automatically)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Automatic Bill Total:", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "₹${String.format(Locale.US, "%.2f", billTotal)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Payment Method Options: Cash, UPI, Credit / Udhar
                    Text("Payment Mode *", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Cash", "UPI", "Credit/Udhar").forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = {
                                    paymentMethod = method
                                    if (method == "Credit/Udhar") {
                                        amountPaidText = "0"
                                    } else {
                                        amountPaidText = String.format(Locale.US, "%.2f", billTotal)
                                    }
                                },
                                label = { Text(method, fontWeight = FontWeight.Medium) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Amount Paid Input
                    OutlinedTextField(
                        value = amountPaidText,
                        onValueChange = { amountPaidText = it },
                        label = { Text("Amount Received Now (₹)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_amount_paid"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Due Balance Calculation Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Due Balance (Udhar):", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "₹${String.format(Locale.US, "%.2f", outstandingBalance)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (outstandingBalance > 0) StatusRed else StatusGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Narration / Notes
                    OutlinedTextField(
                        value = saleNotes,
                        onValueChange = { saleNotes = it },
                        label = { Text("Narration / Notes (Optional)") },
                        placeholder = { Text("e.g. Paid via PhonePe, or due till Saturday") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Section 4: Process Transaction & Generate Receipt Button
        item {
            Button(
                onClick = {
                    if (cartItems.isEmpty()) {
                        Toast.makeText(context, "Please add at least 1 product to the bill", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val finalCustName = selectedCustomer?.name ?: manualCustomerName.ifBlank { "Customer" }

                    if (paymentMethod == "Credit/Udhar" && selectedCustomer == null && manualCustomerName.isBlank()) {
                        Toast.makeText(context, "Please select or type customer name for Credit / Udhar", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    isSaving = true
                    val paid = amountPaidText.toDoubleOrNull() ?: 0.0

                    // If manual customer was typed, ensure customer is also saved in repository
                    if (selectedCustomer == null && manualCustomerName.isNotBlank()) {
                        viewModel.addCustomer(
                            name = manualCustomerName,
                            phone = manualCustomerPhone,
                            address = "",
                            notes = "Auto-created from Sale"
                        ) { newCust ->
                            selectedCustomer = newCust
                            viewModel.recordSale(
                                customerId = newCust.id,
                                customerName = newCust.name,
                                items = cartItems,
                                totalAmount = billTotal,
                                amountPaid = paid,
                                paymentMethod = paymentMethod,
                                notes = saleNotes
                            ) { savedSale, savedPayment ->
                                isSaving = false
                                savedSaleResult = savedSale
                                savedPaymentResult = savedPayment
                            }
                        }
                    } else {
                        viewModel.recordSale(
                            customerId = selectedCustomer?.id,
                            customerName = finalCustName,
                            items = cartItems,
                            totalAmount = billTotal,
                            amountPaid = paid,
                            paymentMethod = paymentMethod,
                            notes = saleNotes
                        ) { savedSale, savedPayment ->
                            isSaving = false
                            savedSaleResult = savedSale
                            savedPaymentResult = savedPayment
                        }
                    }
                },
                enabled = !isSaving && cartItems.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_save_sale"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PROCESS TRANSACTION & GENERATE RECEIPT",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Modal: Add Customer Dialog with Photo Picker
    if (showAddCustomerDialog) {
        var newCustName by remember { mutableStateOf("") }
        var newCustPhone by remember { mutableStateOf("") }
        var newCustAddress by remember { mutableStateOf("") }
        var newCustNotes by remember { mutableStateOf("") }
        var newCustPhotoUri by remember { mutableStateOf("") }

        val customerPhotoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            uri?.let { newCustPhotoUri = it.toString() }
        }

        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text("Add Customer with Photo", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (newCustPhotoUri.isNotBlank()) {
                                AsyncImage(
                                    model = newCustPhotoUri,
                                    contentDescription = "Customer Photo",
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    customerPhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (newCustPhotoUri.isBlank()) "Add Photo" else "Change Photo", fontSize = 12.sp)
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = newCustName,
                            onValueChange = { newCustName = it },
                            label = { Text("Customer Name *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_new_customer_name"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = newCustPhone,
                            onValueChange = { newCustPhone = it },
                            label = { Text("Phone Number (WhatsApp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = newCustAddress,
                            onValueChange = { newCustAddress = it },
                            label = { Text("Address / Village / Mohalla") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = newCustNotes,
                            onValueChange = { newCustNotes = it },
                            label = { Text("Reference / Notes") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCustName.isBlank()) {
                            Toast.makeText(context, "Customer name is required", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addCustomer(
                            name = newCustName,
                            phone = newCustPhone,
                            address = newCustAddress,
                            notes = newCustNotes,
                            imageUri = newCustPhotoUri
                        ) { created ->
                            selectedCustomer = created
                            manualCustomerName = created.name
                            manualCustomerPhone = created.phone
                            showAddCustomerDialog = false
                            Toast.makeText(context, "Customer ${created.name} saved!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save Customer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Add Product Dialog
    if (showAddProductDialog) {
        var newProdName by remember { mutableStateOf("") }
        var newProdCategory by remember { mutableStateOf("") }
        var newBuyPrice by remember { mutableStateOf("") }
        var newSellPrice by remember { mutableStateOf("") }
        var newStockQty by remember { mutableStateOf("") }
        var newUnit by remember { mutableStateOf("Packet") }
        var newDesc by remember { mutableStateOf("") }
        var newImageUri by remember { mutableStateOf("") }

        val productPhotoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            uri?.let { newImageUri = it.toString() }
        }

        // Live calculation of savings
        val buy = newBuyPrice.toDoubleOrNull() ?: 0.0
        val sell = newSellPrice.toDoubleOrNull() ?: 0.0
        val savings = sell - buy

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("Add Product to Inventory", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (newImageUri.isNotBlank()) {
                                AsyncImage(
                                    model = newImageUri,
                                    contentDescription = "Product Photo",
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    productPhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (newImageUri.isBlank()) "Product Photo" else "Change Photo", fontSize = 12.sp)
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = newProdName,
                            onValueChange = { newProdName = it },
                            label = { Text("Product Name *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_new_product_name"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = newProdCategory,
                            onValueChange = { newProdCategory = it },
                            label = { Text("Category (e.g. Bidi, Tobacco)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newBuyPrice,
                                onValueChange = { newBuyPrice = it },
                                label = { Text("Buy Price (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = newSellPrice,
                                onValueChange = { newSellPrice = it },
                                label = { Text("Sell Price (₹) *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    if (sell > 0 || buy > 0) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (savings >= 0) StatusGreenContainer else StatusRedContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Calculated Profit/Savings: ₹${String.format(Locale.US, "%.2f", savings)} per unit",
                                    color = if (savings >= 0) StatusGreen else StatusRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newStockQty,
                                onValueChange = { newStockQty = it },
                                label = { Text("Stock Qty") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = newUnit,
                                onValueChange = { newUnit = it },
                                label = { Text("Unit") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = newDesc,
                            onValueChange = { newDesc = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProdName.isBlank()) {
                            Toast.makeText(context, "Product name is required", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val sellVal = newSellPrice.toDoubleOrNull() ?: 0.0
                        val buyVal = newBuyPrice.toDoubleOrNull() ?: 0.0
                        val stock = newStockQty.toDoubleOrNull() ?: 0.0

                        viewModel.addProduct(
                            name = newProdName,
                            category = newProdCategory,
                            purchasePrice = buyVal,
                            sellingPrice = sellVal,
                            stock = stock,
                            unit = newUnit,
                            description = newDesc,
                            imageUri = newImageUri
                        )
                        showAddProductDialog = false
                        Toast.makeText(context, "Product saved to database!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save Product")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Immediate Receipt Dialog upon Sale Completion
    savedSaleResult?.let { sale ->
        val customerPhone = selectedCustomer?.phone ?: manualCustomerPhone
        val hasUdhar = sale.outstandingBalance > 0

        AlertDialog(
            onDismissRequest = {
                savedSaleResult = null
                cartItems = emptyList()
                selectedCustomer = null
                manualCustomerName = ""
                manualCustomerPhone = ""
                amountPaidText = ""
                saleNotes = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Receipt Generated!")
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
                            Text("Invoice: ${sale.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Customer: ${sale.customerName}", fontWeight = FontWeight.Medium)
                            Text("Payment Mode: ${sale.paymentMethod}")
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Bill:")
                                Text("₹${String.format(Locale.US, "%.2f", sale.totalAmount)}", fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Amount Paid:")
                                Text("₹${String.format(Locale.US, "%.2f", sale.amountPaid)}", color = StatusGreen, fontWeight = FontWeight.Bold)
                            }
                            if (hasUdhar) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Udhar Balance:", color = StatusRed, fontWeight = FontWeight.Bold)
                                    Text("₹${String.format(Locale.US, "%.2f", sale.outstandingBalance)}", color = StatusRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Direct WhatsApp Action Buttons
                    if (customerPhone.isNotBlank()) {
                        Button(
                            onClick = {
                                val msg = WhatsAppManager.formatReceiptMessage(shopProfile, sale, selectedCustomer)
                                WhatsAppManager.launchWhatsAppIntent(context, customerPhone, msg)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Receipt on WhatsApp")
                        }

                        if (hasUdhar) {
                            OutlinedButton(
                                onClick = {
                                    val alertMsg = WhatsAppManager.formatPaymentReminderMessage(
                                        shopProfile,
                                        selectedCustomer ?: CustomerEntity(name = sale.customerName, phone = customerPhone, outstandingBalance = sale.outstandingBalance),
                                        sale.outstandingBalance
                                    )
                                    WhatsAppManager.launchWhatsAppIntent(context, customerPhone, alertMsg)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = StatusRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Payment Alert / Reminder", color = StatusRed)
                            }
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
                    Text("PDF Receipt")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        savedSaleResult = null
                        cartItems = emptyList()
                        selectedCustomer = null
                        manualCustomerName = ""
                        manualCustomerPhone = ""
                        amountPaidText = ""
                        saleNotes = ""
                    }
                ) {
                    Text("Done / New Sale")
                }
            }
        )
    }
}
