package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.PaperRepository
import com.example.data.SettingsRepository

class MyApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var repository: PaperRepository
    lateinit var settings: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, AppDatabase::class.java, "folio_db")
            .addMigrations(AppDatabase.migration1To2(System.currentTimeMillis()))
            .build()
        repository = PaperRepository(database)
        settings = SettingsRepository(this)
    }
}
