package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        CustomerEntity::class,
        SaleEntity::class,
        PaymentEntity::class,
        ExpenseEntity::class,
        SupplierEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun supplierDao(): SupplierDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aslam_record_database"
                )
                .addCallback(DatabaseCallback())
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Clean startup: Do not add any demo grocery items.
                // Database starts clean and genuine, ready for user inputs.
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Clean up any known demo/sample products if they were somehow present
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        db.execSQL(
                            "DELETE FROM products WHERE name IN (" +
                            "'Basmati Premium Rice', 'Bath Soap', 'Dairy Fresh Milk', " +
                            "'Premium Tea Dust', 'Refined Sugar', 'Sunflower Cooking Oil', " +
                            "'Washing Powder', 'Whole Wheat Atta')"
                        )
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
