package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.repository.ShopRepository

class AslamRecordApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: ShopRepository by lazy { ShopRepository(database) }

    override fun onCreate() {
        super.onCreate()
    }
}
