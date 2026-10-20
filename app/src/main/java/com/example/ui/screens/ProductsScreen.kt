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
import com.example.data.model.ProductEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.PdfGenerator
import java.util.Locale

@Composable
fun ProductsScreen(
    viewModel: ShopViewModel
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val shopProfile by viewModel.shopProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    val categories = remember(products) {
        products.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val matchQuery = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.category.contains(searchQuery, ignoreCase = true)
            val matchCategory = selectedCategory == null || p.category.equals(selectedCategory, ignoreCase = true)
            matchQuery && matchCategory
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddProductDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_product")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
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
            // Header with Stock PDF Export and 3D gradient look
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Products & Inventory",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${products.size} Products in Catalog",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            if (products.isNotEmpty()) {
                                FilledTonalButton(
                                    onClick = {
                                        try {
                                            val pdf = PdfGenerator.generateInventoryReport(context, products, shopProfile)
                                            PdfGenerator.sharePdf(context, pdf, "Stock Inventory Report")
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stock PDF", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search product name or category...") },
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
                        .testTag("search_products"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Category Filter Chips
            if (categories.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All (${products.size})") }
                        )
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            }

            // Empty State
            if (filteredProducts.isEmpty()) {
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
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (products.isEmpty()) "No products added yet" else "No matching products found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (products.isEmpty())
                                    "Tap '+ Add Product' to start building your shop catalog with buy/sell rates & photos."
                                else "Try a different search keyword or category filter",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddProductDialog = true },
                                modifier = Modifier.testTag("btn_empty_add_product")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add First Product")
                            }
                        }
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    val isLowStock = product.stockQuantity <= product.lowStockThreshold
                    val unitSavings = product.sellingPrice - product.purchasePrice
                    val marginPercent = if (product.purchasePrice > 0) (unitSavings / product.purchasePrice) * 100 else 0.0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Product Image / Avatar
                            if (product.imageUri.isNotBlank()) {
                                AsyncImage(
                                    model = product.imageUri,
                                    contentDescription = product.name,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = product.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Details
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = product.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isLowStock) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = StatusRedContainer
                                        ) {
                                            Text(
                                                "LOW STOCK",
                                                color = StatusRed,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Category: ${product.category}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Stock: ${product.stockQuantity.toInt()} ${product.unit}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isLowStock) StatusRed else StatusGreen
                                    )
                                    Text(
                                        text = "Sell: ₹${String.format(Locale.US, "%.2f", product.sellingPrice)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (product.purchasePrice > 0) {
                                        Text(
                                            text = "Cost: ₹${String.format(Locale.US, "%.2f", product.purchasePrice)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                // Savings / Profit per Unit
                                if (unitSavings > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = StatusGreenContainer.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            text = "Profit: +₹${String.format(Locale.US, "%.2f", unitSavings)} (${String.format(Locale.US, "%.0f", marginPercent)}%)",
                                            color = StatusGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { editingProduct = product }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { productToDelete = product }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Add / Edit Product with Live Savings Calculation and Photo Picker
    if (showAddProductDialog || editingProduct != null) {
        val isEdit = editingProduct != null
        var name by remember { mutableStateOf(editingProduct?.name ?: "") }
        var category by remember { mutableStateOf(editingProduct?.category ?: "") }
        var purchasePrice by remember { mutableStateOf(editingProduct?.purchasePrice?.let { if (it > 0) it.toString() else "" } ?: "") }
        var sellingPrice by remember { mutableStateOf(editingProduct?.sellingPrice?.let { if (it > 0) it.toString() else "" } ?: "") }
        var stock by remember { mutableStateOf(editingProduct?.stockQuantity?.let { it.toInt().toString() } ?: "") }
        var unit by remember { mutableStateOf(editingProduct?.unit ?: "Packet") }
        var desc by remember { mutableStateOf(editingProduct?.description ?: "") }
        var lowThreshold by remember { mutableStateOf(editingProduct?.lowStockThreshold?.let { it.toInt().toString() } ?: "10") }
        var imageUri by remember { mutableStateOf(editingProduct?.imageUri ?: "") }

        // Live calculation of savings / profit margin
        val buy = purchasePrice.toDoubleOrNull() ?: 0.0
        val sell = sellingPrice.toDoubleOrNull() ?: 0.0
        val liveSavings = sell - buy
        val liveMarginPercent = if (buy > 0) (liveSavings / buy) * 100 else 0.0

        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            uri?.let { imageUri = it.toString() }
        }

        AlertDialog(
            onDismissRequest = {
                showAddProductDialog = false
                editingProduct = null
            },
            title = {
                Text(
                    text = if (isEdit) "Edit Product" else "Add New Product",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Photo Picker Item
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (imageUri.isNotBlank()) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Product Image",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
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
                                Text(if (imageUri.isBlank()) "Add Product Photo" else "Change Photo", fontSize = 12.sp)
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Product Name *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_product_name")
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category (e.g. Bidi, Tobacco, Leaves)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = purchasePrice,
                                onValueChange = { purchasePrice = it },
                                label = { Text("Buy Price (Cost)") },
                                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = sellingPrice,
                                onValueChange = { sellingPrice = it },
                                label = { Text("Sell Price *") },
                                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Live Savings & Profit Margin Field (Automatic upon typing buy & sell price)
                    if (sell > 0 || buy > 0) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (liveSavings >= 0) StatusGreenContainer else StatusRedContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Profit / Savings per $unit:",
                                            fontSize = 11.sp,
                                            color = if (liveSavings >= 0) StatusGreen else StatusRed
                                        )
                                        Text(
                                            text = if (liveSavings >= 0) "+₹${String.format(Locale.US, "%.2f", liveSavings)}" else "-₹${String.format(Locale.US, "%.2f", -liveSavings)}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (liveSavings >= 0) StatusGreen else StatusRed
                                        )
                                    }
                                    if (buy > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (liveSavings >= 0) StatusGreen else StatusRed
                                        ) {
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", liveMarginPercent)}% Margin",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = stock,
                                onValueChange = { stock = it },
                                label = { Text("Current Stock") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = unit,
                                onValueChange = { unit = it },
                                label = { Text("Unit (Bundle / Jhaad / Box)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Text("Bidi Business Units:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Bundle", "Jhaad", "Packet", "Box", "Piece", "Kg").forEach { u ->
                                FilterChip(
                                    selected = unit.equals(u, ignoreCase = true),
                                    onClick = { unit = u },
                                    label = { Text(u, fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = lowThreshold,
                            onValueChange = { lowThreshold = it },
                            label = { Text("Low Stock Alert Threshold") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = desc,
                            onValueChange = { desc = it },
                            label = { Text("Description / Stock Notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "Product name is required", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val currentStock = stock.toDoubleOrNull() ?: 0.0
                        val threshold = lowThreshold.toDoubleOrNull() ?: 10.0

                        if (isEdit && editingProduct != null) {
                            val updated = editingProduct!!.copy(
                                name = name.trim(),
                                category = category.trim().ifEmpty { "General" },
                                purchasePrice = buy,
                                sellingPrice = sell,
                                stockQuantity = currentStock,
                                unit = unit.trim().ifEmpty { "Packet" },
                                description = desc.trim(),
                                lowStockThreshold = threshold,
                                imageUri = imageUri
                            )
                            viewModel.updateProduct(updated)
                            Toast.makeText(context, "Product updated!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addProduct(
                                name = name,
                                category = category,
                                purchasePrice = buy,
                                sellingPrice = sell,
                                stock = currentStock,
                                unit = unit,
                                description = desc,
                                lowStockThreshold = threshold,
                                imageUri = imageUri
                            )
                            Toast.makeText(context, "Product created!", Toast.LENGTH_SHORT).show()
                        }
                        showAddProductDialog = false
                        editingProduct = null
                    }
                ) {
                    Text(if (isEdit) "Update Product" else "Save Product")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddProductDialog = false
                    editingProduct = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Delete Confirmation
    productToDelete?.let { prod ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete Product?") },
            text = { Text("Are you sure you want to delete '${prod.name}'? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(prod)
                        productToDelete = null
                        Toast.makeText(context, "${prod.name} deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
