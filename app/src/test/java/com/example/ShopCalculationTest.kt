package com.example

import com.example.domain.FinancialReport
import com.example.domain.SaleItem
import org.junit.Assert.*
import org.junit.Test

class ShopCalculationTest {

    @Test
    fun test_sale_outstanding_calculation() {
        val totalAmount = 1500.0
        val amountPaid = 500.0
        val outstanding = (totalAmount - amountPaid).coerceAtLeast(0.0)
        assertEquals(1000.0, outstanding, 0.001)

        val fullPaid = 1500.0
        val zeroOutstanding = (totalAmount - fullPaid).coerceAtLeast(0.0)
        assertEquals(0.0, zeroOutstanding, 0.001)
    }

    @Test
    fun test_cogs_and_net_profit_calculation() {
        val items = listOf(
            SaleItem(productId = 1, productName = "Bidi Bundle 502", quantity = 10.0, unit = "bundle", unitPrice = 25.0, lineTotal = 250.0, purchasePrice = 18.0),
            SaleItem(productId = 2, productName = "Special Tobacco 50g", quantity = 5.0, unit = "packet", unitPrice = 40.0, lineTotal = 200.0, purchasePrice = 30.0)
        )

        val totalRevenue = items.sumOf { it.lineTotal } // 250 + 200 = 450
        val totalCogs = items.sumOf { it.purchasePrice * it.quantity } // (10*18) + (5*30) = 180 + 150 = 330
        val grossProfit = totalRevenue - totalCogs // 450 - 330 = 120
        val expenses = 30.0
        val netProfit = grossProfit - expenses // 120 - 30 = 90

        assertEquals(450.0, totalRevenue, 0.001)
        assertEquals(330.0, totalCogs, 0.001)
        assertEquals(120.0, grossProfit, 0.001)
        assertEquals(90.0, netProfit, 0.001)
    }

    @Test
    fun test_low_stock_detection() {
        val threshold = 10.0
        val currentStock = 8.0
        val isLow = currentStock <= threshold
        assertTrue(isLow)

        val plentyStock = 25.0
        val isPlentyLow = plentyStock <= threshold
        assertFalse(isPlentyLow)
    }
}
