package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object RecordSale : Screen("record_sale", "New Sale", Icons.Default.PointOfSale)
    object Products : Screen("products", "Products", Icons.Default.Inventory2)
    object Customers : Screen("customers", "Customers", Icons.Default.People)
    object Reports : Screen("reports", "Reports", Icons.Default.Assessment)
    object AslamAi : Screen("aslam_ai", "ASLAM AI", Icons.Default.AutoAwesome)
    object Calculator : Screen("calculator", "Calculator", Icons.Default.Calculate)
    object Suppliers : Screen("suppliers", "Suppliers & Vendors", Icons.Default.LocalShipping)
    object BackupRestore : Screen("backup_restore", "Backup & Restore", Icons.Default.Backup)
    object SecuritySettings : Screen("security_settings", "App Security & PIN", Icons.Default.Shield)
    object SyncStatus : Screen("sync_status", "Database & Sync", Icons.Default.CloudSync)
    object Settings : Screen("settings", "Settings & Security", Icons.Default.Settings)

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(
                Dashboard,
                RecordSale,
                Products,
                Customers,
                Reports
            )
    }
}
