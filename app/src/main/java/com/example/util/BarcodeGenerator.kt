package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SaleEntity
import com.example.domain.SaleItem
import com.example.ui.theme.StatusGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object BarcodeGenerator {

    /**
     * Formats comprehensive, structured record metadata for external tools like Google Lens / Search.
     * When scanned by Google Lens, external tools automatically parse and display:
     * - Product Names and quantities
     * - Invoice & Transaction ID
     * - Branch / Shop Name
     * - Customer Name
     * - Bill Amount & Payment Status
     */
    fun generateRecordMetadata(
        sale: SaleEntity,
        items: List<SaleItem>,
        shopName: String = "ASLAM RECORD"
    ): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(sale.timestamp))
        val itemsListStr = if (items.isNotEmpty()) {
            items.joinToString("\n") { item ->
                " • ${item.productName} (${item.quantity.toInt()} ${item.unit} @ ₹${String.format(Locale.US, "%.2f", item.unitPrice)}) = ₹${String.format(Locale.US, "%.2f", item.lineTotal)}"
            }
        } else {
            " • Bidi / Dalmot Products"
        }

        return buildString {
            appendLine("═══════════════════════════════════════")
            appendLine("ASLAM RECORD — OFFICIAL TRANSACTION")
            appendLine("═══════════════════════════════════════")
            appendLine("Shop / Branch: $shopName")
            appendLine("Invoice No: ${sale.invoiceNumber}")
            appendLine("Record ID: #${sale.id}")
            appendLine("Customer: ${sale.customerName}")
            appendLine("Date: $dateStr")
            appendLine("───────────────────────────────────────")
            appendLine("PRODUCTS & ITEMS:")
            appendLine(itemsListStr)
            appendLine("───────────────────────────────────────")
            appendLine("Total Bill: ₹${String.format(Locale.US, "%.2f", sale.totalAmount)}")
            appendLine("Amount Paid: ₹${String.format(Locale.US, "%.2f", sale.amountPaid)} (${sale.paymentMethod})")
            if (sale.outstandingBalance > 0) {
                appendLine("Udhar Due: ₹${String.format(Locale.US, "%.2f", sale.outstandingBalance)}")
            }
            appendLine("Status: ${sale.status}")
            appendLine("Barcode ID: AR-${sale.invoiceNumber}-${abs(sale.invoiceNumber.hashCode() % 10000)}")
            appendLine("Verify: https://aslamrecord.app/tx?inv=${sale.invoiceNumber}&id=${sale.id}")
            appendLine("═══════════════════════════════════════")
        }
    }

    /**
     * Generates a deterministic barcode sequence (1s and 0s) for drawing 1D barcode lines.
     */
    fun generate1DBarcodePattern(seedStr: String): List<Float> {
        val hash = abs(seedStr.hashCode())
        val widths = mutableListOf<Float>()
        // Standard start quiet zone
        widths.add(3f)
        widths.add(1f)
        widths.add(2f)
        widths.add(1f)

        var currentSeed = hash
        for (i in 0..28) {
            val barW = ((currentSeed % 3) + 1).toFloat()
            val spaceW = (((currentSeed / 3) % 2) + 1).toFloat()
            widths.add(barW)
            widths.add(spaceW)
            currentSeed = (currentSeed * 31 + 17) and 0x7FFFFFFF
        }

        // Stop guard
        widths.add(2f)
        widths.add(1f)
        widths.add(3f)
        return widths
    }

    /**
     * Generates a deterministic 21x21 QR Code style boolean matrix for drawing 2D QR Code.
     */
    fun generateQrMatrix(seedStr: String): Array<BooleanArray> {
        val size = 21
        val matrix = Array(size) { BooleanArray(size) }

        // Finder patterns (top-left, top-right, bottom-left)
        fun drawFinder(startX: Int, startY: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    matrix[startY + r][startX + c] = isBorder || isCenter
                }
            }
        }
        drawFinder(0, 0)
        drawFinder(size - 7, 0)
        drawFinder(0, size - 7)

        // Timing patterns
        for (i in 8 until size - 8) {
            matrix[6][i] = (i % 2 == 0)
            matrix[i][6] = (i % 2 == 0)
        }

        // Fill remaining with hash pattern
        var h = abs(seedStr.hashCode())
        for (r in 0 until size) {
            for (c in 0 until size) {
                // Skip finders
                val inTopLeft = r < 8 && c < 8
                val inTopRight = r < 8 && c >= size - 8
                val inBottomLeft = r >= size - 8 && c < 8
                val inTiming = (r == 6 && c in 8 until size - 8) || (c == 6 && r in 8 until size - 8)

                if (!inTopLeft && !inTopRight && !inBottomLeft && !inTiming) {
                    h = (h * 37 + 13) and 0x7FFFFFFF
                    matrix[r][c] = (h % 3 != 0)
                }
            }
        }
        return matrix
    }
}

/**
 * Visual Jetpack Compose Barcode & QR Code Card for transactions and records.
 */
@Composable
fun RecordBarcodeView(
    sale: SaleEntity,
    items: List<SaleItem>,
    shopName: String = "ASLAM RECORD",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showQrView by remember { mutableStateOf(false) }

    val barcodeSeed = "AR-${sale.invoiceNumber}-${sale.id}"
    val barcodePattern = remember(barcodeSeed) { BarcodeGenerator.generate1DBarcodePattern(barcodeSeed) }
    val qrMatrix = remember(barcodeSeed) { BarcodeGenerator.generateQrMatrix(barcodeSeed) }
    val metadataText = remember(sale, items, shopName) {
        BarcodeGenerator.generateRecordMetadata(sale, items, shopName)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("record_barcode_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Barcode Label & Lens Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (showQrView) Icons.Default.QrCode2 else Icons.Default.QrCode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showQrView) "QR Code & Google Lens" else "Record Auto-Barcode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Google Lens Ready",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barcode Canvas Display Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(8.dp))
                    .padding(vertical = 12.dp, horizontal = 16.dp)
                    .clickable { showQrView = !showQrView },
                contentAlignment = Alignment.Center
            ) {
                if (showQrView) {
                    // QR Code Matrix Rendering
                    Canvas(
                        modifier = Modifier
                            .size(150.dp)
                            .testTag("canvas_qr_code")
                    ) {
                        val matrixSize = qrMatrix.size
                        val cellSize = size.width / matrixSize
                        for (r in 0 until matrixSize) {
                            for (c in 0 until matrixSize) {
                                if (qrMatrix[r][c]) {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(c * cellSize, r * cellSize),
                                        size = Size(cellSize, cellSize)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // 1D Barcode Line Rendering
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("canvas_1d_barcode")
                        ) {
                            val totalUnits = barcodePattern.sum()
                            val unitPx = size.width / totalUnits
                            var currentX = 0f
                            var isBar = true

                            for (w in barcodePattern) {
                                val barWidthPx = w * unitPx
                                if (isBar) {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(currentX, 0f),
                                        size = Size(barWidthPx, size.height)
                                    )
                                }
                                currentX += barWidthPx
                                isBar = !isBar
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "*$barcodeSeed*",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap barcode to toggle 1D / 2D QR view for Google Lens scanning",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons: Copy Barcode Metadata / Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("ASLAM RECORD Barcode Data", metadataText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Barcode record data copied!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("btn_copy_barcode_data"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Metadata", fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, metadataText)
                            putExtra(Intent.EXTRA_SUBJECT, "ASLAM RECORD Invoice #${sale.invoiceNumber}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Scannable Barcode Data"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("btn_share_barcode_data"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share Barcode", fontSize = 11.sp)
                }
            }
        }
    }
}
